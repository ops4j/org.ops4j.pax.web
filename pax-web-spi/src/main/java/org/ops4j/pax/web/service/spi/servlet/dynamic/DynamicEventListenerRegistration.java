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
package org.ops4j.pax.web.service.spi.servlet.dynamic;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;

/**
 * Dynamic event listenr registration.
 */
public class DynamicEventListenerRegistration {

	private final EventListenerModel model;
	private final OsgiContextModel osgiContextModel;

	public DynamicEventListenerRegistration(EventListenerModel model, OsgiContextModel osgiContextModel) {
		this.model = model;
		// "close" the context list
		this.model.getContextModels();
		this.osgiContextModel = osgiContextModel;
	}

	public EventListenerModel getModel() {
		return model;
	}

}
