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
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

@Component
public class UndertowHttpService implements HttpService {

    private UndertowHttpServer httpServer;
    private DeploymentInfo deploymentInfo;
    private Map<HttpContext, DeploymentManager> contextMap = new HashMap<>();

    @Activate
    public void activate(BundleContext ctx) {
        deploymentInfo = Servlets.deployment().setClassLoader(getClass().getClassLoader())
            .setContextPath("/").setDeploymentName("ROOT");

        // resource manager for static bundle resources
        // deployment.setResourceManager(new BundleResourceManager(bundle));

    }

    @Deactivate
    public void deactivate(BundleContext ctx) {
    }

    @Override
    public void registerServlet(String alias, Servlet servlet, Dictionary initparams,
        HttpContext context) throws ServletException, NamespaceException {
        
        HttpContext currentContext = context;
        if (currentContext == null) {
            currentContext = new DefaultHttpContext();
        }
        DeploymentManager manager = contextMap.get(currentContext);
        
        if (manager == null) {
            deploymentInfo = Servlets.deployment().setClassLoader(getClass().getClassLoader())
                .setContextPath("/").setDeploymentName("ROOT");
            
            ServletInfo servletInfo = createServletInfo(alias, servlet, initparams);
            deploymentInfo.addServlet(servletInfo);

            // now deploy the app
            io.undertow.servlet.api.ServletContainer servletContainer = Servlets.defaultContainer();
            manager = servletContainer.addDeployment(deploymentInfo);
            contextMap.put(currentContext, manager);
        }
        else {
            Deployment deployment = manager.getDeployment();
            DeploymentInfo deploymentInfo = deployment.getDeploymentInfo();
            manager.stop();
            manager.undeploy();
            ServletInfo servletInfo = createServletInfo(alias, servlet, initparams);
            deploymentInfo.addServlet(servletInfo);
            io.undertow.servlet.api.ServletContainer servletContainer = Servlets.defaultContainer();
            manager = servletContainer.addDeployment(deploymentInfo);
            contextMap.put(currentContext, manager);
        }
        

        // now deploy the app
        manager.deploy();
        try {
            httpServer.getPathHandler().addPrefixPath("/", manager.start());
        }
        catch (ServletException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private ServletInfo createServletInfo(String alias, Servlet servlet, Dictionary<String, String> initparams) {
        ServletInfo servletInfo = Servlets.servlet(alias, servlet.getClass(),
            new ImmediateInstanceFactory<Servlet>(servlet));
        servletInfo.addMapping(alias);
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
    public void registerResources(String alias, String name, HttpContext context)
        throws NamespaceException {
        HttpContext currentContext = context;
        if (currentContext == null) {
            currentContext = new DefaultHttpContext();
        }
        
        ResourceManager resourceManager = new HttpContextResourceManager(name, currentContext);
        ResourceHandler handler = new ResourceHandler(resourceManager);
        

        httpServer.getPathHandler().addPrefixPath(alias, handler);
    }

    @Override
    public void unregister(String alias) {
        // TODO Auto-generated method stub

    }

    @Override
    public HttpContext createDefaultHttpContext() {
        return new DefaultHttpContext();
    }

    @Reference
    public void setHttpServer(UndertowHttpServer httpServer) {
        this.httpServer = httpServer;
    }

}
