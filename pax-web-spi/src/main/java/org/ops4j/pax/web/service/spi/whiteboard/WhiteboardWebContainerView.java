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
package org.ops4j.pax.web.service.spi.whiteboard;

import java.util.List;
import javax.servlet.ServletException;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.views.PaxWebContainerView;
import org.osgi.framework.Bundle;
import org.osgi.service.http.NamespaceException;

/**
 * <p>SPI interface which is used by pax-web-extender-whiteboard to gain lower-level access to
 * {@link org.ops4j.pax.web.service.WebContainer} managed and lifecycle-scoped to pax-web-runtime bundle.</p>
 *
 * <p>This is a loose (as loose as possible) coupling between pax-web-runtime (an implementation of HttpService
 * specification) and pax-web-extender-whiteboard (an implementation of Whiteboard Service specification).</p>
 */
public interface WhiteboardWebContainerView extends PaxWebContainerView {

	/**
	 * Returns all the contexts (customized to unified {@link OsgiContextModel} internal representation) managed
	 * at Http Service level. These are either bound to given bundle or "shared"
	 * @param bundle
	 * @return
	 */
	List<OsgiContextModel> getOsgiContextModels(Bundle bundle);

	/**
	 * One-stop method to register a {@link Servlet} described using {@link ServletModel}. {@link ServletModel}
	 * should always be associated with target (one or many) {@link OsgiContextModel}, because differently than with
	 * {@link org.osgi.service.http.HttpService} scenario, contexts are targeted by logical name (or LDAP selector) and
	 * not as any instance.
	 * @param contexts
	 * @param servletModel
	 */
	void registerServlet(ServletModel servletModel) throws ServletException, NamespaceException;

	/**
	 * One-stop method to register a {@link Filter} described using {@link FilterModel}. {@link FilterModel}
	 * should always be associated with target (one or many) {@link OsgiContextModel}.
	 * @param contexts
	 * @param filterModel
	 */
	void registerFilter(FilterModel filterModel) throws ServletException;

}
