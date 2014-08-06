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
package org.ops4j.pax.web.extender.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.annotation.HandlesTypes;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;

import org.apache.xbean.finder.BundleAnnotationFinder;
import org.apache.xbean.osgi.bundle.util.DelegatingBundle;
import org.apache.xbean.osgi.bundle.util.equinox.EquinoxBundleClassLoader;
import org.ops.pax.web.spi.ServletContainer;
import org.ops.pax.web.spi.ServletContainerInitializerModel;
import org.ops.pax.web.spi.WabModel;
import org.ops.pax.web.spi.WebAppModel;
import org.ops4j.pax.web.descriptor.gen.FullyQualifiedClassType;
import org.ops4j.pax.web.descriptor.gen.ListenerType;
import org.ops4j.pax.web.descriptor.gen.WebAppType;
import org.ops4j.pax.web.extender.impl.desc.WebDescriptorParser;
import org.ops4j.pax.web.extender.war.internal.parser.WebFilterAnnotationScanner;
import org.ops4j.pax.web.extender.war.internal.parser.WebServletAnnotationScanner;
import org.ops4j.pax.web.utils.ClassPathUtil;
import org.ops4j.pax.web.utils.FelixBundleClassLoader;
import org.ops4j.spi.SafeServiceLoader;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("deprecation")
@Component(immediate = true, service = DeploymentService.class)
public class DeploymentService {

    private static Logger log = LoggerFactory.getLogger(DeploymentService.class);

    private Bundle extender;
    private ServletContainer servletContainer;
    private EventAdmin eventAdmin;

    private PackageAdmin packageAdmin;

    private WebDescriptorParser parser = new WebDescriptorParser();

    /**
     * Deploys the given WAB. Must only be called when all dependencies are satisfied. For bean
     * bundles, the Pax CDI ServletContainerInitializer is added to the web app.
     * 
     * @param wabContext
     *            context of current WAB
     */
    public void deploy(WabContext wabContext) {
        WabModel webApp = wabContext.getWabModel();
        Bundle bundle = wabContext.getBundle();
        String contextPath = wabContext.getConfiguration().getContextPath();
        webApp.setContextPath(contextPath);
        ClassLoader cl = createExtendedClassLoader(bundle);
        webApp.setClassLoader(cl);

        postEvent("org/osgi/service/web/DEPLOYING", bundle, contextPath);
        boolean metadataComplete = false;
        Enumeration<URL> entries = bundle.findEntries("WEB-INF", "web.xml", false);
        if (entries != null && entries.hasMoreElements()) {
            log.debug("found web.xml in {}", bundle);
            WebAppModel webAppModel = parser.createWebAppModel(entries.nextElement());
            webApp.setWebAppModel(webAppModel);

            WebAppType webAppType = webApp.getWebAppModel().getWebApp();
            if (webAppType.isMetadataComplete() != null) {
                metadataComplete = webAppType.isMetadataComplete();
            }
        }
        int majorVersion = getMajorVersion(webApp);
        if (majorVersion >= 3) {
            servletContainerInitializerScan(bundle, webApp);
        }
        else {
            metadataComplete = true;
        }

        if (!metadataComplete) {
            servletAnnotationScan(bundle, webApp.getWebAppModel());
        }

        webApp.getVirtualHosts().add(wabContext.getConfiguration().getVirtualHost());

        if (wabContext.isBeanBundle()) {
            ServletContainerInitializerModel wsci = new ServletContainerInitializerModel();
            wsci.setServletContainerInitializer(wabContext.getBeanBundleInitializer());
            webApp.getServletContainerInitializers().add(wsci);
        }
        log.info("deploying {}", contextPath);
        servletContainer.deploy(webApp);
        wabContext.setDeployed(true);

        postEvent("org/osgi/service/web/DEPLOYED", bundle, contextPath);

    }

    private int getMajorVersion(WabModel wabModel) {
        WebAppType webApp = wabModel.getWebAppModel().getWebApp();
        if (webApp == null) {
            return 3;
        }
        String version = webApp.getVersion();
        String[] parts = version.split("\\.");
        return Integer.parseInt(parts[0]);
    }

    @SuppressWarnings("unchecked")
    private void servletContainerInitializerScan(Bundle bundle, WabModel webApp) {
        log.debug("scanning for ServletContainerInitializers");
        ClassLoader cl = webApp.getClassLoader();
        SafeServiceLoader safeServiceLoader = new SafeServiceLoader(cl);
        List<ServletContainerInitializer> containerInitializers = safeServiceLoader
            .load("javax.servlet.ServletContainerInitializer");

        for (ServletContainerInitializer sci : containerInitializers) {
            ServletContainerInitializerModel sciModel = new ServletContainerInitializerModel();
            sciModel.setServletContainerInitializer(sci);

            try {
                Class<HandlesTypes> loadClass = (Class<HandlesTypes>) cl
                    .loadClass("javax.servlet.annotation.HandlesTypes");
                HandlesTypes handlesTypes = loadClass.cast(sci.getClass()
                    .getAnnotation(loadClass));
                log.debug("Found HandlesTypes {}", handlesTypes);
                if (handlesTypes != null) {
                    // add annotated classes to service
                    Class<?>[] classes = handlesTypes.value();
                    sciModel.setClasses(classes);
                }
            }
            catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            webApp.getServletContainerInitializers().add(sciModel);
        }

    }

