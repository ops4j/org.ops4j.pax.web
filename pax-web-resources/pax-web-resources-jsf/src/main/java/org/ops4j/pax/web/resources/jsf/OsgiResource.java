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

import org.apache.commons.lang3.StringUtils;
import org.ops4j.pax.web.resources.jsf.internal.FacesServletMapping;
import org.ops4j.pax.web.resources.jsf.internal.ResourceHandlerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.faces.application.ProjectStage;
import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.context.FacesContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Custom resource-implementation because other implementations are tied to
 * either Mojarra or Myfaces.
 */
public class OsgiResource extends Resource {


	public static final String REQUEST_PARAM_TYPE = "type";
	public static final String REQUEST_PARAM_LIBRARY = "ln";
	public static final String REQUEST_PARAM_LIBRARY_VERSION = "lv";
	public static final String REQUEST_PARAM_LOCALE = "loc";
	public static final String REQUEST_PARAM_RESOURCE_VERSION = "rv";
	private static final String PATTERN_RFC_1036 = "EEE, dd-MMM-yy HH:mm:ss zzz";
	private static final String PATTERN_ASCITIME = "EEE MMM d HH:mm:ss yyyy";

	private transient Logger logger;
	private final URL bundleResourceUrl;
	private final LocalDateTime lastModified;
	private String resourceVersion;
	private String libraryVersion;
	private String localePrefix;

	public OsgiResource(URL bundleResourceUrl, String localePrefix, String resourceName, String resourceVersion, String libraryName, String libraryVersion, LocalDateTime lastModified) {
		if (bundleResourceUrl == null) {
			throw new IllegalArgumentException("URL for resource must not be null");
		}
		logger = LoggerFactory.getLogger(getClass());
		setLocalePrefix(localePrefix);
		setResourceName(resourceName);
		setResourceVersion(resourceVersion);
		setLibraryName(libraryName);
		setLibraryVersion(libraryVersion);
		this.bundleResourceUrl = bundleResourceUrl;
		this.lastModified = lastModified;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		try {
			return bundleResourceUrl.openConnection().getInputStream();
		} catch (IOException e) {
			logger.error("Cannot open InputStream. This can happen when the bundle that contains the resource was stopped after resource-creation");
			throw new IOException("Resource not available any more because bundle was uninstalled.");
		}
	}

	@Override
	public String getRequestPath() {
		final FacesContext facesContext = FacesContext.getCurrentInstance();

		FacesServletMapping servletMapping = ResourceHandlerUtils.getFacesServletMapping(facesContext);

		String path = ResourceHandler.RESOURCE_IDENTIFIER + '/' + getResourceName();

		final StringBuilder pathBuilder;
		if (servletMapping.isExtensionMapping()) {
			pathBuilder = new StringBuilder(path).append(servletMapping.getMapping());
		} else {
			pathBuilder = new StringBuilder(servletMapping.getMapping()).append(path);
		}


		List<String> parameters = new ArrayList<>(5);
		// mark OsgiResources with request-parameter
		parameters.add(REQUEST_PARAM_TYPE + "=osgi");
		// add information about resource
		if (StringUtils.isNotBlank(localePrefix)) {
			parameters.add(REQUEST_PARAM_LOCALE + "=" + localePrefix);
		}
		if (getLibraryName() != null) {
			parameters.add(REQUEST_PARAM_LIBRARY + "=" + getLibraryName());
		}
		if (StringUtils.isNotBlank(libraryVersion)) {
			parameters.add(REQUEST_PARAM_LIBRARY_VERSION + "=" + libraryVersion);
		}
		if (StringUtils.isNotBlank(resourceVersion)) {
			parameters.add(REQUEST_PARAM_RESOURCE_VERSION + "=" + resourceVersion);
		}
		if (!facesContext.isProjectStage(ProjectStage.Production)) {
			// append stage for all ProjectStages except Production
			parameters.add("stage=" + facesContext.getApplication().getProjectStage().toString());
		}

		// concat optional parameter with & and add as request-parameters
		String parameterString = parameters.stream().collect(Collectors.joining("&"));
		if (StringUtils.isNotBlank(parameterString)) {
			pathBuilder.append("?").append(parameterString);
		}

		return facesContext.getApplication().getViewHandler().getResourceURL(facesContext, pathBuilder.toString());
	}

	@Override
	public Map<String, String> getResponseHeaders() {
		return new HashMap<>(0);
	}

	@Override
	public URL getURL() {
		return bundleResourceUrl;
	}

	@Override
	public boolean userAgentNeedsUpdate(final FacesContext facesContext) {
		// RFC2616 says related to If-Modified-Since header the following:
		//
		// "... The If-Modified-Since request-header field is used with a method
		// to make it conditional: if the requested variant has not been
		// modified since the time specified in this field, an entity will not
		// be returned from the server; instead, a 304 (not modified) response
		// will be returned without any message-body..."
		//
		// This method is called from ResourceHandlerImpl.handleResourceRequest
		// and if returns false send a 304 Not Modified response.
		if (!facesContext.isProjectStage(ProjectStage.Development)) {
			final Optional<String> ifModSinceHeader = Optional
					.ofNullable(facesContext.getExternalContext().getRequestHeaderMap().get("If-Modified-Since"));
			if (ifModSinceHeader.isPresent()) {
				LocalDateTime ifModifiedSince = convertIfModifiedSinceToDate(ifModSinceHeader.get());
				if (ifModifiedSince != null) {
					return lastModified.isAfter(ifModifiedSince);
				}
			}
		}
		// when in Development, or no if-modified-since header was found
		// (or couldn't be parsed),request a fresh resource
		return true;
	}

	/**
	 * <p>
	 * RFC 2616 allows three different date-formats:
	 * <ul>
	 * <li>RFC 1123</li>
	 * <li>RFC 1036</li>
	 * <li>ASCI-Time</li>
	 * </ul>
	 * </p>
	 *
	 * @param headerValue value transmitted from client
	 * @return the parsed DateTime
	 * @see <a href=
	 * "http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3">RFC
	 * 2616</a>
	 */
	private LocalDateTime convertIfModifiedSinceToDate(String headerValue) {

		LocalDateTime time = null;
		try {
			time = LocalDateTime.parse(headerValue, DateTimeFormatter.RFC_1123_DATE_TIME);
		} catch (DateTimeParseException e) {
			logger.trace("could not parse date with RFC-1123. Will try RFC-1036 format...");
		}

		if (time == null) {
			try {
				time = LocalDateTime.parse(headerValue, DateTimeFormatter.ofPattern(PATTERN_RFC_1036));
			} catch (DateTimeParseException e) {
				logger.trace("could not parse date with RFC-1036. Will try ASCITIME format...");
			}
		}

		if (time == null) {
			try {
				time = LocalDateTime.parse(headerValue, DateTimeFormatter.ofPattern(PATTERN_ASCITIME));
			} catch (DateTimeParseException e) {
				logger.trace("could not parse date with ASCITIME.");
			}
		}

		if (time == null) {
			logger.error("Could not parse given if-modified-since header with value '{}'", headerValue);
		}
		return time;
	}

	public void setLocalePrefix(String localePrefix) {
		this.localePrefix = localePrefix;
	}

	public void setResourceVersion(String resourceVersion) {
		this.resourceVersion = resourceVersion;
	}

	public void setLibraryVersion(String libraryVersion) {
		this.libraryVersion = libraryVersion;
	}

}
