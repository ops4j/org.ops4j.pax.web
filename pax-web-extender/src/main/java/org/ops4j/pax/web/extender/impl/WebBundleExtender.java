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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContainerInitializer;

import org.apache.xbean.osgi.bundle.util.DelegatingBundle;
import org.apache.xbean.osgi.bundle.util.equinox.EquinoxBundleClassLoader;
import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServletContainerInitializer;
import org.ops4j.pax.web.extender.war.internal.parser.WebAppParser;
import org.ops4j.pax.web.service.ServletContainer;
import org.ops4j.pax.web.utils.ClassPathUtil;
import org.ops4j.pax.web.utils.FelixBundleClassLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extender which manages the web bundle lifecycle. This extender watches for bundles getting
 * started and stopped. If the bundle is a web application bundle (WAB) as indicated by the presence
 * of the {@code Web-ContextPath} manifest header, the extender deploys the bundle to the servlet
 * container, provided that all required dependencies are satisfied. It is assumed that the system
 * contains exactly one servlet container.
 * <p>
 * For bean bundles (i.e. WABs with CDI beans managed by Pax CDI), this extender collaborates with
 * the Pax CDI extender and waits for a {@code ServletContainerInitializer} service to be registered
 * by Pax CDI. There are no compile-time dependencies between the two extenders.
 * <p>
 * The extender builds an extended classloader for each web bundle which is passed to the servlet
 * container and must be used as context class loader for the web application. The bundle
 * classloader is <em>not</em> sufficient for this purpose. The servlet container needs to load
 * ServletContainerInitializers from META-INF/services resources and the corresponding classes. Any
 * such extensions (e.g. JSF) may need to load further META-INF resources (like tag library
 * descriptors) from other bundles.
 * <p>
 * For this reason, the extended classloader permits loading META-INF resources from any bundle
 * wired to the given WAB, delivering standard URLs (as opposed to bundle: or bundleresource: URLs).
 * 
 * @author Harald Wellmann
 *
 */
@SuppressWarnings("deprecation")
@Component(immediate = true, service = {})
public class WebBundleExtender implements BundleTrackerCustomizer<WabContext> {

    private static Logger log = LoggerFactory.getLogger(WebBundleExtender.class);

    private BundleTracker<WabContext> bundleWatcher;

    /**
     * Maps bundle IDs to wab contexts. Contains an entry for each WAB.
     */
    private Map<Long, WabContext> wabContextMap = new HashMap<>();

    private Map<String, WebBundleConfiguration> configMap = new ConcurrentHashMap<>();

    /**
     * Context of extender bundle.
     */
    private BundleContext context;

    /**
     * Servlet container service.
     */
    private ServletContainer servletContainer;

    /**
     * Package admin service, required web.xml parser. TODO remove this deprecated dependency and
     * use BundleWiring.
     */
    private PackageAdmin packageAdmin;

    /**
     * Event admin service, required for posting deployment events.
     */
    private EventAdmin eventAdmin;

    private ConfigurationAdmin configAdmin;

    /**
     * This bundle.
     */
    private Bundle extender;


    /**
     * Activates the extender and starts tracking bundles.
     * 
     * @param ctx
     *            bundle context
     */
    @Activate
    public void activate(BundleContext ctx) {
        this.context = ctx;
        extender = context.getBundle();

        log.info("starting WAB extender {}", context.getBundle().getSymbolicName());
        this.bundleWatcher = new BundleTracker<WabContext>(context, Bundle.ACTIVE, this);
        bundleWatcher.open();
    }

    /**
     * Deactivates the extender and stops tracking bundles.
     * <p>
     * TODO undeploy all deployed web apps.
     * 
     * @param ctx
     *            bundle context
     */
    @Deactivate
    public void deactivate(BundleContext ctx) {
        log.info("stopping WAB extender {}", context.getBundle().getSymbolicName());
        bundleWatcher.close();
    }

