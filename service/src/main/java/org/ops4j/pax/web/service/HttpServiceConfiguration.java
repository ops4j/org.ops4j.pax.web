package org.ops4j.pax.web.service;

public interface HttpServiceConfiguration
{
    int getHttpPort();
    boolean isHttpEnabled();
    int getHttpSecurePort();
    boolean isHttpSecureEnabled();
}
