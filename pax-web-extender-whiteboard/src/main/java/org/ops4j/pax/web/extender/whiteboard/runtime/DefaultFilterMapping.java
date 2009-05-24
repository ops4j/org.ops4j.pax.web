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
import javax.servlet.Filter;
import org.ops4j.pax.web.extender.whiteboard.FilterMapping;

/**
 * Default implementation of {@link FilterMapping}.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class DefaultFilterMapping
    implements FilterMapping
{

    /**
     * Http Context id.
     */
    private String m_httpContextId;
    /**
     * Filter.
     */
    private Filter m_filter;
    /**
     * Url patterns.
     */
    private String[] m_urlPatterns;
    /**
     * Servlets names the filter was registered for.
     */
    private String[] m_servletNames;
    /**
     * Initialization parameters.
     */
    private Map<String, String> m_initParams;

    /**
     * @see FilterMapping#getHttpContextId()
     */
    public String getHttpContextId()
    {
        return m_httpContextId;
    }

    /**
     * @see FilterMapping#getFilter()
     */
    public Filter getFilter()
    {
        return m_filter;
    }

    /**
     * @see FilterMapping#getUrlPatterns()
     */
    public String[] getUrlPatterns()
    {
        return m_urlPatterns;
    }

    /**
     * @see FilterMapping#getServletNames()
     */
    public String[] getServletNames()
    {
        return m_servletNames;
    }

    /**
     * @see FilterMapping#getInitParams()
     */
    public Map<String, String> getInitParams()
    {
        return m_initParams;
    }

    /**
     * Setter.
     *
     * @param httpContextId id of the http context this filter belongs to
     */
    public void setHttpContextId( final String httpContextId )
    {
        m_httpContextId = httpContextId;
    }

    /**
     * Setter.
     *
     * @param filter mapped filter
     */
    public void setFilter( final Filter filter )
    {
        m_filter = filter;
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
     * Setter.
     *
     * @param servletNames array of servlet aliases
     */
    public void setServletNames( final String... servletNames )
    {
        m_servletNames = servletNames;
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
            .append( ",servletNames=" ).append( Arrays.deepToString( m_servletNames ) )
            .append( ",initParams=" ).append( m_initParams )
            .append( ",filter=" ).append( m_filter )
            .append( "}" )
            .toString();
    }

}