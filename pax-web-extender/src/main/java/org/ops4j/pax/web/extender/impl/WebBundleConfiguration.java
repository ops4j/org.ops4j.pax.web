package org.ops4j.pax.web.extender.impl;

import java.util.Dictionary;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

@Component(
    immediate = true,
    configurationPid = "org.ops4j.pax.web.deployment", 
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    service = WebBundleConfiguration.class)
public class WebBundleConfiguration {
    
    private String contextPath;
    
    private String virtualHost;
    
    private String symbolicName;
    
    private Long bundleId;
    
    @Activate
    public void activate(ComponentContext cc) {
        Dictionary<String, Object> props = cc.getProperties();
        this.contextPath = (String) props.get("context.path");
        this.virtualHost = (String) props.get("virtual.host");
        this.symbolicName = (String) props.get("bundle.symbolicName");
        this.bundleId = (Long) props.get("bundle.id");
    }

    
    /**
     * @return the contextPath
     */
    public String getContextPath() {
        return contextPath;
    }

    
    /**
     * @return the virtualHost
     */
    public String getVirtualHost() {
        return virtualHost;
    }

    
    /**
     * @return the symbolicName
     */
    public String getSymbolicName() {
        return symbolicName;
    }

    
    /**
     * @return the bundleId
     */
    public Long getBundleId() {
        return bundleId;
    }
}
