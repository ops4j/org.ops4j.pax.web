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
package org.ops4j.pax.web.service.spi.model.views;

import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.views.PaxWebContainerView;

/**
 * Interface with selected methods from {@link org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWebContainerView}
 * used for dynamic addServlet/addFilter/addListener methods of {@link javax.servlet.ServletContext}.
 */
public interface DynamicJEEWebContainerView extends PaxWebContainerView {

	/**
	 * Dynamically registers a servlet without a way to unregister it. Such servlet will be unregistered when
	 * <em>source bundle</em> (determined internally) stops.
	 * @param model
	 */
	void registerServlet(ServletModel model);

	/**
	 * Dynamically registers a filter without a way to unregister it. Such filter will be unregistered when
	 * <em>source bundle</em> (determined internally) stops.
	 * @param model
	 */
	void registerFilter(FilterModel model);

	/**
	 * Dynamically registers a listener without a way to unregister it. Such listener will be unregistered when
	 * <em>source bundle</em> (determined internally) stops.
	 * @param model
	 */
	void registerListener(EventListenerModel model);

}
