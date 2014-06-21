package org.ops4j.pax.web.undertow;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.ServletContainerInitializerInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;

import java.util.Arrays;
import java.util.HashSet;

import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.model.WebAppInitParam;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServlet;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServletContainerInitializer;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServletMapping;
import org.ops4j.pax.web.service.ServletContainer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;


@Component(name = "UndertowServletContainer", immediate = true, service = ServletContainer.class)
public class UndertowServletContainer implements ServletContainer {
    
    private String httpPortNumber = "8080";
    
    private BundleContext context;

    private Undertow server;

    private PathHandler path;

    @Activate
    public void activate(BundleContext ctx) {
        this.context = ctx;
        path = Handlers.path();

        server = Undertow.builder()
            .addHttpListener(Integer.valueOf(httpPortNumber), "localhost").setHandler(path).build();
        server.start();


    }

    public void deactivate(BundleContext ctx) {
        server.stop();


    }

    

    @Override
    public void deploy(WebApp webApp) {
        Bundle bundle = webApp.getBundle();
        //ClassLoader cl = bundle.adapt(BundleWiring.class).getClassLoader();
        
        ClassLoader cl = webApp.getClassLoader();
        DeploymentInfo deployment = Servlets.deployment().setClassLoader(cl)
            .setContextPath(webApp.getRootPath()).setDeploymentName(webApp.getContextName());
        deployment.addServletContextAttribute("osgi-bundlecontext", bundle.getBundleContext());
        deployment.setResourceManager(new BundleResourceManager(bundle));
        
        for (WebAppServletContainerInitializer wsci : webApp.getServletContainerInitializers()) {
            Class<? extends ServletContainerInitializer> sciClass = wsci.getServletContainerInitializer().getClass();
            InstanceFactory<? extends ServletContainerInitializer> instanceFactory = new ImmediateInstanceFactory<>(wsci.getServletContainerInitializer());
            ServletContainerInitializerInfo sciInfo = new ServletContainerInitializerInfo(sciClass, instanceFactory, new HashSet<Class<?>>());
            deployment.addServletContainerInitalizer(sciInfo);
        }
        
        
        for (WebAppInitParam param : webApp.getContextParams()) {
            deployment.addInitParameter(param.getParamName(), param.getParamValue());
        }
        for (WebAppServlet webAppServlet : webApp.getSortedWebAppServlet()) {
            String servletName = webAppServlet.getServletName();
            try {
                Class<? extends Servlet> servletClass = (Class<? extends Servlet>) cl.loadClass(webAppServlet.getServletClassName());
                ServletInfo servletInfo = Servlets.servlet(servletName, servletClass);
                for (WebAppServletMapping servletMapping: webApp.getServletMappings(servletName)) {
                    servletInfo.addMapping(servletMapping.getUrlPattern());
                }
                deployment.addServlet(servletInfo);
            }
            catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        
        for (String welcomeFile : webApp.getWelcomeFiles()) {
            deployment.addWelcomePage(welcomeFile);
        }
        
        io.undertow.servlet.api.ServletContainer servletContainer = Servlets.defaultContainer();
        DeploymentManager manager = servletContainer.addDeployment(deployment);
        manager.deploy();
        try {
            path.addPrefixPath(webApp.getRootPath(), manager.start());
        }
        catch (ServletException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        bundle.getBundleContext().registerService(ServletContext.class, manager.getDeployment().getServletContext(), null);
    }

    @Override
    public void undeploy(WebApp webApp) {
    }

}
