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

import org.ops4j.pax.web.service.spi.config.Configuration;

/**
 * <p>Main SPI interface used by pax-web-runtime to create {@link ServerController} which will be the only
 * way to configure and control targe server runtime.</p>
 */
public interface ServerControllerFactory {

	/**
	 * <p>Creates an instance of {@link ServerController} that will control actual server runtime
	 * (Jetty, Tomcat, Undertow). Can be invoked multiple times for different configurations, but
	 * actual implementation has to decide if the controller is always created or possibly cached.</p>
	 *
	 * <p>Returned {@link ServerController} should always be associated with given configuration, but
	 * {@link ServerController#configure()} method still has to be called to give the controller a chance
	 * for some explicit initialization before it actually starts.</p>
	 *
	 * @param configuration Pax Web configuration that specifies global aspects of target runtime
	 * @return
	 */
	ServerController createServerController(Configuration configuration);

	/**
	 * Cleanup method, because we don't want controllers to live forever.
	 *
	 * @param controller
	 * @param configuration
	 */
	default void releaseServerController(ServerController controller, Configuration configuration) {
	}

}
