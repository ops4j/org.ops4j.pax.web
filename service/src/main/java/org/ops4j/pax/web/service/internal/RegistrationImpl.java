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
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Servlet;
import org.osgi.service.http.HttpContext;

public class RegistrationImpl implements Registration
{

    private final String m_alias;
    private final Servlet m_servlet;
    private Dictionary m_initParams;
    private final HttpContext m_httpContext;
    private String m_servletHolderName;
    private String m_name;
    private final Registrations m_registrations;

    public RegistrationImpl(
        final String alias,
        final Servlet servlet,
        final Dictionary initParams,
        final HttpContext httpContext,
        final Registrations registrations )
    {
        m_alias = alias;
        m_servlet = servlet;
        m_initParams = initParams;
        m_httpContext = httpContext;
        m_registrations = registrations;
    }

    public RegistrationImpl(
        final String alias,
        final String name,
        final Servlet servlet,
        final HttpContext httpContext,
        final Registrations registrations )
    {
        m_alias = alias;
        m_name = name;
        m_servlet = servlet;
        m_httpContext = httpContext;
        m_registrations = registrations;
    }

    public void register( final ServerController serverController )
    {
        Assert.notNull( "serverController == null", serverController );
        m_servletHolderName =
            serverController.addServlet( m_alias, m_servlet, convertToMap( m_initParams ), m_httpContext,
                                         m_registrations
            );
    }

    public void unregister( final ServerController serverController )
    {
        Assert.notNull( "serverController == null", serverController );
        serverController.removeServlet( m_servletHolderName, m_httpContext );
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

    private Map<String, String> convertToMap( final Dictionary dictionary )
    {
        Map<String, String> converted = null;
        if( dictionary != null )
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
                throw new IllegalArgumentException(
                    "Invalid init params for the servlet. The key and value must be Strings."
                );
            }
        }
        return converted;
    }

    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "alias=").append(m_alias );
        if( m_servlet != null )
        {
            builder.append( ", servlet=").append(m_servlet );
        }
        if( m_name != null )
        {
            builder.append( ", resource=").append(m_name );
        }
        builder.append( "}" );
        return builder.toString();
    }

}
