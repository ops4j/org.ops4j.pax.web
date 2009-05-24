/*
 * Copyright 2008 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.whiteboard.runtime;

import java.util.Arrays;
import java.util.Map;
import javax.servlet.Servlet;
import org.ops4j.pax.web.extender.whiteboard.ServletMapping;

/**
 * Default implementation of {@link ServletMapping}.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class DefaultServletMapping
    implements ServletMapping
{

    /**
     * Http Context id.
     */
    private String m_httpContextId;
    /**
     * Servlet.
     */
    private Servlet m_servlet;

    /**
     * Servlet Name.
     */
    private String m_servletName;

    /**
     * Alias.
     */
    private String m_alias;
    /**
     * Url patterns.
     */
    private String[] m_urlPatterns;
    /**
     * Initialization parameters.
     */
    private Map<String, String> m_initParams;

    /**
     * @see ServletMapping#getHttpContextId()
     */
    public String getHttpContextId()
    {
        return m_httpContextId;
    }

    /**
     * @see ServletMapping#getServlet()
     */
    public Servlet getServlet()
    {
        return m_servlet;
    }

    /**
     * @see ServletMapping#getServletName()
     */
    public String getServletName()
    {
        return m_servletName;
    }

    /**
     * @see ServletMapping#getAlias()
     */
    public String getAlias()
    {
        return m_alias;
    }

    /**
     * @see ServletMapping#getUrlPatterns()
     */
    public String[] getUrlPatterns()
    {
        return m_urlPatterns;
    }

    /**
     * @see ServletMapping#getInitParams()
     */
    public Map<String, String> getInitParams()
    {
        return m_initParams;
    }

    /**
     * Setter.
     *
     * @param httpContextId id of the http context this servlet belongs to
     */
    public void setHttpContextId( final String httpContextId )
    {
        m_httpContextId = httpContextId;
    }

    /**
     * Setter.
     *
     * @param servlet mapped servlet
     */
    public void setServlet( final Servlet servlet )
    {
        m_servlet = servlet;
    }

    /**
     * Setter.
     *
     * @param Name of the Servlet being mapped.
     */
    public void setServletName( final String servletName )
    {
        m_servletName = servletName;
    }

    /**
     * Setter.
     *
     * @param alias alias this servlet maps to
     */
    public void setAlias( final String alias )
    {
        m_alias = alias;
    }

    /**
     * Setter.
     *
     * @param urlPatterns array of url patterns
     */
    public void setUrlPatterns( final String... urlPatterns )
    {
        m_urlPatterns = urlPatterns;
    }

    /**
     * Seter.
     *
     * @param initParams map of initialization parameters
     */
    public void setInitParams( final Map<String, String> initParams )
    {
        m_initParams = initParams;
    }

    @Override
    public String toString()
    {
        return new StringBuffer()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "httpContextId=" ).append( m_httpContextId )
            .append( ",urlPatterns=" ).append( Arrays.deepToString( m_urlPatterns ) )
            .append( ",initParams=" ).append( m_initParams )
            .append( ",servlet=" ).append( m_servlet )
            .append( ", alias=").append( m_alias)
            .append( ", servletName").append(m_servletName)
            .append( "}" )
            .toString();
    }

}