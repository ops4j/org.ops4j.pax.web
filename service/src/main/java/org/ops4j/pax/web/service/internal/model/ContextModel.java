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
package org.ops4j.pax.web.service.internal.model;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.osgi.service.http.HttpContext;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.WebContainerConstants;

/**
 * Models a servlet context related to an http context.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, December 29, 2007
 */
public class ContextModel
{

    private final HttpContext m_httpContext;
    private final ClassLoader m_classLoader;
    private final Map<String, String> m_contextParams;
    private String m_contextName;

    public ContextModel( final HttpContext httpContext, final ClassLoader classLoader )
    {
        NullArgumentException.validateNotNull( httpContext, "Http context" );
        NullArgumentException.validateNotNull( classLoader, "Class loader" );
        m_classLoader = classLoader;
        m_httpContext = httpContext;
        m_contextParams = new HashMap<String, String>();
        m_contextName = "";
    }

    public HttpContext getHttpContext()
    {
        return m_httpContext;
    }

    public ClassLoader getClassLoader()
    {
        return m_classLoader;
    }

    public void setContextParams( final Dictionary contextParams )
    {
        if( contextParams != null && !contextParams.isEmpty() )
        {
            m_contextParams.clear();
            final Enumeration keys = contextParams.keys();
            while( keys.hasMoreElements() )
            {
                final Object key = keys.nextElement();
                final Object value = contextParams.get( key );
                if( !( key instanceof String ) || !( value instanceof String ) )
                {
                    throw new IllegalArgumentException( "Context params keys and values must be Strings" );
                }
                m_contextParams.put( (String) key, (String) value );
            }
        }
        m_contextName = m_contextParams.get( WebContainerConstants.CONTEXT_NAME );
        if( m_contextName != null )
        {
            m_contextName = m_contextName.trim();
        }
        else
        {
            m_contextName = "";
        }
        // TODO validate context name (no "/" ?)
    }

    /**
     * Getter.
     *
     * @return map of context params
     */
    public Map<String, String> getContextParams()
    {
        return m_contextParams;
    }

    /**
     * Getter.
     *
     * @return context name
     */
    public String getContextName()
    {
        return m_contextName;
    }

    @Override
    public String toString()
    {
        return new StringBuilder()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "name=" ).append( m_contextName )
            .append( ",httpContext=" ).append( m_httpContext )
            .append( ",contextParams=" ).append( m_contextParams )
            .append( "}" )
            .toString();
    }

}
