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

import java.util.List;

import org.ops4j.pax.web.service.spi.model.WebApplicationModel;
import org.ops4j.pax.web.service.views.PaxWebContainerView;

/**
 * <p>This {@link PaxWebContainerView view} is used only to get read-only information about the state of
 * {@link org.ops4j.pax.web.service.spi.model.ServerModel}. It plays a role similar to
 * {@link org.osgi.service.http.runtime.HttpServiceRuntime} but with much greater flexibility.</p>
 *
 * <p>This view was created especially for the purpose of Karaf commands.</p>
 */
public interface ReportWebContainerView extends PaxWebContainerView {

	/**
	 * <p>List all {@link WebApplicationModel web applications} installed (or failed) into pax-web-runtime.</p>
	 *
	 * <p>The list contains mostly Web Application Bundles (WABs), but it'll probably contain all
	 * <em>web applications</em> - the ones created using Whiteboard registration or direct installation using
	 * {@link org.osgi.service.http.HttpService}.</p>
	 *
	 * @return
	 */
	List<WebApplicationModel> listWebApplications();

	/**
	 * <p>Returns {@link WebApplicationModel} by context path - only real, deployed WABs are considered.
	 * <em>Web applications</em> created using Whiteboard or HttpService methods are not returned.</p>
	 *
	 * @param contextPath
	 * @return
	 */
	WebApplicationModel getWebApplication(String contextPath);

	/**
	 * <p>Returns {@link WebApplicationModel} by bundle Id - only real WABs are considered. <em>Web applications</em>
	 * created using Whiteboard or HttpService methods are not returned.</p>
	 *
	 * @param bundleId
	 * @return
	 */
	WebApplicationModel getWebApplication(long bundleId);

}
