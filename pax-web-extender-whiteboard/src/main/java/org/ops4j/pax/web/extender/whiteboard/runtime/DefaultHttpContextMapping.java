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

import java.util.Map;
import java.util.Arrays;
import org.osgi.service.http.HttpContext;
import org.ops4j.pax.web.extender.whiteboard.HttpContextMapping;

/**
 * Default implementation of {@link HttpContextMapping}.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, March 16, 2008
 */
public class DefaultHttpContextMapping
    implements HttpContextMapping
{

    /**
     * Context id.
     */
    private String m_httpContextId;
    /**
     * Context path.
     */
    private String m_path;
    /**
     * Context parameters.
     */
    private Map<String, String> m_parameters;
    /**
     * Http context.
     */
    private HttpContext m_httpContext;

    /**
     * @see HttpContextMapping#getHttpContextId()
     */
    public String getHttpContextId()
    {
        return m_httpContextId;
    }

    /**
     * @see HttpContextMapping#getPath()
     */
    public String getPath()
    {
        return m_path;
    }

    /**
     * @see HttpContextMapping#getParameters()
     */
    public Map<String, String> getParameters()
    {
        return m_parameters;
    }

    /**
     * @see HttpContextMapping#getHttpContext()
     */
    public HttpContext getHttpContext()
    {
        return m_httpContext;
    }

    /**
     * Setter.
     *
     * @param httpContextId context id; can be null
     */
    public void setHttpContextId( final String httpContextId )
    {
        m_httpContextId = httpContextId;
    }

    /**
     * Setter.
     *
     * @param path context path; can be null
     */
    public void setPath( final String path )
    {
        m_path = path;
    }

    /**
     * Setter.
     *
     * @param parameters context parameters; can be null
     */
    public void setParameters( final Map<String, String> parameters )
    {
        m_parameters = parameters;
    }

    /**
     * Setter.
     *
     * @param httpContext http context; can be null case when a default http context will be used
     */
    public void setHttpContext( HttpContext httpContext )
    {
        m_httpContext = httpContext;
    }

    @Override
    public String toString()
    {
        return new StringBuffer()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "httpContextId=" ).append( m_httpContextId )
            .append( ",path=" ).append( m_path )
            .append( ",params=" ).append( m_parameters )
            .append( ",httpContext=" ).append( m_httpContext )
            .append( "}" )
            .toString();
    }

}