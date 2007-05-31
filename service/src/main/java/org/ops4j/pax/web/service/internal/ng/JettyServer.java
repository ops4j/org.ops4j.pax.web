package org.ops4j.pax.web.service.internal.ng;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.servlet.ServletHandler;
import javax.servlet.Servlet;

interface JettyServer
{
    void start();
    void stop();
    void addConnector( Connector connector );
    void addContext( ServletHandler servletHandler );
    void addServlet( String alias, Servlet servlet );
}
