/*
 * Copyright 2020 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.samples.whiteboard.internal;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhiteboardListener implements ServletRequestListener {

	private static final Logger LOG = LoggerFactory.getLogger(WhiteboardListener.class);

	public void requestInitialized(final ServletRequestEvent sre) {
		try {
			Thread.currentThread().getContextClassLoader().loadClass(WhiteboardServlet.class.getName());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		LOG.info("Request initialized from ip: " + sre.getServletRequest().getRemoteAddr());
	}

	public void requestDestroyed(final ServletRequestEvent sre) {
		try {
			Thread.currentThread().getContextClassLoader().loadClass(WhiteboardServlet.class.getName());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		LOG.info("Request destroyed from ip: " + sre.getServletRequest().getRemoteAddr());
	}

}
