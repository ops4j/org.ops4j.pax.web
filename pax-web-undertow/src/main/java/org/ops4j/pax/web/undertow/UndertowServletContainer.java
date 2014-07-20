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

import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.api.SecurityConstraint;
import io.undertow.servlet.api.SecurityInfo.EmptyRoleSemantic;
import io.undertow.servlet.api.ServletContainerInitializerInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.api.WebResourceCollection;
import io.undertow.servlet.util.DefaultClassIntrospector;
import io.undertow.servlet.util.ImmediateInstanceFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
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

import org.ops.pax.web.spi.ServletContainer;
import org.ops.pax.web.spi.ServletContainerInitializerModel;
import org.ops.pax.web.spi.WabModel;
import org.ops.pax.web.spi.WebAppModel;
import org.ops4j.pax.web.descriptor.gen.AuthMethodType;
import org.ops4j.pax.web.descriptor.gen.FilterMappingType;
import org.ops4j.pax.web.descriptor.gen.FilterType;
import org.ops4j.pax.web.descriptor.gen.FormLoginConfigType;
import org.ops4j.pax.web.descriptor.gen.ListenerType;
import org.ops4j.pax.web.descriptor.gen.LoginConfigType;
import org.ops4j.pax.web.descriptor.gen.ParamValueType;
import org.ops4j.pax.web.descriptor.gen.RoleNameType;
import org.ops4j.pax.web.descriptor.gen.SecurityConstraintType;
import org.ops4j.pax.web.descriptor.gen.ServletMappingType;
import org.ops4j.pax.web.descriptor.gen.ServletNameType;
import org.ops4j.pax.web.descriptor.gen.ServletType;
import org.ops4j.pax.web.descriptor.gen.UrlPatternType;
import org.ops4j.pax.web.descriptor.gen.WarPathType;
import org.ops4j.pax.web.descriptor.gen.WebResourceCollectionType;
import org.ops4j.pax.web.descriptor.gen.WelcomeFileListType;
import org.ops4j.pax.web.undertow.security.IdentityManagerFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a {@link ServletContainer} based on Undertow.
 * <p>
 * On activation, this component starts a HTTP server listening on all interfaces at the port
 * indicated by the {@code org.osgi.service.http.port} property, defaulting to 8181.
 * <p>
 * Deploys and undeploys web applications processed by the web extender.
 * <p>
 * TODO further configuration properties, access log, more security details
 * 
 * @author Harald Wellmann
 *
 */
@Component
public class UndertowServletContainer implements ServletContainer {

    private static Logger log = LoggerFactory.getLogger(UndertowServletContainer.class);

    private boolean jspPresent;

    private IdentityManagerFactory identityManagerFactory;

    private UndertowHttpServer httpServer;

    @Activate
    public void activate(BundleContext ctx) {
        // Check if Jastow (JSP support) is available and print a warning otherwise.
        // Jastow is an optional dependency.
        try {
            ctx.getBundle().loadClass("io.undertow.jsp.JspServletBuilder");
            jspPresent = true;
        }
        catch (ClassNotFoundException e) {
            log.warn("No runtime support for JSP");
        }
    }

