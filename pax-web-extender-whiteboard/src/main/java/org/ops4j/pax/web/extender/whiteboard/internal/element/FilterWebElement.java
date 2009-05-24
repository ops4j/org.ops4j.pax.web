/*
 * Copyright 2007 Damian Golda.
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.whiteboard.internal.element;

import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.whiteboard.FilterMapping;
import org.ops4j.pax.web.extender.whiteboard.internal.util.DictionaryUtils;
import org.ops4j.pax.web.extender.whiteboard.internal.util.WebContainerUtils;
import org.ops4j.pax.web.service.WebContainer;

/**
 * Registers/unregisters {@link FilterMapping} with {@link WebContainer}.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class FilterWebElement
    implements WebElement
{

    /**
     * Filter mapping.
     */
    private FilterMapping m_filterMapping;

    /**
     * Constructor.
     *
     * @param filterMapping filter mapping; cannot be null
     */
    public FilterWebElement( final FilterMapping filterMapping )
    {
        NullArgumentException.validateNotNull( filterMapping, "Filter mapping" );
        m_filterMapping = filterMapping;
    }

    /**
     * Registers filter from http service.
     */
    public void register( final HttpService httpService,
                          final HttpContext httpContext )
        throws Exception
    {
        if( WebContainerUtils.isWebContainer( httpService ) )
        {
            ( (WebContainer) httpService )
                .registerFilter(
                    m_filterMapping.getFilter(),
                    m_filterMapping.getUrlPatterns(),
                    m_filterMapping.getServletNames(),
                    DictionaryUtils.adapt( m_filterMapping.getInitParams() ),
                    httpContext
                );
        }
        else
        {
            throw new UnsupportedOperationException(
                "Internal error: In use HttpService is not an WebContainer (from Pax Web)"
            );
        }
    }

    /**
     * Unregisters filter from http service.
     */
    public void unregister( final HttpService httpService,
                            final HttpContext httpContext )
    {
        if( WebContainerUtils.isWebContainer( httpService ) )
        {
            ( (WebContainer) httpService ).unregisterFilter( m_filterMapping.getFilter() );
        }
    }

    public String getHttpContextId()
    {
        return m_filterMapping.getHttpContextId();
    }

    @Override
    public String toString()
    {
        return new StringBuffer()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "mapping=" ).append( m_filterMapping )
            .append( "}" )
            .toString();
    }

}