package org.ops4j.pax.web.service.internal.ng;

import java.util.Dictionary;
import java.util.Collection;
import javax.servlet.Servlet;
import org.osgi.service.http.HttpContext;

public interface Registrations
{
    Collection<HttpTarget> get();
    HttpTarget registerServlet( String alias, Servlet servlet, Dictionary initParams, HttpContext context );
    HttpTarget getByAlias( String alias );
}
