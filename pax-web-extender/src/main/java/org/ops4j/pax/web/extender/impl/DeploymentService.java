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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xbean.osgi.bundle.util.DelegatingBundle;
import org.apache.xbean.osgi.bundle.util.equinox.EquinoxBundleClassLoader;
import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServletContainerInitializer;
import org.ops4j.pax.web.extender.war.internal.parser.WebAppParser;
import org.ops4j.pax.web.service.ServletContainer;
import org.ops4j.pax.web.utils.ClassPathUtil;
import org.ops4j.pax.web.utils.FelixBundleClassLoader;
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
    
    

    /**
     * Deploys the given WAB. Must only be called when all dependencies are satisfied. For bean
     * bundles, the Pax CDI ServletContainerInitializer is added to the web app.
     * 
     * @param wabContext
     *            context of current WAB
     */
    public void deploy(WabContext wabContext) {
        WebApp webApp = wabContext.getWebApp();
        Bundle bundle = wabContext.getBundle();
        String contextPath = wabContext.getConfiguration().getContextPath();
        webApp.setContextName(contextPath);

        ClassLoader cl = createExtendedClassLoader(bundle);
        webApp.setClassLoader(cl);

        postEvent("org/osgi/service/web/DEPLOYING", bundle, contextPath);
        Enumeration<URL> entries = bundle.findEntries("WEB-INF", "web.xml", false);
        if (entries != null && entries.hasMoreElements()) {
            log.debug("found web.xml in {}", bundle);
            parseWebXml(bundle, webApp);
        }
        
        
        if (wabContext.isBeanBundle()) {
            WebAppServletContainerInitializer wsci = new WebAppServletContainerInitializer();
            wsci.setServletContainerInitializer(wabContext.getBeanBundleInitializer());
            webApp.addServletContainerInitializer(wsci);
        }
        log.info("deploying {}", contextPath);
        servletContainer.deploy(webApp);
        wabContext.setDeployed(true);

        postEvent("org/osgi/service/web/DEPLOYED", bundle, contextPath);
        
    }

    public void undeploy(WabContext wabContext) {
        WebApp webApp = wabContext.getWebApp();
        String contextPath = webApp.getContextName();
        Bundle bundle = wabContext.getBundle();
        postEvent("org/osgi/service/web/UNDEPLOYING", bundle, contextPath);
        log.info("undeploying {}", contextPath);
        servletContainer.undeploy(webApp);
        wabContext.setWebApp(null);
        postEvent("org/osgi/service/web/UNDEPLOYED", bundle, contextPath);
        wabContext.setDeployed(false);
        
    }
    
    private void parseWebXml(Bundle bundle, WebApp webApp) {
        WebAppParser parser = new WebAppParser(packageAdmin);
        try {
            parser.parse(bundle, webApp);
        }
        catch (Exception exc) {
            log.error("error parsing web.xml", exc);
        }
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
