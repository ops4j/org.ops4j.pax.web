/*
 * Copyright 2012 Romain Gilles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.ops4j.pax.web.service.tomcat.internal;

import static org.ops4j.pax.web.service.tomcat.internal.ServerState.States.ACTIVE;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.Servlet;

import org.ops4j.pax.web.service.spi.Configuration;
import org.ops4j.pax.web.service.spi.LifeCycle;
import org.ops4j.pax.web.service.spi.model.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.FilterModel;
import org.ops4j.pax.web.service.spi.model.SecurityConstraintMappingModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.osgi.service.http.HttpContext;

/**
 * @author Romaim Gilles
 */
class ActiveServerState extends AbstractServerState implements ServerState
{


    private final ServerState m_initializedState;
    private final ServerWrapper m_serverWrapper;

    ActiveServerState(ServerStateFactory serverStateFactory, ServerState initializedState, ServerWrapper serverWrapper)
    {
        super( serverStateFactory );
        this.m_initializedState = initializedState;
        this.m_serverWrapper = serverWrapper;
    }

    @Override
    public Servlet createResourceServlet(ContextModel contextModel, String alias, String name)
    {
        return m_serverWrapper.createResourceServlet( contextModel, alias, name );
    }

    @Override
    public void addSecurityConstraintMapping(SecurityConstraintMappingModel secMapModel)
    {
        m_serverWrapper.addSecurityConstraintMapping( secMapModel );
    }

    @Override
    public void addContainerInitializerModel(ContainerInitializerModel model)
    {
        super.addContainerInitializerModel( model );    //To change body of overridden methods use File | Settings | File Templates.
    }

    static ServerState getInstance(ServerStateFactory serverStateFactory, ServerState initializedState, ServerWrapper server)
    {
        return new ActiveServerState( serverStateFactory, initializedState, server );
    }

    @Override
    public ServerState start()
    {
        return throwIllegalState();
    }

    @Override
    public ServerState stop()
    {
        m_serverWrapper.stop();
        return m_initializedState;
    }

    @Override
    public boolean isStarted()
    {
        return true;
    }

    @Override
    public boolean isConfigured()
    {
        return true;
    }

    @Override
    public ServerState configure(Configuration configuration)
    {
        return stop().configure( configuration ).start();
    }

    @Override
    public States getState()
    {
        return ACTIVE;
    }

    @Override
    public Configuration getConfiguration()
    {
        return m_initializedState.getConfiguration();
    }

    @Override
    public void addServlet(ServletModel model)
    {
        m_serverWrapper.addServlet( model );
    }

    @Override
    public void removeServlet(ServletModel model)
    {
        m_serverWrapper.removeServlet( model );
    }

    @Override
    public void removeContext(HttpContext httpContext)
    {
        m_serverWrapper.removeContext( httpContext );
    }

    @Override
    public void addErrorPage(ErrorPageModel model)
    {
        m_serverWrapper.addErrorPage( model );
    }

    @Override
    public void removeErrorPage(ErrorPageModel model)
    {
        m_serverWrapper.removeErrorPage( model );
    }

    @Override
    public void addFilter(FilterModel filterModel)
    {
        m_serverWrapper.addFilter( filterModel );
    }

    @Override
    public void removeFilter(FilterModel filterModel)
    {
        m_serverWrapper.removeFilter( filterModel );
    }

    @Override
    public void addEventListener(EventListenerModel eventListenerModel)
    {
        m_serverWrapper.addEventListener( eventListenerModel );
    }

    @Override
    public void removeEventListener(EventListenerModel eventListenerModel)
    {
        m_serverWrapper.removeEventListener( eventListenerModel );
    }

    @Override
    Collection<String> getSupportedOperations()
    {
        //TODO

        Collection<String> result = new ArrayList<String>();
        result.add("#*(...)");
        return result;
    }
    
    @Override
    public Integer getHttpPort() {
    	return m_initializedState.getHttpPort();
    }
    
    @Override
    public Integer getHttpSecurePort() {
    	return m_initializedState.getHttpSecurePort();
    }

    @Override
    public LifeCycle getContext(ContextModel model) {
        return m_serverWrapper.getContext( model );
    }
}
