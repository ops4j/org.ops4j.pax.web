package org.ops4j.pax.web.service.internal.ng;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.osgi.service.http.HttpContext;

public interface HttpTarget
{
    void register( ServerController serverController );
    String getAlias();
    HttpContext getHttpContext();
    Type getType();

    public static enum Type {
        SERVLET, RESOURCE
    }
}
