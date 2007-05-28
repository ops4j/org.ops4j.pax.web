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

import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.osgi.service.http.HttpContext;
import javax.servlet.Servlet;
import java.util.Dictionary;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;

class ServletRegistration extends Registration
{
    private String m_alias;
    private ServletHandler m_servletHandler;

    public ServletRegistration( String alias, Servlet servlet, Dictionary initParams, HttpContext context )
        throws Exception
    {
        super( alias, context );
        m_servletHandler = new ServletHandler();
        ServletHolder holder = new ServletHolder( servlet );
        Map<String, String> init = new HashMap<String, String>();
        if( initParams != null )
        {
            Enumeration enumeration = initParams.keys();
            while( enumeration.hasMoreElements() )
            {
                String key = (String) enumeration.nextElement();
                String value = (String) initParams.get( key );
                init.put( key, value );
            }
        }
        holder.setInitParameters( init );
        m_servletHandler.addServletWithMapping( holder, alias );
        m_servletHandler.start();
    }

    public String getAlias()
    {
        return m_alias;
    }

    public ServletHandler getServletHandler()
    {
        return m_servletHandler;
    }
}
