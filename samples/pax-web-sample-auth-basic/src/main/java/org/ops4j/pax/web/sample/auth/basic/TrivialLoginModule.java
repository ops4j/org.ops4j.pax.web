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
package org.ops4j.pax.web.sample.auth.basic;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;

public class TrivialLoginModule implements LoginModule {

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Set<Principal> principals;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
        Map<String, ?> sharedState, Map<String, ?> options) {
        if (subject == null) {
            this.subject = new Subject();
        }
        else {
            this.subject = subject;
        }
        this.callbackHandler = callbackHandler;
    }

    @Override
    public boolean login() throws LoginException {
        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("Username: ");
        callbacks[1] = new PasswordCallback("Password: ", false);

        try {
            callbackHandler.handle(callbacks);
        }
        catch (IOException ioe) {
            throw new LoginException(ioe.getMessage());
        }
        catch (UnsupportedCallbackException uce) {
            throw new LoginException(uce.getMessage()
                + " not available to obtain information from user");
        }

        principals = new HashSet<>();
        String user = ((NameCallback) callbacks[0]).getName();
        if (user == null) {
            return false;
        }

        char[] password = ((PasswordCallback) callbacks[1]).getPassword();

        boolean authenticated = Arrays.equals(user.toCharArray(), password);
        if (authenticated) {
            principals.add(new UserPrincipal(user));
            principals.add(new RolePrincipal("USER"));
        }
        return authenticated;
    }

    @Override
    public boolean commit() throws LoginException {
        subject.getPrincipals().addAll(principals);
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        subject.getPrincipals().removeAll(principals);
        principals.clear();
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        return false;
    }

}
