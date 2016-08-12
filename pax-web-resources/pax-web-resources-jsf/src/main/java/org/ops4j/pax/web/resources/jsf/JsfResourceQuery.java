/* Copyright 2016 Marc Schlegel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.resources.jsf;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ops4j.pax.web.resources.api.query.ResourceQueryMatcher;
import org.ops4j.pax.web.resources.api.query.ResourceQueryResult;

/**
 * An implementation of {@link ResourceQueryMatcher} which is able to parse
 * JSF resources. Those resources may use locale-prefix, library-name, library-version,
 * resource-version in addition to the resource-name.
 */
public class JsfResourceQuery implements ResourceQueryMatcher {

	/**
	 * Following Servlet 3.0 Specification for JAR-Resources
	 */
	private static final String RESOURCE_ROOT = "/META-INF/resources/";

	/**
	 * It checks version like this: /1/, /1_0/, /1_0_0/, /100_100/
	 * <p>
	 * Used on getLibraryVersion to filter resource directories
	 **/
	private static final Pattern VERSION_CHECKER = Pattern.compile("/\\p{Digit}+(_\\p{Digit}*)*/");

	/**
	 * It checks version like this: /1.js, /1_0.js, /1_0_0.js, /100_100.js
	 * <p>
	 * Used on getResourceVersion to filter resources
	 **/
	private static final Pattern RESOURCE_VERSION_CHECKER = Pattern.compile("/\\p{Digit}+(_\\p{Digit}*)*\\..*");

	private static final char PATH_SEPARATOR = '/';

	private final String localePrefix;
	private final String libraryName;
	private final String resourceName;
	private final String contentType;


	public JsfResourceQuery(String localePrefix, String libraryName, String resourceName, String contentType) {
		if (resourceName == null) {
			throw new IllegalArgumentException("resourceName must be set!");
		}
		this.localePrefix = localePrefix;
		this.libraryName = libraryName;
		this.resourceName = resourceName;
		this.contentType = contentType;
	}

	/**
	 * <p>Reduces duplication for the actual parsing-functions, by checking for null and wrapping the result
	 * in an Optional.</p>
	 * <p>Used by the actual matching:
	 * <ul>
	 * <li>{@link #matchLocalePrefix(String, MatchingResult)}</li>
	 * <li>{@link #matchLibraryName(String, MatchingResult)}</li>
	 * <li>{@link #matchLibraryVersion(String, MatchingResult)}</li>
	 * <li>{@link #matchResourceName(String, MatchingResult)}</li>
	 * <li>{@link #matchResourceVersion(String, MatchingResult)}</li>
	 * <p>
	 * </ul>
	 * </p>
	 *
	 * @param path the path (may be partially stripped) which will be checked and passed to the function
	 * @param func actual matching logic provided as function
	 * @return the result from the given function wrapped in an Optional
	 */
	private Optional<String> process(final String path, Function<String, String> func) {
		String workPath = path;
		if (workPath == null) {
			throw new IllegalArgumentException("Given String must not be null!");
		}
		if (workPath.charAt(0) != PATH_SEPARATOR) {
			workPath = PATH_SEPARATOR + workPath;
		}
		workPath = func.apply(workPath);
		if (workPath != null && workPath.trim().length() > 0) {
			return Optional.of(workPath);
		} else {
			return Optional.empty();
		}
	}

	private String matchLocalePrefix(final String path, final MatchingResult result) {
		if (localePrefix != null && path.startsWith(PATH_SEPARATOR + localePrefix)) {
			result.matchedLocalePrefix = true;
			return path.substring(path.indexOf(PATH_SEPARATOR, 1));
		} else {
			return path;
		}
	}


	private String matchLibraryName(final String path, final MatchingResult result) {
		if (libraryName != null && path.startsWith(PATH_SEPARATOR + libraryName)) {
			result.matchedLibraryName = true;
			return path.substring(path.indexOf(PATH_SEPARATOR, 1));
		} else {
			return path;
		}
	}

	private String matchLibraryVersion(final String path, final MatchingResult result) {
		Matcher matcher = VERSION_CHECKER.matcher(path);
		if (matcher.find()) {
			String libraryVersion = matcher.group();
			result.matchedLibraryVersion = libraryVersion.substring(1, libraryVersion.length() - 1);
			return path.substring(libraryVersion.length() - 1);
		} else {
			return path;
		}
	}

	private String matchResourceName(final String path, final MatchingResult result) {
		if (path.startsWith(PATH_SEPARATOR + resourceName)) {
			result.matchedResourceName = true;
			// after resourceName, an optional resourceVersion might follow
			int index = path.indexOf(PATH_SEPARATOR, 1);
			if (index != -1) {
				return path.substring(index);
			} else {
				return null;
			}

		} else {
			return path;
		}
	}

	private String matchResourceVersion(final String path, final MatchingResult result) {
		Matcher matcher = RESOURCE_VERSION_CHECKER.matcher(path);
		if (matcher.find()) {
			result.matchedResourceVersion = matcher.group().substring(1);
			// after resourceVersion no, more elements follow
			return null;
		} else {
			return path;
		}
	}


	@SuppressWarnings("unchecked")
	@Override
	public <R extends ResourceQueryResult> Optional<R> matches(final String resourcePath) {
		String workPath = resourcePath;

		if (workPath.startsWith(RESOURCE_ROOT)) {
			// remove /META-INF/resources because it is not important for the matching
			workPath = workPath.substring(RESOURCE_ROOT.length());
		}
		MatchingResult result = new MatchingResult();
		Optional<String> workingString = process(workPath, path -> matchLocalePrefix(path, result));
		if (workingString.isPresent()) {
			workingString = process(workingString.get(), path -> matchLibraryName(path, result));
		}
		if (workingString.isPresent()) {
			workingString = process(workingString.get(), path -> matchLibraryVersion(path, result));
		}
		if (workingString.isPresent()) {
			workingString = process(workingString.get(), path -> matchResourceName(path, result));
		}
		if (workingString.isPresent()) {
			process(workingString.get(), path -> matchResourceVersion(path, result));
		}

		return (Optional<R>) result.createFinalResult();
	}


	private static final class MatchingResult {
		private boolean matchedLocalePrefix;
		private boolean matchedLibraryName;
		private String matchedLibraryVersion;
		private boolean matchedResourceName;
		private String matchedResourceVersion;

		private Optional<JsfResourceQueryResult> createFinalResult() {
			if (!matchedResourceName) {
				return Optional.empty();
			} else {
				return Optional.of(new JsfResourceQueryResult(matchedLocalePrefix, matchedLibraryName, matchedLibraryVersion, matchedResourceVersion));
			}
		}
	}
}
