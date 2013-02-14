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
package org.ops4j.pax.web.service.spi.model;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.Filter;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.ops4j.pax.web.service.spi.util.ConversionUtil;
import org.ops4j.pax.web.service.spi.util.Path;

public class FilterModel
    extends Model
{

    private static final Set<String> VALID_DISPATCHER_VALUES = new HashSet<String>()
    {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{
            add( "request" );
            add( "forward" );
            add( "include" );
            add( "error" );
        }
    };

    private final Filter m_filter;
    private final String[] m_urlPatterns;
    private final String[] m_servletNames;
    private final Map<String, String> m_initParams;
    private final String m_name;
    private final Set<String> m_dispatcher = new HashSet<String>();

    @SuppressWarnings("rawtypes")
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
        m_initParams = ConversionUtil.convertToMap( initParams );
        String name = m_initParams.get( WebContainerConstants.FILTER_NAME );
        if( name == null )
        {
            name = getId();
        }
        m_name = name;
        setupDispatcher();
    }

    /*

     */
    private void setupDispatcher()
    {
        String dispatches = m_initParams.get( WebContainerConstants.FILTER_MAPPING_DISPATCHER );
        if( dispatches != null && dispatches.trim().length() > 0 )
        {
            if( dispatches.indexOf( "," ) > -1 )
            {
                // parse
                StringTokenizer tok = new StringTokenizer( dispatches.trim(), "," );
                while( tok.hasMoreTokens() )
                {
                    String element = tok.nextToken();
                    if( element != null && element.trim().length() > 0 )
                    {
                        if( VALID_DISPATCHER_VALUES.contains( element.trim().toLowerCase() ) )
                        {
                            m_dispatcher.add( element.trim() );
                        }
                        else
                        {
                            throw new IllegalArgumentException( "Incorrect value of dispatcher " + element.trim() );
                        }
                    }
                }
            }
            else
            {
                if( VALID_DISPATCHER_VALUES.contains( dispatches.trim().toLowerCase() ) )
                {
                    m_dispatcher.add( dispatches.trim() );
                }
                else
                {
                    throw new IllegalArgumentException( "Incorrect value of dispatcher " + dispatches.trim() );
                }
            }
        }
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

    public String[] getDispatcher()
    {
        return m_dispatcher.toArray( new String[m_dispatcher.size()] );
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
