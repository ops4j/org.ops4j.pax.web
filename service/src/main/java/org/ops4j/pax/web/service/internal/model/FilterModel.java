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
import javax.servlet.Filter;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.ops4j.pax.web.service.internal.util.Path;
import org.ops4j.lang.NullArgumentException;

public class FilterModel
    extends Model
{

    private final Filter m_filter;
    private final String[] m_urlPatterns;
    private final String[] m_servletNames;
    private final Map<String, String> m_initParams;
    private final String m_name;

    public FilterModel( final ContextModel contextModel,
                        final Filter filter,
                        final String[] urlPatterns,
                        final String[] servletNames,
                        final Dictionary initParams )
    {
        super( contextModel );
        NullArgumentException.validateNotNull( filter, "Filter" ); 
        if( urlPatterns == null && servletNames == null )
        {
            throw new IllegalArgumentException(
                "Registered filter must have at least one url pattern or servlet name mapping"
            );
        }

        m_filter = filter;
        m_urlPatterns = Path.normalizePatterns( urlPatterns );
        m_servletNames = servletNames;
        m_initParams = convertToMap( initParams );
        String name = m_initParams.get( WebContainerConstants.FILTER_NAME );
        if( name == null )
        {
            name = getId();
        }
        m_name = name;
    }

    public Filter getFilter()
    {
        return m_filter;
    }

    public String getName()
    {
        return m_name;
    }

    public String[] getUrlPatterns()
    {
        return m_urlPatterns;
    }

    public String[] getServletNames()
    {
        return m_servletNames;
    }

    public Map<String, String> getInitParams()
    {
        return m_initParams;
    }

    private static Map<String, String> convertToMap( final Dictionary dictionary )
    {
        Map<String, String> converted = new HashMap<String, String>();
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

    @Override
    public String toString()
    {
        return new StringBuilder()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "id=" ).append( getId() )
            .append( ",urlPatterns=" ).append( Arrays.toString( m_urlPatterns ) )
            .append( ",servletNames=" ).append( Arrays.toString( m_servletNames ) )
            .append( ",filter=" ).append( m_filter )
            .append( ",context=" ).append( getContextModel() )
            .append( "}" )
            .toString();
    }

}
