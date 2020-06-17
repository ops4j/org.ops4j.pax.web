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
package org.ops4j.pax.web.service.internal.views;

import java.util.Collection;
import javax.servlet.ServletException;

import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.views.PaxWebContainerView;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

/**
 * <p>{@link org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWebContainerView} equivalent for <em>direct
 * Http Service-like registrations</em>.</p>
 *
 * <p>This interface should be treated as internal-only (for test purposes). Its package is not exported.</p>
 */
public interface DirectWebContainerView extends PaxWebContainerView {

	void registerServlet(Collection<HttpContext> contexts, ServletModel build) throws ServletException, NamespaceException;

	void registerFilter(Collection<HttpContext> contexts, FilterModel model) throws ServletException;

	void unregisterServlet(ServletModel model);

	void unregisterFilter(FilterModel model);

}
