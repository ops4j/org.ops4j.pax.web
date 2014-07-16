package org.ops4j.pax.web.undertow.security;


public interface PasswordValidator {

    boolean validatePassword(byte[] password);
    
}
