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

import java.util.EventListener;
import java.util.Map;
import javax.servlet.Servlet;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;

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
     * @param servletHandler related servlet handler
     * @param attributes     map of context attributes
     * @param sessionTimeout session timeout in minutes
     */
    void addContext( Handler servletHandler, Map<String, Object> attributes, Integer sessionTimeout );

    String addServlet( String alias, Servlet servlet, Map<String, String> initParams );

    void removeServlet( String name );

    /**
     * @see org.ops4j.pax.web.service.ExtendedHttpService#registerEventListener(java.util.EventListener)
     */
    void addEventListener( EventListener listener );

    /**
     * @see org.ops4j.pax.web.service.ExtendedHttpService#unregisterEventListener(java.util.EventListener)
     */
    void removeEventListener( EventListener listener );
}
