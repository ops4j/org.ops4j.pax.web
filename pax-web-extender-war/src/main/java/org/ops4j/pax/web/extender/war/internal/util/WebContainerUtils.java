/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.war.internal.util;

import org.ops4j.pax.web.service.WebContainer;
import org.osgi.service.http.HttpService;

/**
 * Utilities related to Web Container (Pax Web)
 */
public class WebContainerUtils {

	/**
	 * Utility class. Should be used only via static methods.
	 */
	private WebContainerUtils() {
		// utility class
	}

	/**
	 * Verify if WebContainer (from Pax Web) is available.
	 * 
	 * @param httpService
	 *            http service instance to check
	 * 
	 * @return true if WebContainer is available
	 */
	public static boolean webContainerAvailable(final HttpService httpService) {
		try {
			return (WebContainer.class != null)
					&& (httpService instanceof WebContainer);
		} catch (NoClassDefFoundError ignore) {
			return false;
		}

	}

}