    private void servletAnnotationScan(final Bundle bundle, final WebAppModel webApp)  {

        log.debug("metadata-complete is either false or not set");

        log.debug("scanning for annotated classes");
        BundleAnnotationFinder baf = null;
        try {
            baf = new BundleAnnotationFinder(packageAdmin, bundle);
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        Set<Class<?>> webServletClasses = new LinkedHashSet<Class<?>>(
            baf.findAnnotatedClasses(WebServlet.class));
        Set<Class<?>> webFilterClasses = new LinkedHashSet<Class<?>>(
            baf.findAnnotatedClasses(WebFilter.class));
        Set<Class<?>> webListenerClasses = new LinkedHashSet<Class<?>>(
            baf.findAnnotatedClasses(WebListener.class));

        for (Class<?> webServletClass : webServletClasses) {
            log.debug("found WebServlet annotation on class: {}", webServletClass);
            WebServletAnnotationScanner annonScanner = new WebServletAnnotationScanner(bundle,
                webServletClass.getCanonicalName());
            annonScanner.scan(webApp);
        }
        for (Class<?> webFilterClass : webFilterClasses) {
            log.debug("found WebFilter annotation on class: {}", webFilterClass);
            WebFilterAnnotationScanner filterScanner = new WebFilterAnnotationScanner(bundle,
                webFilterClass.getCanonicalName());
            filterScanner.scan(webApp);
        }
        for (Class<?> webListenerClass : webListenerClasses) {
            log.debug("found WebListener annotation on class: {}", webListenerClass);
            addWebListener(webApp, webListenerClass.getCanonicalName());
        }

        log.debug("class scanning done");
    }

    private static void addWebListener(final WebAppModel webApp, String clazz) {
        ListenerType listener = new ListenerType();
        FullyQualifiedClassType klass = new FullyQualifiedClassType();
        klass.setValue(clazz);
        listener.setListenerClass(klass);
        webApp.getListeners().add(listener);
    }

    public void undeploy(WabContext wabContext) {
        WabModel webApp = wabContext.getWabModel();
        String contextPath = webApp.getContextPath();
        Bundle bundle = wabContext.getBundle();
        postEvent("org/osgi/service/web/UNDEPLOYING", bundle, contextPath);
        log.info("undeploying {}", contextPath);
        servletContainer.undeploy(webApp);
        wabContext.setWabModel(null);
        ;
        postEvent("org/osgi/service/web/UNDEPLOYED", bundle, contextPath);
        wabContext.setDeployed(false);

    }

    /**
     * Posts a deployment event with the given properties.
     * 
     * @param topic
     *            event topic
     * @param bundle
     *            current bundle
     * @param contextPath
     *            web context path
     */
    private void postEvent(String topic, Bundle bundle, String contextPath) {
        Map<String, Object> props = buildEventProperties(bundle, contextPath);
        Event event = new Event(topic, props);
        eventAdmin.postEvent(event);
    }

    /**
     * Builds properties for a deployment event.
     * 
     * @param bundle
     *            current bundle
     * @param contextPath
     *            web context path
     * @return property map
     */
    private Map<String, Object> buildEventProperties(Bundle bundle, String contextPath) {
        Map<String, Object> props = new HashMap<>();
        props.put("bundle.symbolicName", bundle.getSymbolicName());
        props.put("bundle.id", bundle.getBundleId());
        props.put("bundle.version", bundle.getVersion());
        props.put("bundle", bundle);
        props.put("context.path", contextPath);
        props.put("timestamp", System.currentTimeMillis());
        props.put("extender.bundle.symbolicName", extender.getSymbolicName());
        props.put("extender.bundle.id", extender.getBundleId());
        props.put("extender.bundle.version", extender.getVersion());
        props.put("extender.bundle", extender);
        return props;
    }

    /**
     * Creates the extended classloader for the current WAB. Since JSF cannot work with bundle: URLs
     * and since OSGi has no standard API for converting these URLs to local URLs, we use
     * framework-specific approaches for Equinox and Felix.
     * 
     * @param bundle
     *            current web bundle
     * @return extended class loader
     */
    private ClassLoader createExtendedClassLoader(Bundle bundle) {
        Set<Bundle> bundleSet = new HashSet<>();
        bundleSet = ClassPathUtil.getBundlesInClassSpace(bundle, bundleSet);
        List<Bundle> bundles = new ArrayList<>();
        bundles.add(bundle);
        bundles.addAll(bundleSet);
        String vendor = extender.getBundleContext().getProperty("org.osgi.framework.vendor");
        ClassLoader cl;
        if ("Eclipse".equals(vendor)) {
            cl = new EquinoxBundleClassLoader(new DelegatingBundle(bundles), true, true);
        }
        // TODO don't assume that "not Equinox" is equivalent to "Felix"
        else {
            cl = new FelixBundleClassLoader(bundles);
        }
        log.debug("extended classloader: {}", cl);
        return cl;
    }

    public Bundle getExtender() {
        return extender;
    }

    public void setExtender(Bundle extender) {
        this.extender = extender;
    }

    @Reference
    public void setServletContainer(ServletContainer servletContainer) {
        this.servletContainer = servletContainer;
    }

    @Reference
    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    @Reference
    public void setPackageAdmin(PackageAdmin packageAdmin) {
        this.packageAdmin = packageAdmin;
    }
}
