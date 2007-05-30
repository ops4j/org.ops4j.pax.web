package org.ops4j.pax.web.service;

import org.ops4j.pax.web.service.HttpServiceConfiguration;

public interface HttpServiceConfigurer
{
    void configure( HttpServiceConfiguration configuration );
    HttpServiceConfiguration get();
}
