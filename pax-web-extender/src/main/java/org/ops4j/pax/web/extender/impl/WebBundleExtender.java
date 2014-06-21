package org.ops4j.pax.web.extender.impl;

import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContainerInitializer;

import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServletContainerInitializer;
import org.ops4j.pax.web.extender.war.internal.parser.WebAppParser;
import org.ops4j.pax.web.service.ServletContainer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component(name = "WebBundleExtender", immediate = true, service = {})
public class WebBundleExtender implements BundleTrackerCustomizer<WabContext> {

    public static final String CDI_EXTENDER = "pax.cdi";
    
    public static final String CDI_BUNDLE_ID = "org.ops4j.pax.cdi.bundle.id";    
    public static final String EXTENDER_CAPABILITY = "osgi.extender";

    private static Logger log = LoggerFactory.getLogger(WebBundleExtender.class);
    
    private BundleTracker<WabContext> bundleWatcher;
    private Map<Long, WabContext> wabContextMap = new HashMap<>();

    private BundleContext context;

    private ServletContainer servletContainer;
    
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

    public WabContext addingBundle(Bundle bundle, BundleEvent event) {
        Dictionary<String, String> headers = bundle.getHeaders();
        String contextPath = headers.get("Web-ContextPath");
        WabContext wabContext = null;
        if (contextPath != null) {
            wabContext = wabContextMap.get(bundle.getBundleId());
            boolean beanBundle = isBeanBundle(bundle);
            if (wabContext == null) {
                wabContext = new WabContext(bundle);
                wabContext.setBeanBundle(beanBundle);
                wabContextMap.put(bundle.getBundleId(), wabContext);
            }

            Enumeration<URL> entries = bundle.findEntries("WEB-INF", "web.xml", false);
            if (entries != null && entries.hasMoreElements()) {
                log.debug("found web.xml in {}", bundle);
                WebApp webApp = new WebApp();
                WebAppParser parser = new WebAppParser(null);
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

    public void modifiedBundle(Bundle bundle, BundleEvent event, WabContext object) {
        // TODO Auto-generated method stub
        
    }

    public void removedBundle(Bundle bundle, BundleEvent event, WabContext object) {
        // TODO Auto-generated method stub
        
    }
    
    @Reference
    public void setServletContainer(ServletContainer servletContainer) {
        this.servletContainer = servletContainer;
    }

    public void unsetServletContainer(ServletContainer servletContainer) {
        this.servletContainer = null;
    }
    
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addServletContainerInitializer(ServletContainerInitializer sci, Map<String, Object> props) {
        Long bundleId = (Long) props.get(CDI_BUNDLE_ID);
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

    public void removeServletContainerInitializer(ServletContainerInitializer sci, Map<String, Object> props) {
        
    }
    
    public static boolean isBeanBundle(Bundle candidate) {
        BundleWiring wiring = candidate.adapt(BundleWiring.class);
        if (wiring == null) {
            return false;
        }
        List<BundleWire> wires = wiring.getRequiredWires(EXTENDER_CAPABILITY);
        for (BundleWire wire : wires) {
            Object object = wire.getCapability().getAttributes().get(EXTENDER_CAPABILITY);
            if (object instanceof String) {
                String extender = (String) object;
                if (extender.equals(CDI_EXTENDER)) {
                    return true;
                }
            }
        }
        return false;
    }
}
