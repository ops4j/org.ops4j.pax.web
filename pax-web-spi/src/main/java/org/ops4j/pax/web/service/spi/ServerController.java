/*
 * Copyright 2007 Alin Dreghiciu.
 * Copyright 2020 Grzegorz Grzybek.
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
package org.ops4j.pax.web.service.spi;

import java.net.URL;
import javax.servlet.Servlet;

import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.task.Batch;

/**
 * <p>Interface used by pax-web-runtime to interact with actual server runtime. There are three groups of tasks
 * that can be performed:<ol>
 *     <li>Changing state of the server: stop, start, (re)configuration</li>
 *     <li>Sending <em>registration</em> orders related to deployed web applications, contexts, servlets, ...</li>
 *     <li>Listener management.</li>
 * </ol></p>
 *
 * <p>{@link ServerController} SHOULD be created with some {@link Configuration} and then the runtime SHOULD
 * {@link ServerController#configure()} it, making it prepared for {@link ServerController#start()}.</p>
 */
public interface ServerController {

	// --- lifecycle methods

	/**
	 * <p>Get a {@link ServerState state} of the server. Server is {@link ServerState#UNCONFIGURED} initially, but
	 * may later be reconfigured.</p>
	 *
	 * @return
	 */
	ServerState getState();

	/**
	 * <p>Order the server to perform initial configuration. Server can do some initial checks, detect
	 * available options (like HTTP/2 support, native libraries, ALPN, ...) and prepare itself to be
	 * {@link #start() started}.</p>
	 *
	 * <p>After server is configured, it can be started and stopped as needed. Of course this server controller, after
	 * being configured can be used to register contexts, servlets, filters, ...</p>
	 */
	void configure() throws Exception;

	/**
	 * <p>Starts a configured server. Server is considered to be <em>started</em> after it has opened the listening
	 * server socket.</p>
	 */
	void start() throws Exception;

	/**
	 * <p>Stops started server. The server MAY be able to start again, but it's not necessary. Stopping
	 * server controller means it will never be started. Only when new instance will be created, the controller
	 * may start again.</p>
	 */
	void stop() throws Exception;

	/**
	 * Returns {@link Configuration} associated with this {@link ServerController}
	 * @return
	 */
	Configuration getConfiguration();

	// --- listener related methods

	/**
	 * Registers a listener to be notified about server state changes.
	 * @param listener
	 */
	void addListener(ServerListener listener);

	/**
	 * Unregisters previously registered listener
	 *
	 * @param listener
	 */
	void removeListener(ServerListener listener);

//	LifeCycle getContext(OsgiContextModel model);
//
//	void removeContext(HttpContext httpContext);
//
//	void addEventListener(EventListenerModel eventListenerModel);
//
//	void removeEventListener(EventListenerModel eventListenerModel);
//
//	void addErrorPage(ErrorPageModel model);
//
//	void removeErrorPage(ErrorPageModel model);
//
//	void addWelcomFiles(WelcomeFileModel model);
//
//	void removeWelcomeFiles(WelcomeFileModel model);
//
//	void addSecurityConstraintMapping(SecurityConstraintMappingModel secMapModel);
//
//	void removeSecurityConstraintMapping(SecurityConstraintMappingModel secMapModel);

	/**
	 * Each {@link ServerController} can be instructed to perform {@link Batch} of atomic operations.
	 * @param batch
	 */
	void sendBatch(Batch batch);

	/**
	 * Each native Servlet container has own version of <em>default/resource servlet</em> usually implementing such
	 * aspects as resource caching. Such servlet can be created using two (distinct) parameters:<ul>
	 *     <li>{@code urlBase} - when the "base" is found to be proper, absolute path to existing, accessible
	 *         directory</li>
	 *     <li>{@code base} - if the "base" is not an absolute file: directory URL, this parameter will be treated
	 *         as prefix for the resource access as defined in Http/Whiteboard Service specifications (access via
	 *         {@link org.osgi.service.http.HttpContext#getResource(String)} or
	 *         {@link org.osgi.service.http.context.ServletContextHelper#getResource(String)}.</li>
	 * </ul>
	 *
	 * @param urlBase if the "base" is found to be proper, accessible {@code file:} based directory, it is passed
	 *        to this creation method and {@code base} can be safely ignored
	 * @param base According to HttpService and Whiteboard specifications, this is the <em>base</em> location for
	 *        resource fetching. By default this <em>base</em> is prepended to a path and (combined) used as argument
	 *        to {@link org.osgi.service.http.HttpContext#getResource(String)} or
	 *        {@link org.osgi.service.http.context.ServletContextHelper#getResource(String)}. It should not end with
	 *        slash ({@code /}) and can be an empty string (meaning root of the bundle).
	 * @return
	 */
	Servlet createResourceServlet(URL urlBase, String base);

}
