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
package org.ops4j.pax.web.undertow;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.ServletContainerInitializerInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.DefaultClassIntrospector;
import io.undertow.servlet.util.ImmediateInstanceFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.model.WebAppFilter;
import org.ops4j.pax.web.extender.war.internal.model.WebAppFilterMapping;
import org.ops4j.pax.web.extender.war.internal.model.WebAppInitParam;
import org.ops4j.pax.web.extender.war.internal.model.WebAppListener;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServlet;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServletContainerInitializer;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServletMapping;
import org.ops4j.pax.web.service.ServletContainer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true)
public class UndertowServletContainer implements ServletContainer {

    private String httpPortNumber;

    private Undertow server;

    private PathHandler path;

    @Activate
    public void activate(BundleContext ctx) {
        httpPortNumber = ctx.getProperty("org.osgi.service.http.port");
        if (httpPortNumber == null) {
            httpPortNumber = "8181";
        }
        path = Handlers.path();

        server = Undertow.builder().addHttpListener(Integer.valueOf(httpPortNumber), "localhost")
            .setHandler(path).build();
        server.start();
    }

    public void deactivate(BundleContext ctx) {
        server.stop();
    }

    @Override
    public void deploy(WebApp webApp) {
        Bundle bundle = webApp.getBundle();

        ClassLoader cl = webApp.getClassLoader();
        DeploymentInfo deployment = Servlets.deployment().setClassLoader(cl)
            .setContextPath(webApp.getRootPath()).setDeploymentName(webApp.getContextName());
        deployment.addServletContextAttribute("osgi-bundlecontext", bundle.getBundleContext());
        deployment.addServletContextAttribute("org.ops4j.pax.web.attributes",
            new HashMap<String, Object>());
        deployment.setResourceManager(new BundleResourceManager(bundle));
        if (webApp.isBeanBundle()) {
            deployment.setClassIntrospecter(new LazyCdiClassIntrospector(deployment));
        }
        else {
            deployment.setClassIntrospecter(DefaultClassIntrospector.INSTANCE);
        }

        addServletContainerInitializers(deployment, webApp);
        addInitParameters(deployment, webApp);
        addServlets(deployment, webApp);
        addFilters(deployment, webApp);
        addListeners(deployment, webApp);
        addWelcomePages(deployment, webApp);

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
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("osgi.web.contextpath", webApp.getRootPath());
        props.put("osgi.web.symbolicname", bundle.getSymbolicName());
        if (bundle.getVersion() != Version.emptyVersion) {
            props.put("osgi.web.versuib", bundle.getVersion().toString());
        }
        bundle.getBundleContext().registerService(ServletContext.class,
            manager.getDeployment().getServletContext(), props);
    }

    private void addWelcomePages(DeploymentInfo deployment, WebApp webApp) {
        for (String welcomeFile : webApp.getWelcomeFiles()) {
            deployment.addWelcomePage(welcomeFile);
        }
    }

    private void addServlets(DeploymentInfo deployment, WebApp webApp) {
        for (WebAppServlet webAppServlet : webApp.getSortedWebAppServlet()) {
            addServlet(webApp, deployment, webAppServlet);
        }
    }

    private void addInitParameters(DeploymentInfo deployment, WebApp webApp) {
        for (WebAppInitParam param : webApp.getContextParams()) {
            deployment.addInitParameter(param.getParamName(), param.getParamValue());
        }
    }

    private void addServletContainerInitializers(DeploymentInfo deployment, WebApp webApp) {
        for (WebAppServletContainerInitializer wsci : webApp.getServletContainerInitializers()) {
            Class<? extends ServletContainerInitializer> sciClass = wsci
                .getServletContainerInitializer().getClass();
            InstanceFactory<? extends ServletContainerInitializer> instanceFactory = new ImmediateInstanceFactory<>(
                wsci.getServletContainerInitializer());
            // TODO find classes handled by initializer
            ServletContainerInitializerInfo sciInfo = new ServletContainerInitializerInfo(sciClass,
                instanceFactory, new HashSet<Class<?>>());
            deployment.addServletContainerInitalizer(sciInfo);
        }
    }

