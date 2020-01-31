/*
 * Copyright 2009 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.spi;

import org.ops4j.pax.web.service.spi.model.ServerModel;

/**
 * Main SPI interface used by pax-web-runtime to control actual implementation of HTTP server
 * using provided configuration.
 */
public interface ServerControllerFactory {

	/**
	 * <p>Creates an instance of {@link ServerController} that will control actual server runtime
	 * (Jetty, Tomcat, Undertow)</p>
	 * <p>The configuration is passed via {@link ServerModel} instance.</p>
	 *
	 * @param serverModel
	 * @return
	 */
	ServerController createServerController(ServerModel serverModel);

}
