package org.ops4j.pax.web.service.internal.ng;

import javax.servlet.Servlet;
import org.ops4j.pax.web.service.HttpServiceConfiguration;

interface ServerController
{
    void start();
    void stop();
    void configure( HttpServiceConfiguration configuration );
    HttpServiceConfiguration getConfiguration();
    public RegistrationsCluster getRegistrationsCluster();
    void addListener( ServerListener listener );
    void addServlet( String alias, Servlet servlet );
    boolean isStarted();
}
