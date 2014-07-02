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
