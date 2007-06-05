package org.ops4j.pax.web.service.internal.ng;

import javax.servlet.Servlet;

public interface RegistrationsCluster
{
    void remove( Registrations registrations );
    HttpTarget getByAlias( String alias );
    Registrations create();
    boolean containsServlet( Servlet servlet );
}
