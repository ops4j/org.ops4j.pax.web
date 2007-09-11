/*  Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.osgi.service.http.HttpContext;

public class HttpServiceContext extends Context
{

    private static final Log m_logger = LogFactory.getLog( HttpServiceContext.class );

    public HttpServiceContext( final Server server, final String contextPath, final int options )
    {
        super( server, contextPath, options );
        _scontext = new SContext();
    }

    @SuppressWarnings( { "deprecation" } )
    public class SContext extends Context.SContext
    {

        @Override
        public URL getResource( final String path )
        {
            if( m_logger.isInfoEnabled() )
            {
                m_logger.info( "getting resource: [" + path + "]" );
            }
            HttpContext httpContext = HttpServiceHandler.getActiveHttpContext();
            if( httpContext == null )
            {
                throw new IllegalStateException( "unexpected active http context" );
            }
            URL resource = httpContext.getResource( path );
            if( m_logger.isInfoEnabled() )
            {
                m_logger.info( "found resource: " + resource );
            }
            return resource;
        }

        @Override
        public InputStream getResourceAsStream( final String path )
        {
            URL url = getResource( path );
            if( url != null )
            {
                try
                {
                    return url.openStream();
                }
                catch( IOException e )
                {
                    return null;
                }
            }
            return null;
        }

        @Override
        public String getMimeType( final String name )
        {
            if( m_logger.isInfoEnabled() )
            {
                m_logger.info( "getting mime type for: [" + name + "]" );
            }
            HttpContext httpContext = HttpServiceHandler.getActiveHttpContext();
            if( httpContext == null )
            {
                throw new IllegalStateException( "unexpected active http context" );
            }
            return httpContext.getMimeType( name );
        }
    }

}
