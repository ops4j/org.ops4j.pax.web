package org.ops4j.pax.web.extender.impl;

import java.util.Dictionary;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;


public class Bundles {

    public static final String CDI_EXTENDER = "pax.cdi";
    
    public static final String CDI_BUNDLE_ID = "org.ops4j.pax.cdi.bundle.id";    
    public static final String EXTENDER_CAPABILITY = "osgi.extender";

    public static boolean isWebBundle(Bundle candidate) {
        Dictionary<String, String> headers = candidate.getHeaders();
        String contextPath = headers.get("Web-ContextPath");
        return contextPath != null;
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
