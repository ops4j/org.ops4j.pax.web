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
package org.ops4j.pax.web.service.internal;

import org.osgi.service.http.HttpContext;

public class HttpResource implements HttpTarget
{
    private String m_alias;
    private String m_name;
    private HttpContext m_httpContext;

    public HttpResource( String alias, String name, HttpContext httpContext )
    {
        //To change body of created methods use File | Settings | File Templates.
        m_alias = alias;
        m_name = name;
        m_httpContext = httpContext;
    }

    public void register( final ServerController serverController )
    {
        // TODO implement register
    }

    public void unregister( ServerController serverController )
    {
        // TODO implement unregister
    }

    public String getAlias()
    {
        return m_alias;
    }

    public String getName()
    {
        return m_name;
    }

    public HttpContext getHttpContext()
    {
        return m_httpContext;
    }

    public Type getType()
    {
        return Type.RESOURCE;
    }
}
