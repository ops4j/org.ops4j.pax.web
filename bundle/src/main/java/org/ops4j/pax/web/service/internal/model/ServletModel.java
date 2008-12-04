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

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Servlet;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.ops4j.pax.web.service.internal.util.Path;

public class ServletModel
    extends Model
{

    private final Servlet m_servlet;
    private final String m_alias;
    private final String[] m_urlPatterns;
    private final Map<String, String> m_initParams;
    private final String m_name;

    public ServletModel( final ContextModel contextModel,
                         final Servlet servlet,
                         final String alias,
                         final Dictionary initParams )
    {
        this( contextModel,
              servlet,
              null,
              new String[]{ aliasAsUrlPattern( alias ) },
              validateAlias( alias ),
              initParams
        );
    }

    public ServletModel( final ContextModel contextModel,
                         final Servlet servlet,
                         final String servletName,
                         final String[] urlPatterns,
                         final String alias,
                         final Dictionary initParams )
    {
        super( contextModel );
        NullArgumentException.validateNotNull( servlet, "Servlet" );
        NullArgumentException.validateNotNull( urlPatterns, "Url patterns" );
        if( urlPatterns.length == 0 )
        {
            throw new IllegalArgumentException( "Registered servlet must have at least one url pattern" );
        }
        m_urlPatterns = Path.normalizePatterns( urlPatterns );
        m_alias = alias;
        m_servlet = servlet;
        m_initParams = convertToMap( initParams );
        String name = servletName;
        if( name == null )
        {
            name = m_initParams.get( WebContainerConstants.SERVLET_NAME );
        }
        if( name == null )
        {
            name = getId();
        }
        m_name = name;
    }

    public String getName()
    {
        return m_name;
    }

    public String[] getUrlPatterns()
    {
        return m_urlPatterns;
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

    /**
     * Validates that aan alias conforms to OSGi specs requirements. See OSGi R4 Http Service specs for details about
     * alias validation.
     *
     * @param alias to validate
     *
     * @return received alias if validation succeeds
     *
     * @throws IllegalArgumentException if validation fails
     */
    private static String validateAlias( final String alias )
    {
        NullArgumentException.validateNotNull( alias, "Alias" );
        if( !alias.startsWith( "/" ) )
        {
            throw new IllegalArgumentException( "Alias does not start with slash (/)" );
        }
        // "/" must be allowed
        if( alias.length() > 1 && alias.endsWith( "/" ) )
        {
            throw new IllegalArgumentException( "Alias ends with slash (/)" );
        }
        return alias;
    }

    /**
     * Converts a Dictionary to a String/String Map.
     *
     * @param dictionary to convert
     *
     * @return converted Map
     */
    private static Map<String, String> convertToMap( final Dictionary dictionary )
    {
        final Map<String, String> converted = new HashMap<String, String>();
        if( dictionary != null )
        {
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

    /**
     * Transforms an alias into a url pattern.
     *
     * @param alias to transform
     *
     * @return url pattern
     */
    private static String aliasAsUrlPattern( final String alias )
    {
        String urlPattern = alias;
        if( urlPattern != null && !urlPattern.equals( "/" ) && !urlPattern.contains( "*" ) )
        {
            if( urlPattern.endsWith( "/" ) )
            {
                urlPattern = urlPattern + "*";
            }
            else
            {
                urlPattern = urlPattern + "/*";
            }
        }
        return urlPattern;
    }

    @Override
    public String toString()
    {
        return new StringBuilder()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "id=" ).append( getId() )
            .append( ",name=" ).append( getName() )
            .append( ",urlPatterns=" ).append( Arrays.toString( m_urlPatterns ) )
            .append( ",alias=" ).append( m_alias )
            .append( ",servlet=" ).append( m_servlet )
            .append( ",initParams=" ).append( m_initParams )
            .append( ",context=" ).append( getContextModel() )
            .append( "}" )
            .toString();
    }

}
