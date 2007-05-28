package org.ops4j.pax.web.service.internal;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.util.Dictionary;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

/**
 * Created by IntelliJ IDEA.
 * User: alin.dreghiciu
 * Date: May 28, 2007
 * Time: 11:49:07 PM
 * To change this template use File | Settings | File Templates.
 */
public interface OsgiHandler
{
    void registerServlet( String alias, Servlet servlet, Dictionary initParams, HttpContext context )
        throws NamespaceException, ServletException;

    void registerResource( String alias, String name, HttpContext context )
            throws NamespaceException;

    void unregister( String alias )
                throws Exception;
}
