/*
 * Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.whiteboard.internal.util;

import org.ops4j.pax.web.service.WebContainer;
import org.osgi.service.http.HttpService;

/**
 * Utilities related to web container.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, March 16, 2008
 */
public class WebContainerUtils {

	/**
	 * True if Pax Web imported packages were resolved.
	 */
	public static final boolean WEB_CONATAINER_AVAILABLE = webContainerAvailable();
	public static final boolean WEBSOCKETS_AVAILABLE = webSocketsAvailable();

	private WebContainerUtils() {
		// hide constructor
	}

	/**
	 * Verify if web container (from Pax Web) is available (package import was
	 * resolved).
	 *
	 * @return true if web container is available
	 */
	private static boolean webContainerAvailable() {
		try {
			return WebContainer.class != null;
		} catch (NoClassDefFoundError ignore) {
			return false;
		}
	}

	private static boolean webSocketsAvailable() {
//		try {
//			return javax.websocket.Endpoint.class != null;
//		} catch (NoClassDefFoundError ignore) {
			return false;
//		}
	}

	/**
	 * Verify if an http service is an Web Container (from pax Web)
	 *
	 * @param httpService to verify
	 * @return true if http service is an web container
	 */
	public static boolean isWebContainer(HttpService httpService) {
		return WEB_CONATAINER_AVAILABLE && httpService instanceof WebContainer;
	}

}