    /**
     * Deploys the given web application bundle. An Undertow DeploymentInfo object is built from the
     * parsed metadata. The deployment is added to the container, and the context path is added to
     * the path handler.
     */
    @Override
    public void deploy(WabModel webApp) {
        Bundle bundle = webApp.getBundle();
        WebAppModel webAppModel = webApp.getWebAppModel();

        ClassLoader cl = webApp.getClassLoader();
        DeploymentInfo deployment = Servlets.deployment().setClassLoader(cl)
            .setContextPath(webApp.getContextPath()).setDeploymentName(webApp.getContextPath());
        deployment.addServletContextAttribute("osgi-bundlecontext", bundle.getBundleContext());
        deployment.addServletContextAttribute("org.ops4j.pax.web.attributes",
            new HashMap<String, Object>());

        // resource manager for static bundle resources
        deployment.setResourceManager(new BundleResourceManager(bundle));

        // For bean bundles, we need a class introspector which performs CDI injection.
        // For other WABs, we simply create instances using the constructor.
        if (webApp.isBeanBundle()) {
            deployment.setClassIntrospecter(new LazyCdiClassIntrospector(deployment));
        }
        else {
            deployment.setClassIntrospecter(DefaultClassIntrospector.INSTANCE);
        }

        addServletContainerInitializers(deployment, webApp);
        addInitParameters(deployment, webAppModel);
        addServlets(deployment, webApp);

        // add the JSP servlet, if Jastow is present
        if (jspPresent) {
            JspServletFactory.addJspServlet(deployment, webApp);
        }
        addFilters(deployment, webApp);
        addListeners(deployment, webApp);
        addWelcomePages(deployment, webApp);
        addLoginConfig(deployment, webApp);
        addSecurityConstraints(deployment, webApp);

        // now deploy the app
        io.undertow.servlet.api.ServletContainer servletContainer = Servlets.defaultContainer();
        DeploymentManager manager = servletContainer.addDeployment(deployment);
        manager.deploy();
        try {
            String virtualHost = null;

            // TODO handle more than one virtual host
            if (!webApp.getVirtualHosts().isEmpty()) {
                virtualHost = webApp.getVirtualHosts().get(0);
            }
            PathHandler pathHandler = httpServer.findPathHandlerForHost(virtualHost);
            if (pathHandler == null) {
                log.error("virtual host [{}] is not defined", virtualHost);
                return;
            }
            pathHandler.addPrefixPath(webApp.getContextPath(), manager.start());
        }
        catch (ServletException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // register ServletContext service
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("osgi.web.contextpath", webApp.getContextPath());
        props.put("osgi.web.symbolicname", bundle.getSymbolicName());
        if (bundle.getVersion() != Version.emptyVersion) {
            props.put("osgi.web.version", bundle.getVersion().toString());
        }
        ServiceRegistration<ServletContext> registration = bundle.getBundleContext()
            .registerService(ServletContext.class, manager.getDeployment().getServletContext(),
                props);
        webApp.setServletContextRegistration(registration);
    }

    private void addWelcomePages(DeploymentInfo deployment, WabModel webApp) {
        WelcomeFileListType welcomeFileList = webApp.getWebAppModel().getWelcomeFileList();
        for (String welcomeFile : welcomeFileList.getWelcomeFile()) {
            deployment.addWelcomePage(welcomeFile);
        }
    }

    private void addServlets(DeploymentInfo deployment, WabModel webApp) {
        for (ServletType webAppServlet : webApp.getWebAppModel().getServlets()) {
            addServlet(webApp, deployment, webAppServlet);
        }
    }

    private void addInitParameters(DeploymentInfo deployment, WebAppModel webApp) {
        for (ParamValueType param : webApp.getContextParams()) {
            deployment.addInitParameter(param.getParamName().getValue(), param.getParamValue()
                .getValue());
        }
    }

    private void addServletContainerInitializers(DeploymentInfo deployment, WabModel webApp) {
        for (ServletContainerInitializerModel wsci : webApp.getServletContainerInitializers()) {
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

    private void addServlet(WabModel webApp, DeploymentInfo deployment, ServletType webAppServlet) {
        String servletName = webAppServlet.getServletName().getValue();
        Class<? extends Servlet> servletClass = loadClass(webApp, webAppServlet.getServletClass()
            .getValue(), Servlet.class);
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
        for (ServletMappingType servletMapping : webApp.getWebAppModel().getServletMappings()) {
            if (servletMapping.getServletName().getValue().equals(servletName)) {
                for (UrlPatternType urlPattern : servletMapping.getUrlPattern()) {
                    servletInfo.addMapping(urlPattern.getValue());
                }
            }
        }
        deployment.addServlet(servletInfo);
    }

    private <T> Class<? extends T> loadClass(WabModel webApp, String className, Class<T> baseClass) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends T> klass = (Class<? extends T>) webApp.getClassLoader().loadClass(
                className);
            return klass;
        }
        catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    private void addFilters(DeploymentInfo deployment, WabModel webApp) {
        WebAppModel webAppModel = webApp.getWebAppModel();
        for (FilterType filter : webAppModel.getFilters()) {
            addFilter(webApp, deployment, filter);
        }
        String filterName = "OSGi Protected Dirs";
        FilterInfo filterInfo = Servlets.filter(filterName, ProtectedDirectoryFilter.class);
        deployment.addFilter(filterInfo);
        Collection<DispatcherType> dispatcherTypes = getDispatcherTypes(null);
        for (DispatcherType dispatcherType : dispatcherTypes) {
            deployment.addFilterUrlMapping(filterName, "/*", dispatcherType);
        }
    }

    private void addFilter(WabModel webApp, DeploymentInfo deployment, FilterType webAppFilter) {
        String filterName = webAppFilter.getFilterName().getValue();
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

        for (FilterMappingType filterMapping : webApp.getWebAppModel().getFilterMappings()) {
            if (filterMapping.getFilterName().getValue().equals(filterName)) {
                Collection<DispatcherType> dispatcherTypes = getDispatcherTypes(filterMapping
                    .getDispatcher());

                for (Object obj : filterMapping.getUrlPatternOrServletName()) {
                    if (obj instanceof ServletNameType) {
                        ServletNameType servletName = (ServletNameType) obj;
                        for (DispatcherType dispatcherType : dispatcherTypes) {
                            deployment.addFilterServletNameMapping(filterName,
                                servletName.getValue(), dispatcherType);
                        }

                    }
                    else if (obj instanceof UrlPatternType) {
                        UrlPatternType urlPattern = (UrlPatternType) obj;
                        for (DispatcherType dispatcherType : dispatcherTypes) {
                            deployment.addFilterUrlMapping(filterName, urlPattern.getValue(),
                                dispatcherType);
                        }
                    }
                }
            }
        }
    }

    private Collection<DispatcherType> getDispatcherTypes(
        List<org.ops4j.pax.web.descriptor.gen.DispatcherType> dispatcherTypes) {
        List<DispatcherType> result = new ArrayList<>();
        if (dispatcherTypes == null || dispatcherTypes.isEmpty()) {
            result.add(DispatcherType.REQUEST);
        }
        else {
            for (org.ops4j.pax.web.descriptor.gen.DispatcherType dispatcherType : dispatcherTypes) {
                result.add(DispatcherType.valueOf(dispatcherType.getValue()));
            }
        }
        return result;
    }

    private void addListeners(DeploymentInfo deployment, WabModel webApp) {
        for (ListenerType webAppListener : webApp.getWebAppModel().getListeners()) {
            ListenerInfo listenerInfo;
            Class<? extends EventListener> listenerClass = loadClass(webApp, webAppListener
                .getListenerClass().getValue(), EventListener.class);
            if (webApp.isBeanBundle()) {
                listenerInfo = Servlets.listener(listenerClass, new LazyCdiInstanceFactory<>(
                    deployment, listenerClass));
            }
            else {
                listenerInfo = Servlets.listener(listenerClass);
            }
            deployment.addListener(listenerInfo);
        }
    }

    private void addLoginConfig(DeploymentInfo deployment, WabModel webApp) {
        LoginConfigType webAppLoginConfig = webApp.getWebAppModel().getLoginConfig();
        if (webAppLoginConfig == null) {
            return;
        }
        String realmName = webAppLoginConfig.getRealmName().getValue();
        FormLoginConfigType formLoginConfig = webAppLoginConfig.getFormLoginConfig();
        String loginPage = null;
        String errorPage = null;
        if (formLoginConfig != null) {
            WarPathType flp = formLoginConfig.getFormLoginPage();
            WarPathType fep = formLoginConfig.getFormErrorPage();
            loginPage = flp == null ? null : flp.getValue();
            errorPage = fep == null ? null : fep.getValue();
        }
        AuthMethodType authMethod = webAppLoginConfig.getAuthMethod();
        LoginConfig loginConfig = new LoginConfig(
            authMethod == null ? null : authMethod.getValue(), realmName, loginPage, errorPage);
        deployment.setLoginConfig(loginConfig);
        deployment.setIdentityManager(identityManagerFactory
            .createIdentityManagerFactory(realmName));
    }

    private void addSecurityConstraints(DeploymentInfo deployment, WabModel webApp) {
        for (SecurityConstraintType constraint : webApp.getWebAppModel().getSecurityConstraints()) {
            addSecurityConstraint(deployment, constraint);
        }
    }

    private void addSecurityConstraint(DeploymentInfo deployment, SecurityConstraintType constraint) {

        SecurityConstraint securityConstraint = new SecurityConstraint();
        List<String> roles = new ArrayList<>();
        for (RoleNameType roleName : constraint.getAuthConstraint().getRoleName()) {
            roles.add(roleName.getValue());
        }
        securityConstraint.addRolesAllowed(roles);
        securityConstraint.setEmptyRoleSemantic(EmptyRoleSemantic.PERMIT);

        for (WebResourceCollectionType wrcSource : constraint.getWebResourceCollection()) {
            WebResourceCollection wrc = new WebResourceCollection();
            for (UrlPatternType urlPattern : wrcSource.getUrlPattern()) {
                wrc.addUrlPattern(urlPattern.getValue());
            }
            securityConstraint.addWebResourceCollection(wrc);
        }
        deployment.addSecurityConstraint(securityConstraint);
    }

    /**
     * Undeploys the given web application bundle. Stops the deployment and removes the context path
     * from the path handler.
     */
    @Override
    public void undeploy(WabModel webApp) {
        ServiceRegistration<ServletContext> registration = webApp.getServletContextRegistration();
        registration.unregister();

        io.undertow.servlet.api.ServletContainer servletContainer = Servlets.defaultContainer();
        DeploymentManager manager = servletContainer.getDeploymentByPath(webApp.getContextPath());
        httpServer.getPathHandler().removePrefixPath(webApp.getContextPath());
        try {
            manager.stop();
        }
        catch (ServletException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        manager.undeploy();
    }

    @Reference
    public void setIdentityManager(IdentityManagerFactory identityManagerFactory) {
        this.identityManagerFactory = identityManagerFactory;
    }

    @Reference
    public void setHttpServer(UndertowHttpServer httpServer) {
        this.httpServer = httpServer;
    }
}
