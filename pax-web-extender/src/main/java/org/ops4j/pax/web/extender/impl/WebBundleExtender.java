package org.ops4j.pax.web.extender.impl;

import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContainerInitializer;

import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServletContainerInitializer;
import org.ops4j.pax.web.extender.war.internal.parser.WebAppParser;
import org.ops4j.pax.web.service.ServletContainer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings("deprecation")
@Component(name = "WebBundleExtender", immediate = true, service = {})
public class WebBundleExtender implements BundleTrackerCustomizer<WabContext> {

    private static Logger log = LoggerFactory.getLogger(WebBundleExtender.class);
    
    private BundleTracker<WabContext> bundleWatcher;
    private Map<Long, WabContext> wabContextMap = new HashMap<>();

    private BundleContext context;

    private ServletContainer servletContainer;

    private PackageAdmin packageAdmin;
    
    public void activate(BundleContext ctx) {
        this.context = ctx;

        log.info("starting WAB extender {}", context.getBundle().getSymbolicName());
        this.bundleWatcher = new BundleTracker<WabContext>(context, Bundle.ACTIVE, this);
        bundleWatcher.open();
    }

    public void deactivate(BundleContext ctx) {
        log.info("stopping WAB extender {}", context.getBundle().getSymbolicName());
        bundleWatcher.close();
    }

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

            Enumeration<URL> entries = bundle.findEntries("WEB-INF", "web.xml", false);
            if (entries != null && entries.hasMoreElements()) {
                log.debug("found web.xml in {}", bundle);
                WebApp webApp = new WebApp();
                WebAppParser parser = new WebAppParser(packageAdmin);
                try {
                    parser.parse(bundle, webApp);
                    webApp.setBundle(bundle);
                    webApp.setContextName(contextPath);
                    webApp.setRootPath(contextPath);
                    wabContext.setWebApp(webApp);
                }
                catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (canDeploy(wabContext)) {
                deploy(wabContext);
            }
            
            
        }
        return wabContext;
    }

    private void deploy(WabContext wabContext) {
        WebApp webApp = wabContext.getWebApp();
        if (wabContext.isBeanBundle()) {
            WebAppServletContainerInitializer wsci = new WebAppServletContainerInitializer();
            wsci.setServletContainerInitializer(wabContext.getBeanBundleInitializer());
            webApp.addServletContainerInitializer(wsci);
        }
        servletContainer.deploy(webApp);
    }

    private boolean canDeploy(WabContext wabContext) {
        if (servletContainer == null) {
            return false;
        }
        if (wabContext.isBeanBundle()) {
            return wabContext.getBeanBundleInitializer() != null;
        }
        else {
            return true;            
        }
    }

    public synchronized void modifiedBundle(Bundle bundle, BundleEvent event, WabContext object) {
        // TODO Auto-generated method stub
        
    }

    public synchronized void removedBundle(Bundle bundle, BundleEvent event, WabContext object) {
        WabContext wabContext = wabContextMap.remove(bundle.getBundleId());
        if (wabContext == null) {
            return;
        }
        servletContainer.undeploy(wabContext.getWebApp());
    }
    
    @Reference
    public synchronized void setServletContainer(ServletContainer servletContainer) {
        this.servletContainer = servletContainer;
    }

    public synchronized void unsetServletContainer(ServletContainer servletContainer) {
        this.servletContainer = null;
    }
    
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public synchronized void addServletContainerInitializer(ServletContainerInitializer sci, Map<String, Object> props) {
        Long bundleId = (Long) props.get(Bundles.CDI_BUNDLE_ID);
        if (bundleId != null) {
            WabContext wabContext = wabContextMap.get(bundleId);
            if (wabContext == null) {
                wabContext = new WabContext(context.getBundle(bundleId));
                wabContext.setBeanBundle(true);
            }
            wabContext.setBeanBundleInitializer(sci);
            if (canDeploy(wabContext)) {
                deploy(wabContext);
            }
        }        
    }

    public synchronized void removeServletContainerInitializer(ServletContainerInitializer sci, Map<String, Object> props) {
        
    }
    
    @Reference
    public void setPackageAdmin(PackageAdmin packageAdmin) {
        this.packageAdmin = packageAdmin;
    }
    
}
