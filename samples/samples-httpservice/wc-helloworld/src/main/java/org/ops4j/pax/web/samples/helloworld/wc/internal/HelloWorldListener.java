/* Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.samples.helloworld.wc.internal;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;

/**
 * Hello World Request Listener. Counts each request and stores the value in
 * context attributes.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 02, 2008
 */
public class HelloWorldListener implements ServletRequestListener {

	public void requestInitialized(final ServletRequestEvent sre) {
		final ServletContext sc = sre.getServletContext();
		Integer requestCounter = (Integer) sc.getAttribute("requestCounter");
		if (requestCounter == null) {
			requestCounter = 0;
		}
		requestCounter++;
		sc.setAttribute("requestCounter", requestCounter);
	}

	public void requestDestroyed(final ServletRequestEvent sre) {
		// do nothing
	}

}
