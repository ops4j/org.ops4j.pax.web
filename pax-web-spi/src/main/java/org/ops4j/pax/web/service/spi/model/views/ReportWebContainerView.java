/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.service.spi.model.views;

import java.util.Set;

import org.ops4j.pax.web.service.spi.model.info.ServletInfo;
import org.ops4j.pax.web.service.spi.model.info.WebApplicationInfo;
import org.ops4j.pax.web.service.views.PaxWebContainerView;

/**
 * <p>This {@link PaxWebContainerView view} is used only to get read-only information about the state of
 * {@link org.ops4j.pax.web.service.spi.model.ServerModel}. It plays a role similar to
 * {@link org.osgi.service.servlet.runtime.HttpServiceRuntime} but with much greater flexibility.</p>
 *
 * <p>This view was created especially for the purpose of Karaf commands.</p>
 */
public interface ReportWebContainerView extends PaxWebContainerView {

	/**
	 * <p>List all {@link WebApplicationInfo web applications} installed (or failed) into pax-web-runtime.</p>
	 *
	 * <p>User expects mostly the Web Application Bundles (WABs), but the list will contain all
	 * <em>web applications</em> - the ones created using Whiteboard registration or direct installation using
	 * {@link org.ops4j.pax.web.service.http.HttpService} as well.</p>
	 *
	 * @return
	 */
	Set<WebApplicationInfo> listWebApplications();

	/**
	 * <p>Returns {@link WebApplicationInfo} by context path - only real, deployed WABs are considered.
	 * <em>Web applications</em> created using Whiteboard or HttpService methods are not returned.</p>
	 *
	 * @param contextPath
	 * @return
	 */
	WebApplicationInfo getWebApplication(String contextPath);

	/**
	 * <p>Returns {@link WebApplicationInfo} by bundle Id - only real WABs are considered. <em>Web applications</em>
	 * created using Whiteboard or HttpService methods are not returned.</p>
	 *
	 * @param bundleId
	 * @return
	 */
	WebApplicationInfo getWebApplication(long bundleId);

	/**
	 * <p>Returns all available servlets registered using different means.</p>
	 *
	 * @return
	 */
	Set<ServletInfo> listServlets();

}
