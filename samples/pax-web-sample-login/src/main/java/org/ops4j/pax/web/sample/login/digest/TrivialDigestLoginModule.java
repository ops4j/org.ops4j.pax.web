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
package org.ops4j.pax.web.sample.login.digest;

import static java.nio.charset.StandardCharsets.UTF_8;
import io.undertow.util.HexConverter;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.ops4j.pax.web.undertow.security.PasswordValidator;
import org.ops4j.pax.web.undertow.security.PasswordValidatorCallback;

public class TrivialDigestLoginModule implements LoginModule {

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
        
        NameCallback nameCallback = new NameCallback("Username:");
        PasswordValidatorCallback validatorCallback = new PasswordValidatorCallback();
        Callback[] callbacks = new Callback[] { nameCallback, validatorCallback};

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
        String user = nameCallback.getName();
        if (user == null) {
            return false;
        }

        boolean authenticated = false;
        
        PasswordValidator validator = validatorCallback.getPasswordValidator();
        
        String realm = "test-digest";
        
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
            digest.update(user.getBytes(UTF_8));
            digest.update((byte) ':');
            digest.update(realm.getBytes(UTF_8));
            digest.update((byte) ':');
            char[] expectedPassword = user.toCharArray();
            digest.update(new String(expectedPassword).getBytes(UTF_8));

            byte[] ha1 = HexConverter.convertToHexBytes(digest.digest());
            authenticated = validator.validatePassword(ha1);
        }
        catch (NoSuchAlgorithmException e) {
            throw new LoginException(e.getMessage());
        }
        
        

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
