package org.ops4j.pax.web.extender.impl;

import javax.servlet.ServletContainerInitializer;

import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.osgi.framework.Bundle;

public class WabContext {

    private Bundle bundle;

    private boolean isBeanBundle;

    private ServletContainerInitializer beanBundleInitializer;
    
    private WebApp webApp;

    public WabContext(Bundle bundle) {
        this.bundle = bundle;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public boolean isBeanBundle() {
        return isBeanBundle;
    }

    public void setBeanBundle(boolean isBeanBundle) {
        this.isBeanBundle = isBeanBundle;
    }

    public ServletContainerInitializer getBeanBundleInitializer() {
        return beanBundleInitializer;
    }

    public void setBeanBundleInitializer(ServletContainerInitializer beanBundleInitializer) {
        this.beanBundleInitializer = beanBundleInitializer;
    }

    
    public WebApp getWebApp() {
        return webApp;
    }

    
    public void setWebApp(WebApp webApp) {
        this.webApp = webApp;
    }
    
    
}
