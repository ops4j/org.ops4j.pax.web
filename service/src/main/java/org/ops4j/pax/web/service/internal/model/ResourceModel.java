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

import javax.servlet.Servlet;
import org.osgi.service.http.HttpContext;
import org.ops4j.pax.web.service.internal.util.Assert;

public class ResourceModel
    extends ServletModel
{

    private String m_name;

    public ResourceModel( final HttpContext httpContext,
                          final Servlet servlet,
                          final String alias,
                          final String name,
                          final ClassLoader classLoader )
    {
        super( httpContext, servlet, alias, null, classLoader );
        Assert.notNull( "name == null", name );
        if( name.endsWith( "/" ) )
        {
            throw new IllegalArgumentException( "name ends with slash (/)" );
        }
        m_name = name;
    }

    public String getName()
    {
        return m_name;
    }

}