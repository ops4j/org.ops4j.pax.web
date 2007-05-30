package org.ops4j.pax.web.service.internal.ng;

import javax.servlet.Servlet;
import org.ops4j.pax.web.service.HttpServiceConfiguration;

interface HttpServiceServer
{
    void start();
    void stop();
    void configure( HttpServiceConfiguration configuration );
    HttpServiceConfiguration getConfiguration();    
    void addListener( HttpServiceServerListener listener );
    void addServlet( String alias, Servlet servlet );
    boolean isStarted();
}
