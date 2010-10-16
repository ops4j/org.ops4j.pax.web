/*
 * Copyright 2007 Damian Golda.
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
package org.ops4j.pax.web.extender.war.internal;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.war.internal.model.WebAppMimeMapping;
import org.ops4j.pax.web.extender.war.internal.util.Path;

/**
 * Default implementation of HttpContext, which gets resources from the bundle that registered the service.
 * It delegates to the provided http context beside for getResource that should look in the original bundle.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, December 27, 2007
 */
class WebAppHttpContext implements HttpContext
{

    /**
     * Logger.
     */
    final Log LOG = LogFactory.getLog( this.getClass() );

    /**
     * The bundle that registered the service.
     */
    final Bundle m_bundle;
    /**
     * The http context to delegate to.
     */
    private final HttpContext m_httpContext;
    /**
     * Mime mappings.
     */
    private final Map<String, String> m_mimeMappings;

    /**
     * Creates a new http context that delegates to the specified http context but get's resources from the specified
     * bundle.
     *
     * @param httpContext  wrapped http context
     * @param bundle       bundle to search for resorce
     * @param mimeMappings an array of mime mappings
     *
     * @throws NullArgumentException if http context or bundle is null
     */
    WebAppHttpContext( final HttpContext httpContext, final Bundle bundle, final WebAppMimeMapping[] mimeMappings )
    {
        NullArgumentException.validateNotNull( httpContext, "http context" );
        NullArgumentException.validateNotNull( bundle, "Bundle" );
        if (LOG.isDebugEnabled())
        	LOG.debug("Creating WebAppHttpContext for "+httpContext);
        m_httpContext = httpContext;
        m_bundle = bundle;
        m_mimeMappings = new HashMap<String, String>();
        for( WebAppMimeMapping mimeMapping : mimeMappings )
        {
            m_mimeMappings.put( mimeMapping.getExtension(), mimeMapping.getMimeType() );
        }
    }

    /**
     * Delegate to wrapped http context.
     *
     * @see org.osgi.service.http.HttpContext#handleSecurity(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public boolean handleSecurity( final HttpServletRequest request, final HttpServletResponse response )
        throws IOException
    {
        return m_httpContext.handleSecurity( request, response );
    }

    /**
     * Searches for the resource in the bundle that published the service.
     *
     * @see org.osgi.service.http.HttpContext#getResource(String)
     */
    public URL getResource( final String name )
    {
        final String normalizedName = Path.normalizeResourcePath( name );
        LOG.debug(
            "Searching bundle [" + m_bundle + "] for resource [" + name + "], normalized to [" + normalizedName + "]"
        );
        URL url = null;
        if( normalizedName != null && normalizedName.trim().length() > 0 )
        {
            String path = "";
            String file = normalizedName;
            int idx = file.lastIndexOf( '/' );
            if( idx > 0 )
            {
                path = normalizedName.substring( 0, idx );
                file = normalizedName.substring( idx + 1 );
            }
            Enumeration e = m_bundle.findEntries( path, file, false );
            if( e != null && e.hasMoreElements() )
            {
                url = (URL) e.nextElement();
            }
        }
        if( url != null )
        {
            LOG.debug( "Resource found as url [" + url + "]" );
        }
        else
        {
            LOG.debug( "Resource not found" );
        }
        return url;
    }

    /**
     * Find the mime type in the mime mappings. If not found delegate to wrapped http context.
     *
     * @see org.osgi.service.http.HttpContext#getMimeType(String)
     */
    public String getMimeType( final String name )
    {
        String mimeType = null;
        if( name != null && name.length() > 0 && name.contains( "." ) )
        {
            final String[] segments = name.split( "\\." );
            mimeType = m_mimeMappings.get( segments[ segments.length - 1 ] );
        }
        if( mimeType == null )
        {
            mimeType = m_httpContext.getMimeType( name );
        }
        return mimeType;
    }
}