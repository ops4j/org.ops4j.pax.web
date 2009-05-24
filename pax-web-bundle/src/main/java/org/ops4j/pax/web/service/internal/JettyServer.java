/* Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.internal;

import java.util.Map;
import org.mortbay.jetty.Connector;
import org.osgi.service.http.HttpContext;
import org.ops4j.pax.web.service.internal.model.ErrorPageModel;
import org.ops4j.pax.web.service.internal.model.EventListenerModel;
import org.ops4j.pax.web.service.internal.model.FilterModel;
import org.ops4j.pax.web.service.internal.model.ServletModel;

/**
 * Abstraction of Jetty server.
 *
 * @author Alin Dreghiciu
 * @since 0.2.0
 */
public interface JettyServer
{

    void start();

    void stop();

    /**
     * Adds a connector to Jetty.
     *
     * @param connector a secure connector
     */
    void addConnector( Connector connector );

    /**
     * Adds a context to jetty server.
     *
     * @param attributes        map of context attributes
     * @param sessionTimeout    session timeout in minutes
     * @param sessionCookie     session cookie name. Defaults to JSESSIONID.
     * @param sessionUrl        session URL parameter name. Defaults to jsessionid. If set to null or  "none" no URL
     *                          rewriting will be done.
     * @param sessionWorkerName name appended to session id, used to assist session affinity in a load balancer
     */
    void configureContext( Map<String, Object> attributes,
                           Integer sessionTimeout,
                           String sessionCookie,
                           String sessionUrl,
                           String sessionWorkerName );

    void removeContext( HttpContext httpContext );

    void addServlet( ServletModel model );

    void removeServlet( ServletModel model );

    void addEventListener( EventListenerModel eventListenerModel );

    void removeEventListener( EventListenerModel eventListenerModel );

    void addFilter( FilterModel filterModel );

    void removeFilter( FilterModel filterModel );

    void addErrorPage( ErrorPageModel model );

    void removeErrorPage( ErrorPageModel model );
}
