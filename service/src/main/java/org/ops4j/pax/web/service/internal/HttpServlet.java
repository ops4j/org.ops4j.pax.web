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
import javax.servlet.Servlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.http.HttpContext;

public class HttpServlet implements HttpTarget
{

    private static final Log m_logger = LogFactory.getLog( HttpServlet.class );

    private String m_alias;
    private Servlet m_servlet;
    private Dictionary m_initParams;
    private HttpContext m_httpContext;
    private String m_servletHolderName;

    public HttpServlet(
        final String alias,
        final Servlet servlet,
        final Dictionary initParams,
        final HttpContext httpContext )
    {
        m_alias = alias;
        m_servlet = servlet;
        m_initParams = initParams;
        m_httpContext = httpContext;
    }

    public void register( final ServerController serverController )
    {
        Assert.notNull( "serverController == null", serverController );
        m_servletHolderName = serverController.addServlet( m_alias, m_servlet );
    }

    public void unregister( final ServerController serverController )
    {
        Assert.notNull( "serverController == null", serverController );
        serverController.removeServlet( m_servletHolderName );
    }

    public String getAlias()
    {
        return m_alias;
    }

    public HttpContext getHttpContext()
    {
        return m_httpContext;
    }

    public Type getType()
    {
        return Type.SERVLET;
    }

    public Servlet getServlet()
    {
        return m_servlet;
    }
}
