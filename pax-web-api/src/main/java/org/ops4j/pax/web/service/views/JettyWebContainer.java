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
package org.ops4j.pax.web.service.views;

import java.net.URL;

import org.osgi.service.http.HttpContext;

/**
 * A container {@link PaxWebContainerView view} providing Jetty specific configuration methods.
 */
public interface JettyWebContainer extends PaxWebContainerView {

	/**
	 * Add Jetty-specific descriptor to given context.
	 *
	 * @param jettyWebXmlURL
	 * @param httpContext
	 */
	void registerJettyWebXml(URL jettyWebXmlURL, HttpContext httpContext);

	//	void setConnectorsAndVirtualHosts(List<String> connectors, List<String> virtualHosts, HttpContext httpContext);

	/**
	 * Enable stack traces in error pages for Jetty.
	 * See: {@code org.eclipse.jetty.server.handler.ErrorHandler#setShowStacks(boolean)}.
	 *
	 * @param showStacks
	 */
	void setShowStacks(boolean showStacks);

}
