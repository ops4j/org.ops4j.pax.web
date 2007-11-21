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

import java.util.Dictionary;
import java.util.EventListener;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

public class StoppedHttpService
    implements StoppableHttpService
{

    private static final Log LOG = LogFactory.getLog( StoppedHttpService.class );

    public StoppedHttpService()
    {
        LOG.info( "Changing HttpService state to " + this );
    }

    public void registerServlet(
        final String alias,
        final Servlet servlet,
        final Dictionary initParams,
        final HttpContext httpContext )
        throws ServletException, NamespaceException
    {
        LOG.warn( "Http service has already been stopped" );
    }

    public void registerResources(
        final String alias,
        final String name,
        final HttpContext httpContext )
        throws NamespaceException
    {
        LOG.warn( "Http service has already been stopped" );
    }

    public void unregister( final String alias )
    {
        LOG.warn( "Http service has already been stopped" );
    }

    public HttpContext createDefaultHttpContext()
    {
        LOG.warn( "Http service has already been stopped" );
        return null;
    }

    public void stop()
    {
        LOG.warn( "Http service has already been stopped" );
    }

    /**
     * Does nothing.
     *
     * @see org.ops4j.pax.web.service.ExtendedHttpService#registerEventListener(java.util.EventListener)
     */
    public void registerEventListener( final EventListener listener )
    {
        LOG.warn( "Http service has already been stopped" );
    }

    /**
     * Does nothing.
     *
     * @see org.ops4j.pax.web.service.ExtendedHttpService#unregisterEventListener(java.util.EventListener)
     */
    public void unregisterEventListener( final EventListener listener )
    {
        LOG.warn( "Http service has already been stopped" );
    }
}