package org.ops4j.pax.web.service.internal;

import org.osgi.service.http.HttpContext;

public interface HttpTarget
{
    void register( ServerController serverController );
    void unregister( ServerController serverController );
    String getAlias();
    HttpContext getHttpContext();
    Type getType();

    public static enum Type {
        SERVLET, RESOURCE
    }
}
