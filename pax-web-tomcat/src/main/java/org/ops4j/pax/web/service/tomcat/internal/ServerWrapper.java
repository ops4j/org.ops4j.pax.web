/*
 * Copyright 2012 Romain Gilles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.ops4j.pax.web.service.tomcat.internal;

import javax.servlet.Servlet;

import org.ops4j.pax.web.service.spi.LifeCycle;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.SecurityConstraintMappingModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.model.elements.WelcomeFileModel;
import org.osgi.service.http.HttpContext;

/**
 * Created with IntelliJ IDEA. User: romain.gilles Date: 6/10/12 Time: 6:27 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ServerWrapper {
	void start();

	void stop();

	void addServlet(ServletModel model);

	void removeServlet(ServletModel model);

	void removeContext(HttpContext httpContext);

	void addErrorPage(ErrorPageModel model);

	void removeErrorPage(ErrorPageModel model);

	void addFilter(FilterModel filterModel);

	void removeFilter(FilterModel filterModel);

	void addEventListener(EventListenerModel eventListenerModel);

	void removeEventListener(EventListenerModel eventListenerModel);

	Servlet createResourceServlet(OsgiContextModel contextModel, String alias,
								  String name);

	void addSecurityConstraintMapping(SecurityConstraintMappingModel secMapModel);

	void removeSecurityConstraintMapping(SecurityConstraintMappingModel secMapModel);

	LifeCycle getContext(OsgiContextModel model);

	void addWelcomeFiles(WelcomeFileModel model);

	void removeWelcomeFiles(WelcomeFileModel model);

}
