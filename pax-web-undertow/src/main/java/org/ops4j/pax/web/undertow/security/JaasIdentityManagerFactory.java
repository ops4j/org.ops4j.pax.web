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
package org.ops4j.pax.web.undertow.security;

import io.undertow.security.idm.IdentityManager;

import org.apache.felix.jaas.LoginContextFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class JaasIdentityManagerFactory implements IdentityManagerFactory {
    
    
    
    private LoginContextFactory loginContextFactory;

    @Override
    public IdentityManager createIdentityManagerFactory(String realmName) {
        JaasIdentityManager jaasIdentityManager = new JaasIdentityManager(realmName);
        jaasIdentityManager.setLoginContextFactory(loginContextFactory);
        return jaasIdentityManager;
    }

    @Reference
    public void setLoginContextFactory(LoginContextFactory factory) {
        this.loginContextFactory = factory;
    }

    public void unsetLoginContextFactory(LoginContextFactory factory) {
        this.loginContextFactory = null;
    }

}
