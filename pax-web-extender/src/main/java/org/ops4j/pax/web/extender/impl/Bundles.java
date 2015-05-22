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
