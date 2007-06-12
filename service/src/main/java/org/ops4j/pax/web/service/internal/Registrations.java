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

import java.util.Dictionary;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

public interface Registrations
{
    Registration[] get();
    Registration registerServlet( String alias, Servlet servlet, Dictionary initParams, HttpContext context )
        throws NamespaceException, ServletException;
    Registration registerResources( String alias, String name, HttpContext context )
        throws NamespaceException;
    void unregister( Registration registration );
    Registration getByAlias( String alias );
    boolean containsServlet( Servlet servlet );
}
