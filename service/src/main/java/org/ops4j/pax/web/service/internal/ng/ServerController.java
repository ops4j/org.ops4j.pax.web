package org.ops4j.pax.web.service.internal.ng;

import javax.servlet.Servlet;
import org.ops4j.pax.web.service.HttpServiceConfiguration;

interface ServerController
{
    void start();
    void stop();
    void configure( HttpServiceConfiguration configuration );
    HttpServiceConfiguration getConfiguration();
    void addListener( ServerListener listener );
    String addServlet( String alias, Servlet servlet );
    void removeServlet( String name );
    boolean isStarted();

}
