/*
 * Copyright 2009 Alin Dreghiciu.
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
package org.ops4j.pax.web.service;

import java.util.Set;
import org.osgi.service.http.HttpContext;

/**
 * {@link HttpContext} extensions.
 * 
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @since 0.5.3, March 30, 2009
 */
public interface WebContainerContext extends HttpContext {

    /**
	 * Returns a set of all the paths (String objects) to entries within the web
	 * application whose longest sub-path matches the supplied path argument. A
	 * specified path of "/" indicates the root of the web application.
	 * 
	 * @param name
	 *            the path name for which to return resource paths
	 * 
	 * @return a set of the resource paths (String objects) or null if no
	 *         resource paths could be found or if the caller does not have the
	 *         appropriate permissions.
	 */
	Set<String> getResourcePaths(String name);

}