    /**
     * Event handler for bundle start. Does nothing when the bundle is not a WAB. Parses web.xml if
     * present and builds a metadata model which is passed to the servlet container.
     * <p>
     * Posts DEPLOYING and DEPLOYED events.
     * <p>
     * TODO Post FAILED event on failure. Handle conflicting context paths.
     */
    public synchronized WabContext addingBundle(Bundle bundle, BundleEvent event) {
        Dictionary<String, String> headers = bundle.getHeaders();
        String contextPath = headers.get("Web-ContextPath");
        WabContext wabContext = null;
        if (contextPath != null) {

            wabContext = wabContextMap.get(bundle.getBundleId());
            boolean beanBundle = Bundles.isBeanBundle(bundle);
            if (wabContext == null) {
                wabContext = new WabContext(bundle);
                wabContext.setBeanBundle(beanBundle);
                wabContextMap.put(bundle.getBundleId(), wabContext);
            }

            ClassLoader cl = createExtendedClassLoader(bundle);
            WebApp webApp = new WebApp();
            webApp.setClassLoader(cl);
            wabContext.setWebApp(webApp);
            webApp.setBundle(bundle);
            webApp.setBeanBundle(beanBundle);
            webApp.setContextName(contextPath);

            findConfiguration(wabContext);
            if (canDeploy(wabContext)) {
                postEvent("org/osgi/service/web/DEPLOYING", bundle, contextPath);
                Enumeration<URL> entries = bundle.findEntries("WEB-INF", "web.xml", false);
                if (entries != null && entries.hasMoreElements()) {
                    log.debug("found web.xml in {}", bundle);
                    parseWebXml(bundle, webApp);
                }
                deploy(wabContext);
                postEvent("org/osgi/service/web/DEPLOYED", bundle, contextPath);
            }

        }
        return wabContext;
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

    private WebBundleConfiguration findConfiguration(WabContext wabContext) {
        Bundle bundle = wabContext.getBundle();
        WebBundleConfiguration deployer = configMap.get(bundle.getSymbolicName());
        if (deployer == null) {
            try {
                log.info("creating new configuration for {}", bundle.getSymbolicName());
                Configuration config = configAdmin.createFactoryConfiguration("org.ops4j.pax.web.deployment");
                Dictionary<String,Object> props = new Hashtable<>();
                props.put("symbolicName", bundle.getSymbolicName());
                props.put("contextPath", wabContext.getWebApp().getContextName());
                props.put("bundle.id", bundle.getBundleId());
                config.update(props);
            }
            catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else {
            log.info("found configuration for {}", bundle.getSymbolicName());
            wabContext.setConfiguration(deployer);
        }

        return null;
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
        String vendor = context.getProperty("org.osgi.framework.vendor");
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
     * Deploys the given WAB. Must only be called when all dependencies are satisfied. For bean
     * bundles, the Pax CDI ServletContainerInitializer is added to the web app.
     * 
     * @param wabContext
     *            context of current WAB
     */
    private void deploy(WabContext wabContext) {
        WebApp webApp = wabContext.getWebApp();
        String contextPath = wabContext.getConfiguration().getContextPath();
        webApp.setContextName(contextPath);
        if (wabContext.isBeanBundle()) {
            WebAppServletContainerInitializer wsci = new WebAppServletContainerInitializer();
            wsci.setServletContainerInitializer(wabContext.getBeanBundleInitializer());
            webApp.addServletContainerInitializer(wsci);
        }
        log.info("deploying {}", contextPath);
        servletContainer.deploy(webApp);
        wabContext.setDeployed(true);
    }

    /**
     * Checks if the given WAB can be deployed. This requires a servlet container, parsed metadata
     * and (if the bundle is a bean bundle) a ServletContainerInitializer provided by Pax CDI.
     * 
     * @param wabContext
     *            context of current WAB
     * @return true if the WAB can be deployed
     */
    private boolean canDeploy(WabContext wabContext) {
        if (servletContainer == null) {
            log.trace("servlet container is null");
            return false;
        }
        if (wabContext.getWebApp() == null) {
            log.trace("webapp is null");
            return false;
        }
        if (wabContext.getConfiguration() == null) {
            log.trace("configuration is null");
            return false;
        }
        if (wabContext.isBeanBundle()) {
            boolean hasInitializer = wabContext.getBeanBundleInitializer() != null;
            if (!hasInitializer) {
                log.trace("initializer is null");
            }
            return hasInitializer;
        }
        else {
            return true;
        }
    }

    /**
     * TODO
     */
    public synchronized void modifiedBundle(Bundle bundle, BundleEvent event, WabContext object) {
    }

    /**
     * Event handler for bundle stop. If the bundle is a WAB, the web application is undeployed.
     * TODO proper synchronization, the bundle might not be deployed.
     * 
     */
    public synchronized void removedBundle(Bundle bundle, BundleEvent event, WabContext object) {
        WabContext wabContext = wabContextMap.remove(bundle.getBundleId());
        if (wabContext == null) {
            return;
        }

        undeploy(wabContext);
    }

    private void undeploy(WabContext wabContext) {
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

    @Reference
    public synchronized void setServletContainer(ServletContainer servletContainer) {
        this.servletContainer = servletContainer;
    }

    public synchronized void unsetServletContainer(ServletContainer servletContainer) {
        this.servletContainer = null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public synchronized void addServletContainerInitializer(ServletContainerInitializer sci,
        Map<String, Object> props) {
        Long bundleId = (Long) props.get(Bundles.CDI_BUNDLE_ID);
        if (bundleId != null) {
            WabContext wabContext = wabContextMap.get(bundleId);
            if (wabContext == null) {
                wabContext = new WabContext(context.getBundle(bundleId));
                wabContext.setBeanBundle(true);
                wabContextMap.put(bundleId, wabContext);
            }
            wabContext.setBeanBundleInitializer(sci);
            if (canDeploy(wabContext)) {
                deploy(wabContext);
            }
        }
    }

    public synchronized void removeServletContainerInitializer(ServletContainerInitializer sci,
        Map<String, Object> props) {
        Long bundleId = (Long) props.get(Bundles.CDI_BUNDLE_ID);
        if (bundleId != null) {
            WabContext wabContext = wabContextMap.get(bundleId);
            if (wabContext != null) {
                if (wabContext.isDeployed()) {
                    undeploy(wabContext);
                }
                wabContext.setBeanBundleInitializer(null);
            }
        }
    }

    @Reference
    public void setPackageAdmin(PackageAdmin packageAdmin) {
        this.packageAdmin = packageAdmin;
    }

    public void unsetPackageAdmin(PackageAdmin packageAdmin) {
        this.packageAdmin = null;
    }

    @Reference
    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void unsetEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = null;
    }

    @Reference
    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    public void unsetConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public synchronized void addConfiguration(WebBundleConfiguration deployer, Map<String, Object> props) {
        String symbolicName = (String) props.get("symbolicName");
        if (symbolicName != null) {
            configMap.put(symbolicName, deployer);
        }

        Long bundleId = (Long) props.get("bundle.id");
        if (bundleId != null) {
            WabContext wabContext = wabContextMap.get(bundleId);
            wabContext.setConfiguration(deployer);
            if (wabContext != null && canDeploy(wabContext)) {
                deploy(wabContext);
            }
        }        
    }

    public synchronized void removeConfiguration(WebBundleConfiguration deployer, Map<String, Object> props) {
        String symbolicName = (String) props.get("symbolicName");
        if (symbolicName != null) {
            configMap.remove(symbolicName);
        }
    }

}
