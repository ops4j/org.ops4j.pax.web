package org.ops4j.pax.web.undertow.security;

import javax.security.auth.callback.Callback;


public class PasswordValidatorCallback implements Callback {
    
    private PasswordValidator passwordValidator;

    
    public PasswordValidator getPasswordValidator() {
        return passwordValidator;
    }

    
    public void setPasswordValidator(PasswordValidator passwordValidator) {
        this.passwordValidator = passwordValidator;
    }
    

}
