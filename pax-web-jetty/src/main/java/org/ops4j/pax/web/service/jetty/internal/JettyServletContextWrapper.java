/* Copyright 2012 Harald Wellmann
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
package org.ops4j.pax.web.service.jetty.internal;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.ops4j.pax.web.service.spi.ServletContextManager;
import org.ops4j.pax.web.service.spi.ServletContextManager.ServletContextWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapts a Jetty {@link ServletContextHandler} to the {@link ServletContextWrapper} interface
 * required by {@link ServletContextManager}.
 * 
 * @author Harald Wellmann
 */
public class JettyServletContextWrapper implements ServletContextWrapper {

	private static final Logger LOG = LoggerFactory.getLogger(JettyServletContextWrapper.class);

	private ServletContextHandler context;

	public JettyServletContextWrapper(ServletContextHandler context) {
		this.context = context;
	}

	@Override
	public void start() {
		try {
			context.start();
		}
		catch (Exception exc) {
			LOG.error(
				"Could not start the servlet context for context path [" + context.getContextPath()
					+ "]", exc);
		}
	}

	@Override
	public void stop() {
		try {
			context.stop();
		}
		catch (Exception exc) {
			LOG.error(
				"Could not stop the servlet context for context path [" + context.getContextPath()
					+ "]", exc);
		}
	}
}