    private void addServlet(WebApp webApp, DeploymentInfo deployment, WebAppServlet webAppServlet) {
        String servletName = webAppServlet.getServletName();
        Class<? extends Servlet> servletClass = loadClass(webApp,
            webAppServlet.getServletClassName(), Servlet.class);
        ServletInfo servletInfo;
        if (webApp.isBeanBundle()) {
            try {
                servletInfo = Servlets.servlet(servletName, servletClass, deployment
                    .getClassIntrospecter().createInstanceFactory(servletClass));
            }
            catch (NoSuchMethodException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }
        }
        else {
            servletInfo = Servlets.servlet(servletName, servletClass);
        }
        for (WebAppServletMapping servletMapping : webApp.getServletMappings(servletName)) {
            servletInfo.addMapping(servletMapping.getUrlPattern());
        }
        deployment.addServlet(servletInfo);
    }

    private <T> Class<? extends T> loadClass(WebApp webApp, String className, Class<T> baseClass) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends T> klass = (Class<? extends T>) webApp.getClassLoader().loadClass(className);
            return klass;
        }
        catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    private void addFilters(DeploymentInfo deployment, WebApp webApp) {
        for (String filterName : webApp.getFilters()) {
            addFilter(webApp, deployment, webApp.getFilter(filterName));
        }
    }

    private void addFilter(WebApp webApp, DeploymentInfo deployment, WebAppFilter webAppFilter) {
        String filterName = webAppFilter.getFilterName();
        Class<? extends Filter> filterClass = loadClass(webApp, filterName, Filter.class);
        FilterInfo filterInfo;
        if (webApp.isBeanBundle()) {
            filterInfo = Servlets.filter(filterName, filterClass, new LazyCdiInstanceFactory<>(
                deployment, filterClass));
        }
        else {
            filterInfo = Servlets.filter(filterName, filterClass);
        }
        deployment.addFilter(filterInfo);

        for (WebAppFilterMapping filterMapping : webApp.getFilterMappings(filterName)) {
            Collection<DispatcherType> dispatcherTypes = getDispatcherTypes(filterMapping
                .getDispatcherTypes());
            if (filterMapping.getServletName() != null) {
                for (DispatcherType dispatcherType : dispatcherTypes) {
                    deployment.addFilterServletNameMapping(filterName,
                        filterMapping.getServletName(), dispatcherType);
                }
            }
            else if (filterMapping.getUrlPattern() != null) {
                for (DispatcherType dispatcherType : dispatcherTypes) {
                    deployment.addFilterUrlMapping(filterName, filterMapping.getUrlPattern(),
                        dispatcherType);
                }
            }
        }
    }

    private Collection<DispatcherType> getDispatcherTypes(EnumSet<DispatcherType> dispatcherTypes) {
        List<DispatcherType> result = new ArrayList<>();
        if (dispatcherTypes == null || dispatcherTypes.isEmpty()) {
            result.add(DispatcherType.REQUEST);
        }
        else {
            for (DispatcherType dispatcherType : dispatcherTypes) {
                result.add(dispatcherType);
            }
        }
        return result;
    }

    private void addListeners(DeploymentInfo deployment, WebApp webApp) {
        for (WebAppListener webAppListener : webApp.getListeners()) {
            EventListener listener = webAppListener.getListener();
            ListenerInfo listenerInfo;
            if (listener != null) {
                listenerInfo = Servlets.listener(listener.getClass(),
                    new ImmediateInstanceFactory<EventListener>(listener));
            }
            else {
                Class<? extends EventListener> listenerClass = loadClass(webApp,
                    webAppListener.getListenerClass(), EventListener.class);
                if (webApp.isBeanBundle()) {
                    listenerInfo = Servlets.listener(listenerClass, new LazyCdiInstanceFactory<>(
                        deployment, listenerClass));
                }
                else {
                    listenerInfo = Servlets.listener(listenerClass);
                }
            }
            deployment.addListener(listenerInfo);
        }
    }

    @Override
    public void undeploy(WebApp webApp) {
        io.undertow.servlet.api.ServletContainer servletContainer = Servlets.defaultContainer();
        DeploymentManager manager = servletContainer.getDeploymentByPath(webApp.getRootPath());
        path.removePrefixPath(webApp.getRootPath());
        try {
            manager.stop();
        }
        catch (ServletException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        manager.undeploy();
    }
}
