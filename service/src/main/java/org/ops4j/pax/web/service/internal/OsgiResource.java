/*  Copyright 2007 Niclas Hedhman.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.mortbay.resource.Resource;
import org.osgi.service.http.HttpContext;

class OsgiResource extends Resource
{
    private String m_alias;
    private String m_name;
    private HttpContext m_httpContext;

    public OsgiResource( String alias, String name, HttpContext httpContext )
    {
        m_alias = alias;
        m_name = name;
        m_httpContext = httpContext;
    }

    public void release()
    {

    }

    public boolean exists()
    {
        return true;
    }

    public boolean isDirectory()
    {
        return false;
    }

    public long lastModified()
    {
        return 0;
    }

    public long length()
    {
        return 0;
    }

    public URL getURL()
    {
        URL url = m_httpContext.getResource( m_name );
        return url;
    }

    public File getFile()
        throws IOException
    {
        return null;
    }

    public String getName()
    {
        return m_name;
    }

    public InputStream getInputStream()
        throws IOException
    {
        return getURL().openStream();
    }

    public OutputStream getOutputStream()
        throws IOException, SecurityException
    {
        return null;
    }

    public boolean delete()
        throws SecurityException
    {
        return false;
    }

    public boolean renameTo( Resource resource )
        throws SecurityException
    {
        return false;
    }

    public String[] list()
    {
        return new String[0];
    }

    public Resource addPath( String alias )
        throws IOException, MalformedURLException
    {
        return new OsgiResource( m_alias + "/" + alias, m_name, m_httpContext );
    }
}
