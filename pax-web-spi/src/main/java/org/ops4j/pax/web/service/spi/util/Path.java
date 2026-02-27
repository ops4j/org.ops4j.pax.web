/*
 * Copyright 2007 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.ops4j.pax.web.service.spi.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Divers utilities related to request paths.
 *
 * @author Alin Dreghiciu
 * @since 0.2.1
 */
public class Path {

	/**
	 * Utility class. Ment to be used via static methods.
	 */
	private Path() {
		// utility class. Ment to be used via static methods.
	}

	/**
	 * Normalize the path for accesing a resource, meaning that will replace
	 * consecutive slashes and will remove a leading slash if present.
	 *
	 * @param path path to normalize
	 * @return normalized path or the original path if there is nothing to be
	 * replaced.
	 */
	public static String normalizeResourcePath(final String path) {
		if (path == null) {
			return null;
		}
		String normalizedPath = replaceSlashes(path.trim());
		if (normalizedPath.startsWith("/") && normalizedPath.length() > 1) {
			normalizedPath = normalizedPath.substring(1);
		}
		return normalizedPath;
	}

	/**
	 * <p>Make passed path securely appendable to some resource base path. Resource base path may (but doesn't have to)
	 * be a String with trailing slash - the only requirement is that it should mean a <em>base directory</em> or
	 * <em>chroot</em> that can't be escaped.</p>
	 *
	 * <p>The returned path may, but doesn't have to end with "/" which could be treated as file vs. directory
	 * distinction.</p>
	 *
	 * <p>If {@code null} is returned, it means that path is invalid and/or it goes out of the chroot.</p>
	 *
	 * @param path
	 * @return
	 */
	public static String securePath(final String path) {
		String p = path == null ? "" : path.replace('\\', '/');
		try {
			URL url = new URL(p);
			p = url.getPath();
		} catch (MalformedURLException ignored) {
		}

		return normalize(p);
	}

	/**
	 * Replaces multiple subsequent slashes with one slash. E.g. ////a//path//
	 * will become /a/path/
	 *
	 * @param target target sring to be replaced
	 * @return a string where the subsequent slashes are replaced with one slash
	 */
	public static String replaceSlashes(final String target) {
		String replaced = target;
		if (replaced != null) {
			replaced = replaced.replaceAll("/+", "/");
		}
		return replaced;
	}

	/**
	 * Normalize an array of patterns.
	 *
	 * @param urlPatterns to mormalize
	 * @return array of nomalized patterns
	 */
	public static String[] normalizePatterns(final String[] urlPatterns) {
		String[] normalized = null;
		if (urlPatterns != null) {
			normalized = new String[urlPatterns.length];
			for (int i = 0; i < urlPatterns.length; i++) {
				normalized[i] = normalizePattern(urlPatterns[i]);
			}
		}
		return normalized;
	}

	/**
	 * Normalizes a pattern = prepends the path with slash (/) if the path does
	 * not start with a slash.
	 *
	 * @param pattern to normalize
	 * @return normalized pattern
	 */
	public static String normalizePattern(final String pattern) {
		if (pattern == null || "".equals(pattern.trim())) {
			return "/";
		}
		if (!pattern.startsWith("/") && !pattern.startsWith("*")) {
			return "/" + pattern;
		}
		return pattern;
	}

	/**
	 * Alias should always be "exact" path
	 *
	 * @param alias
	 * @return
	 */
	public static String normalizeAlias(String alias) {
		if (alias == null) {
			return null;
		}
		if (alias.equals("/")) {
			return alias;
		}
		if (alias.startsWith("*.")) {
			throw new IllegalArgumentException("Alias can't be in the form of \"*.alias\"");
		}
		if (!alias.startsWith("/")) {
			alias = "/" + alias;
		}
		while (alias.length() > 1 && alias.endsWith("/")) {
			alias = alias.substring(0, alias.length() - 1);
		}

		return alias;
	}

	// copy from org.apache.commons.io.FilenameUtils.requireNonNullChars
	private static String requireNonNullChars(final String path) {
		if (path.indexOf(0) >= 0) {
			throw new IllegalArgumentException(
					"Null character present in file/path name. There are no known legitimate use cases for such data, but several injection attacks may use it");
		}
		return path;
	}

