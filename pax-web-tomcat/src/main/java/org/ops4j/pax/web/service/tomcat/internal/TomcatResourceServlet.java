package org.ops4j.pax.web.service.tomcat.internal;

import java.io.IOException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * can be based on org.apache.catalina.servlets.DefaultServlet
 * @author Romain Gilles
 *         Date: 7/26/12
 *         Time: 10:41 AM
 */
public class TomcatResourceServlet extends HttpServlet
{
    private static final Logger LOG = LoggerFactory.getLogger( TomcatResourceServlet.class );
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        LOG.debug("REQUEST: {}", req);
        LOG.debug("RESPONSE: {}", resp);
        super.doGet( req, resp );
    }
}
