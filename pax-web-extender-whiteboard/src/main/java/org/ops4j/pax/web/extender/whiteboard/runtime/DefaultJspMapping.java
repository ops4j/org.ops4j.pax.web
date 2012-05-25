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

import org.ops4j.pax.web.extender.whiteboard.JspMapping;

/**
 * Default implementation of {@link JspMapping}.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, March 15, 2008
 */
public class DefaultJspMapping
    implements JspMapping
{

    /**
     * Http Context id.
     */
    private String m_httpContextId;
    /**
     * Url patterns.
     */
    private String[] m_urlPatterns;
    /**
     * Initialization parameters.
     */
    private Map<String, String> m_initParams;

    /**
     * @see JspMapping#getInitParams()
     */
    public Map<String, String> getInitParams()
    {
        return m_initParams;
    }
    
    /**
     * @see JspMapping#getHttpContextId()
     */
    public String getHttpContextId()
    {
        return m_httpContextId;
    }

    /**
     * @see JspMapping#getUrlPatterns()
     */
    public String[] getUrlPatterns()
    {
        return m_urlPatterns;
    }

    /**
     * Setter.
     *
     * @param httpContextId id of the http context this jsp belongs to
     */
    public void setHttpContextId( final String httpContextId )
    {
        m_httpContextId = httpContextId;
    }

    /**
     * Setter.
     *
     * @param urlPatterns array of url patterns
     */
    public void setUrlPatterns( String... urlPatterns )
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
            .append( "}" )
            .toString();
    }

}