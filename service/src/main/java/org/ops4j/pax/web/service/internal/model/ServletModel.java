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
import javax.servlet.Servlet;
import org.osgi.service.http.HttpContext;
import org.ops4j.pax.web.service.internal.util.Assert;

public class ServletModel
    extends Model
{

    private final Servlet m_servlet;
    private final String m_alias;
    private final Map<String, String> m_initParams;

    public ServletModel( final HttpContext httpContext,
                         final Servlet servlet,
                         final String alias,
                         final Dictionary initParams )
    {
        super( httpContext );
        validateAlias( alias );
        Assert.notNull( "Servlet cannot be null", servlet );
        m_alias = alias;
        m_servlet = servlet;
        m_initParams = convertToMap( initParams );
    }

    public String getAlias()
    {
        return m_alias;
    }

    public Servlet getServlet()
    {
        return m_servlet;
    }

    public Map<String, String> getInitParams()
    {
        return m_initParams;
    }

    private void validateAlias( final String alias )
    {
        Assert.notNull( "alias == null", alias );
        if( !alias.startsWith( "/" ) )
        {
            throw new IllegalArgumentException( "Alias does not start with slash (/)" );
        }
        // "/" must be allowed
        if( alias.length() > 1 && alias.endsWith( "/" ) )
        {
            throw new IllegalArgumentException( "Alias ends with slash (/)" );
        }
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
        return new StringBuilder()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "id=" ).append( m_id )
            .append( ",alias=" ).append( m_alias )
            .append( ",servlet=" ).append( m_servlet )
            .append( ",initParams=" ).append( m_initParams )
            .append( ",httpContext=" ).append( m_httpContext )
            .append( "}" )
            .toString();
    }

}
