/*
 * Copyright 2007 Damian Golda.
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
package org.ops4j.pax.web.extender.war.internal.util;

import java.net.URL;

/**
 * Divers utilities related to request paths (copy of the same class form Pax
 * Web).
 * 
 * @author Alin Dreghiciu
 * @since 0.3.0
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
	 * @param path
	 *            path to normalize
	 * 
	 * @return normalized path or the original path if there is nothing to be
	 *         replaced.
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
	 * Replaces multiple subsequent slashes with one slash. E.g. ////a//path//
	 * will becaome /a/path/
	 * 
	 * @param target
	 *            target sring to be replaced
	 * 
	 * @return a string where the subsequent slashes are replaced with one slash
	 */
	static String replaceSlashes(final String target) {
		String replaced = target;
		if (replaced != null) {
			replaced = replaced.replaceAll("/+", "/");
		}
		return replaced;
	}

	/**
	 * Finds the direct parent of the path of the given URL. E.g.
	 * /parent/file.xml yields parent, but /parent/file.xml/ yields file.xml
	 * 
	 * @param entry
	 *            A location.
	 * @return The (bare) parent path.
	 */
	public static String getDirectParent(URL entry) {
		String path = entry.getPath();
		int last = path.lastIndexOf('/');
		if (last > 0) {
			int first = path.lastIndexOf('/', last - 1);
			if (first >= 0) {
				return path.substring(first + 1, last);
			} else {
				return path.substring(0, last);
			}
		} else {
			return "";
		}
	}

}