	// copy from org.apache.commons.io.FilenameUtils.doNormalize
	private static String normalize(String fileName) {
		char separator = '/';
		if (fileName == null) {
			fileName = "";
		}

		fileName = requireNonNullChars(fileName);

		int size = fileName.length();
		if (size == 0) {
			return fileName;
		}
		final int prefix = getPrefixLength(fileName);
		if (prefix < 0) {
			return null;
		}

		final char[] array = new char[size + 2];  // +1 for possible extra slash, +2 for arraycopy
		fileName.getChars(0, fileName.length(), array, 0);

		// fix separators throughout
		final char otherSeparator = flipSeparator(separator);
		for (int i = 0; i < array.length; i++) {
			if (array[i] == otherSeparator) {
				array[i] = separator;
			}
		}

		// add extra separator on the end to simplify code below
		boolean lastIsDirectory = true;
		if (array[size - 1] != separator) {
			array[size++] = separator;
			lastIsDirectory = false;
		}

		// adjoining slashes
		// If we get here, prefix can only be 0 or greater, size 1 or greater
		// If prefix is 0, set loop start to 1 to prevent index errors
		// CHECKSTYLE:OFF
		for (int i = prefix != 0 ? prefix : 1; i < size; i++) {
			if (array[i] == separator && array[i - 1] == separator) {
				System.arraycopy(array, i, array, i - 1, size - i);
				size--;
				i--;
			}
		}

		// period slash
		for (int i = prefix + 1; i < size; i++) {
			if (array[i] == separator && array[i - 1] == '.' &&
					(i == prefix + 1 || array[i - 2] == separator)) {
				if (i == size - 1) {
					lastIsDirectory = true;
				}
				System.arraycopy(array, i + 1, array, i - 1, size - i);
				size -= 2;
				i--;
			}
		}

		// double period slash
		outer:
		for (int i = prefix + 2; i < size; i++) {
			if (array[i] == separator && array[i - 1] == '.' && array[i - 2] == '.' &&
					(i == prefix + 2 || array[i - 3] == separator)) {
				if (i == prefix + 2) {
					return null;
				}
				if (i == size - 1) {
					lastIsDirectory = true;
				}
				int j;
				for (j = i - 4 ; j >= prefix; j--) {
					if (array[j] == separator) {
						// remove b/../ from a/b/../c
						System.arraycopy(array, i + 1, array, j + 1, size - i);
						size -= i - j;
						i = j + 1;
						continue outer;
					}
				}
				// remove a/../ from a/../c
				System.arraycopy(array, i + 1, array, prefix, size - i);
				size -= i + 1 - prefix;
				i = prefix + 1;
			}
		}
		// CHECKSTYLE:ON

		if (size <= 0) {  // should never be less than 0
			return "";
		}
		if (size <= prefix) {  // should never be less than prefix
			return new String(array, 0, size);
		}
		if (lastIsDirectory) {
			return new String(array, 0, size);  // keep trailing separator
		}
		return new String(array, 0, size - 1);  // lose trailing separator
	}

	// copy from org.apache.commons.io.FilenameUtils.getPrefixLength
	public static int getPrefixLength(final String fileName) {
		if (fileName == null) {
			return -1;
		}
		final int len = fileName.length();
		if (len == 0) {
			return 0;
		}
		char ch0 = fileName.charAt(0);
		if (ch0 == ':') {
			return -1;
		}
		if (len == 1) {
			if (ch0 == '~') {
				return 2;  // return a length greater than the input
			}
			return isSeparator(ch0) ? 1 : 0;
		}
		if (ch0 == '~') {
			int posUnix = fileName.indexOf('/', 1);
			int posWin = fileName.indexOf('\\', 1);
			if (posUnix == -1 && posWin == -1) {
				return len + 1;  // return a length greater than the input
			}
			posUnix = posUnix == -1 ? posWin : posUnix;
			posWin = posWin == -1 ? posUnix : posWin;
			return Math.min(posUnix, posWin) + 1;
		}
		final char ch1 = fileName.charAt(1);
		if (ch1 == ':') {
			ch0 = Character.toUpperCase(ch0);
			if (ch0 >= 'A' && ch0 <= 'Z') {
				if (len == 2 && File.separatorChar != '\\') {
					// not Windows
					return 0;
				}
				if (len == 2 || !isSeparator(fileName.charAt(2))) {
					return 2;
				}
				return 3;
			}
			if (ch0 == '/') {
				return 1;
			}
			return -1;

		}
		if (!isSeparator(ch0) || !isSeparator(ch1)) {
			return isSeparator(ch0) ? 1 : 0;
		}
		int posUnix = fileName.indexOf('/', 2);
		int posWin = fileName.indexOf('\\', 2);
		if (posUnix == -1 && posWin == -1 || posUnix == 2 || posWin == 2) {
			return -1;
		}
		posUnix = posUnix == -1 ? posWin : posUnix;
		posWin = posWin == -1 ? posUnix : posWin;
		final int pos = Math.min(posUnix, posWin) + 1;
		final String hostnamePart = fileName.substring(2, pos - 1);
		return pos;
	}

	// copy from org.apache.commons.io.FilenameUtils.isSeparator
	private static boolean isSeparator(final char ch) {
		return ch == '/' || ch == '\\';
	}

	// copy from org.apache.commons.io.FilenameUtils.flipSeparator
	static char flipSeparator(final char ch) {
		if (ch == '/') {
			return '\\';
		}
		if (ch == '\\') {
			return '/';
		}
		throw new IllegalArgumentException(String.valueOf(ch));
	}

}
