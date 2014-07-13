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
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.felix.jaas.LoginContextFactory;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;

public class JaasIdentityManager implements IdentityManager {

    private String realmName;
    private LoginContextFactory loginContextFactory;

    public JaasIdentityManager(String realmName) {
        this.realmName = realmName;
    }

    @Override
    public Account verify(Account account) {
        if (account instanceof SimpleAccount) {
            SimpleAccount simpleAccount = (SimpleAccount) account;
            char[] password = (char[]) simpleAccount.getCredential();
            return verifyPassword(simpleAccount.getPrincipal().getName(), password);
        }
        return null;
    }

    @Override
    public Account verify(final String id, final Credential credential) {
            char[] password;
            if (credential instanceof PasswordCredential) {
                PasswordCredential passCred = (PasswordCredential) credential;
                password = passCred.getPassword();
                return verifyPassword(id, password);
            }
            return null;
    }

    private Account verifyPassword(String id, char[] password) {
        try {
            char[] passwordCopy = Arrays.copyOf(password, password.length);
            CallbackHandler handler = new NamePasswordCallbackHandler(id, password);
            Subject subject = new Subject();
            LoginContext loginContext = loginContextFactory.createLoginContext(realmName, subject,
                handler);
            loginContext.login();

            Set<String> roles = new HashSet<>();
            for (RolePrincipal role : subject.getPrincipals(RolePrincipal.class)) {
                roles.add(role.getName());
            }
            return new SimpleAccount(id, passwordCopy, roles);
        }
        catch (LoginException e) {
            return null;
        }

    }

    @Override
    public Account verify(Credential credential) {
        throw new UnsupportedOperationException();
    }

    public void setLoginContextFactory(LoginContextFactory factory) {
        this.loginContextFactory = factory;
    }
}
