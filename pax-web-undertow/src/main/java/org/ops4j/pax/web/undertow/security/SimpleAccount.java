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

import io.undertow.security.idm.Account;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.ops4j.pax.web.jaas.UserPrincipal;


public class SimpleAccount implements Account {
    
    
    private Principal principal;
    private Set<String> roles;
    private Object credential;

    public SimpleAccount(String userName, Object credential, String... roles) {
        this.principal = new UserPrincipal(userName);
        this.credential = credential;
        Set<String> roleSet = new HashSet<String>(Arrays.asList(roles));
        this.roles = Collections.unmodifiableSet(roleSet);
    }

    public SimpleAccount(String userName, Object credential, Set<String> roles) {
        this.principal = new UserPrincipal(userName);
        this.credential = credential;
        this.roles = Collections.unmodifiableSet(roles);
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public Set<String> getRoles() {
        return roles;
    }

    
    public Object getCredential() {
        return credential;
    }
    
    
}
