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
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;
import javax.servlet.Servlet;
import org.osgi.service.http.HttpContext;
import org.ops4j.pax.web.service.internal.logging.LogFactory;
import org.ops4j.pax.web.service.internal.logging.Log;

public class RegistrationImpl implements Registration
{

    private static final Log m_logger = LogFactory.getLog( RegistrationImpl.class );

    private String m_alias;
    private Servlet m_servlet;
    private Dictionary m_initParams;
    private HttpContext m_httpContext;
    private String m_servletHolderName;
    private String m_name;

    public RegistrationImpl(
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

    public RegistrationImpl(
        final String alias,
        final String name,
        final Servlet servlet,
        final HttpContext httpContext )
    {
        m_alias = alias;
        m_name = name;
        m_servlet = servlet;
        m_httpContext = httpContext;
    }

    public void register( final ServerController serverController )
    {
        Assert.notNull( "serverController == null", serverController );
        m_servletHolderName = serverController.addServlet( m_alias, m_servlet, convertToMap( m_initParams ) );
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

    public String getName()
    {
        return m_name;
    }

    public HttpContext getHttpContext()
    {
        return m_httpContext;
    }

    public Servlet getServlet()
    {
        return m_servlet;
    }

    private Map<String, String> convertToMap( final Dictionary dictionary)
    {
        Map<String, String> converted = null;
        if ( dictionary != null )
        {
            converted = new HashMap<String, String>();
            Enumeration enumeration = dictionary.keys();
            try
            {
                while( enumeration.hasMoreElements() )
                {
                    String key = (String) enumeration.nextElement();
                    String value = (String) dictionary.get( key );
                    converted.put( key, value );
                }
            }
            catch( ClassCastException e )
            {
                throw new IllegalArgumentException( "Invalid init params for the servlet. The key and value must be Strings.");
            }
        }
        return converted;
    }
    
}
