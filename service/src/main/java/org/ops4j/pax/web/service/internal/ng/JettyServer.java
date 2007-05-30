package org.ops4j.pax.web.service.internal.ng;

import org.mortbay.jetty.Connector;
import javax.servlet.Servlet;

interface JettyServer
{
    void start();
    void stop();
    void addConnector( Connector connector );
    void addContext();
    void addServlet( String alias, Servlet servlet );
}
