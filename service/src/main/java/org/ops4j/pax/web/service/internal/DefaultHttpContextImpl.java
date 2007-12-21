/* Copyright 2007 Niclas Hedhman.
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
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

public class DefaultHttpContextImpl
    implements HttpContext
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( DefaultHttpContextImpl.class );

    private final Bundle m_bundle;

    public DefaultHttpContextImpl( Bundle bundle )
    {
        m_bundle = bundle;
    }

    public boolean handleSecurity( HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse )
        throws IOException
    {
        return true;
    }

    public URL getResource( final String name )
    {
        final String normalizedname = Utils.normalizeResourcePath( name );
        LOG.debug( "Searching bundle [" + m_bundle + "] for resource [" + normalizedname + "]" );
        return m_bundle.getResource( normalizedname );
    }

    public String getMimeType( String name )
    {
        return null;
    }
}
