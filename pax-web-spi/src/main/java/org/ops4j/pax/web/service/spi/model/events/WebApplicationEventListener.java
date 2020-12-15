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
package org.ops4j.pax.web.service.spi.model.events;

/**
 * <p>128.5 Events - a listener interface to send and receive (by callback) events related to entire web
 * application.</p>
 *
 * <p>For events related to components of web applications (elements and contexts), see {@link WebElementEventListener}.</p>
 */
public interface WebApplicationEventListener {

	/**
	 * Notification about registration (failed or successful) and unregistration of a web application (WAB)
	 * @param event
	 */
	void webEvent(WebApplicationEvent event);

}
