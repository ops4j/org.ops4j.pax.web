package org.ops4j.pax.web.service;

import java.util.EventListener;
import org.osgi.service.http.HttpService;

public interface ExtendedHttpService
    extends HttpService
{

    void registerEventListener( EventListener listener );

    void unregisterEventListener( EventListener listener );

}
