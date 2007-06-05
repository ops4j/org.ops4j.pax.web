package org.ops4j.pax.web.service.internal.ng;

import java.util.Dictionary;
import java.util.Collection;
import javax.servlet.Servlet;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

public interface Registrations
{
    HttpTarget[] get();
    HttpTarget registerServlet( String alias, Servlet servlet, Dictionary initParams, HttpContext context )
        throws NamespaceException;
    HttpTarget registerResources( String alias, String name, HttpContext context )
        throws NamespaceException;
    void unregister( HttpTarget httpTarget );
    HttpTarget getByAlias( String alias );
}
