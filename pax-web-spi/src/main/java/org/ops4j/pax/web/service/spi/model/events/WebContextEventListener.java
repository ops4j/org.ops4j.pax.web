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
package org.ops4j.pax.web.service.spi.model.events;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;

/**
 * <p>Listener interface for one particular task - passing information from pax-web-runtime to
 * pax-web-extender-whiteboard about {@link OsgiContextModel} instances
 * created in pax-web-extender-war (I know it's ugly, but it's almost the last thing before Pax Web 8!).</p>
 *
 * @author Grzegorz Grzybek
 */
public interface WebContextEventListener {

	/**
	 * Notification about registration of WAB's {@link OsgiContextModel}
	 * @param model
	 */
	void wabContextRegistered(OsgiContextModel model);

	/**
	 * Notification about unregistration of WAB's {@link OsgiContextModel}
	 * @param model
	 */
	void wabContextUnregistered(OsgiContextModel model);

}
