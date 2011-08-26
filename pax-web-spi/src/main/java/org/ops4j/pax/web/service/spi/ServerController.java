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
package org.ops4j.pax.web.service.spi;

import javax.servlet.Servlet;

import org.ops4j.pax.web.service.spi.model.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.FilterModel;
import org.ops4j.pax.web.service.spi.model.SecurityConstraintMappingModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.osgi.service.http.HttpContext;

public interface ServerController
{

    void start();

    void stop();

    boolean isStarted();

    boolean isConfigured();

    void configure( Configuration configuration );

    Configuration getConfiguration();

    void addListener( ServerListener listener );
    
    void removeListener( ServerListener listener );

    void removeContext( HttpContext httpContext );

    void addServlet( ServletModel model );

    void removeServlet( ServletModel model );

    void addEventListener( EventListenerModel eventListenerModel );

    void removeEventListener( EventListenerModel eventListenerModel );

    void addFilter( FilterModel filterModel );

    void removeFilter( FilterModel filterModel );

    void addErrorPage( ErrorPageModel model );

    void removeErrorPage( ErrorPageModel model );

    
    Integer getHttpPort();

    Integer getHttpSecurePort();

    Servlet createResourceServlet( ContextModel contextModel, String alias, String name );

	void addSecurityConstraintMapping(SecurityConstraintMappingModel secMapModel);

	void addContainerInitializerModel(ContainerInitializerModel model);

}
