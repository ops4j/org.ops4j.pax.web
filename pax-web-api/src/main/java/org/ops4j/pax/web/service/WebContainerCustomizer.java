package org.ops4j.pax.web.service;

import javax.servlet.ServletContainerInitializer;

import org.osgi.framework.Bundle;


public interface WebContainerCustomizer {
    
    ServletContainerInitializer getServletContainerInitializer(Bundle webBundle);

}
