package org.ops4j.pax.web.service.internal.ng;

import java.util.Dictionary;
import java.util.Collection;
import javax.servlet.Servlet;
import org.osgi.service.http.HttpContext;

public interface RegistrationRepository
{
    Collection<Registration> get();
    Registration registerServlet( String alias, Servlet servlet, Dictionary initParams, HttpContext context );
}
