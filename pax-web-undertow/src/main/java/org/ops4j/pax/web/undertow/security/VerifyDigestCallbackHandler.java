package org.ops4j.pax.web.undertow.security;

import io.undertow.security.idm.DigestCredential;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;


public class VerifyDigestCallbackHandler implements CallbackHandler {
    
    
    private DigestCredential digestCredential;
    private String username;

    public VerifyDigestCallbackHandler(String username, DigestCredential digestCredential) {
        this.username = username;
        this.digestCredential = digestCredential;
    }
    
    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback c : callbacks) {
            if (c instanceof NameCallback) {
                NameCallback nc = (NameCallback) c;
                nc.setName(username);;
            }
            if (c instanceof PasswordValidatorCallback) {
                PasswordValidatorCallback pvc = (PasswordValidatorCallback) c;
                
                pvc.setPasswordValidator(new PasswordValidator() {

                    @Override
                    public boolean validatePassword(byte[] password) {
                        return digestCredential.verifyHA1(password);
                    }
                    
                });
                
            }
        }
    }


}
