/*
 * Copyright 2014 Harald Wellmann.
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
package org.ops4j.pax.web.undertow.service;

import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.ops4j.pax.web.undertow.UndertowHttpServer;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/**
 * Implements {@link HttpService} using an {@link UndertowHttpServer}. Note that this service
 * component uses a service factory to create a new instance of this service per using bundle.
 * <p>
 * This is required to create a default {@link HttpContext} rendering resources from the using
 * bundle (as opposed to the owning bundle).
 * 
 * @author Harald Wellmann
 *
 */
@Component(servicefactory = true, service = HttpService.class)
public class UndertowHttpService implements HttpService {

    /**
     * Bundle using this service.
     */
    private Bundle bundle;

    private UndertowHttpServer httpServer;
    private Map<HttpContext, DeploymentManager> contextMap = new HashMap<>();
    private Map<String, AliasWrapper> aliasMap = new HashMap<>();

    @Activate
    public void activate(ComponentContext cc) {
        this.bundle = cc.getUsingBundle();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public synchronized void registerServlet(String alias, Servlet servlet, Dictionary initparams,
        HttpContext context) throws ServletException, NamespaceException {

        if (aliasMap.get(alias) != null) {
            throw new NamespaceException("alias already registered: " + alias);
        }

        HttpContext currentContext = context;
        if (currentContext == null) {
            currentContext = new DefaultHttpContext(bundle);
        }
        DeploymentManager manager = contextMap.get(currentContext);
        DeploymentInfo deploymentInfo;
        if (manager != null) {
            Deployment deployment = manager.getDeployment();
            deploymentInfo = deployment.getDeploymentInfo();
            manager.stop();
            manager.undeploy();
        }

        ServletInfo servletInfo = createServletInfo(alias, servlet, initparams);
        AliasWrapper aliasWrapper = new AliasWrapper(alias);
        aliasWrapper.setServletInfo(servletInfo);
        aliasWrapper.setHttpContext(currentContext);
        aliasMap.put(alias, aliasWrapper);
        
        deploymentInfo = buildServletDeployment(currentContext);
        manager = deploy(deploymentInfo);
        contextMap.put(currentContext, manager);                    
    }

    private DeploymentManager deploy(DeploymentInfo deploymentInfo) throws ServletException {
        io.undertow.servlet.api.ServletContainer servletContainer = Servlets
            .defaultContainer();
        DeploymentManager manager = servletContainer.addDeployment(deploymentInfo);
        manager.deploy();
        httpServer.getPathHandler().addPrefixPath("/", manager.start());
        return manager;
    }

    private ServletInfo createServletInfo(String alias, Servlet servlet,
        Dictionary<String, String> initparams) {
        ServletInfo servletInfo = Servlets.servlet(alias, servlet.getClass(),
            new ImmediateInstanceFactory<Servlet>(servlet));
        servletInfo.addMapping(alias + "/*");
        if (initparams != null) {
            Enumeration<String> keys = initparams.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                servletInfo.addInitParam(key, (String) initparams.get(key));
            }
        }
        return servletInfo;
    }

    @Override
    public synchronized void registerResources(String alias, String name, HttpContext context)
        throws NamespaceException {
        if (aliasMap.get(alias) != null) {
            throw new NamespaceException("alias already registered: " + alias);
        }

        HttpContext currentContext = context;
        if (currentContext == null) {
            currentContext = new DefaultHttpContext(bundle);
        }
        AliasWrapper aliasWrapper = new AliasWrapper(alias);
        aliasWrapper.setHttpContext(currentContext);
        aliasMap.put(alias, aliasWrapper);

        ResourceManager resourceManager = new HttpContextResourceManager(name, currentContext);
        ResourceHandler handler = new ResourceHandler(resourceManager);

        httpServer.getPathHandler().addPrefixPath(alias, handler);
    }

    @Override
    public synchronized void unregister(String alias) {
        AliasWrapper aliasWrapper = aliasMap.remove(alias);
        if (aliasWrapper == null) {
            throw new IllegalArgumentException("alias not registered: " + alias);
        }
        if (aliasWrapper.getServletInfo() == null) {
            httpServer.getPathHandler().removePrefixPath(alias);
        }
        else {
            HttpContext httpContext = aliasWrapper.getHttpContext();
            DeploymentManager manager = contextMap.get(httpContext);
            if (manager == null) {
                return;
            }
            DeploymentInfo deploymentInfo = manager.getDeployment().getDeploymentInfo();
            boolean redeploy = (deploymentInfo.getServlets().size() > 1);
            try {
                manager.stop();
                manager.undeploy();
                if (redeploy) {
                    deploymentInfo = buildServletDeployment(httpContext);
                    manager = deploy(deploymentInfo);
                    contextMap.put(httpContext, manager);                    
                }
            }
            catch (ServletException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    private DeploymentInfo buildServletDeployment(HttpContext httpContext) {
        DeploymentInfo deploymentInfo = Servlets.deployment().setClassLoader(getClass().getClassLoader())
            .setContextPath("").setDeploymentName("ROOT");
        for (AliasWrapper wrapper : aliasMap.values()) {
            ServletInfo servletInfo = wrapper.getServletInfo();
            if (servletInfo != null && httpContext == wrapper.getHttpContext()) {
                deploymentInfo.addServlet(servletInfo);
            }
        }
        return deploymentInfo;
    }

    @Override
    public HttpContext createDefaultHttpContext() {
        return new DefaultHttpContext(bundle);
    }

    @Reference
    public void setHttpServer(UndertowHttpServer httpServer) {
        this.httpServer = httpServer;
    }

}
