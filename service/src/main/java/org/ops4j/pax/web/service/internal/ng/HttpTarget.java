package org.ops4j.pax.web.service.internal.ng;

import org.osgi.service.http.HttpContext;

public interface HttpTarget
{
    void register( ServerController serverController );
    String getAlias();
    HttpContext getHttpContext();
}
