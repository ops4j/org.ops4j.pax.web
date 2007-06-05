package org.ops4j.pax.web.service.internal;

import org.osgi.service.http.HttpService;

public interface StoppableHttpService extends HttpService
{
    void stop();
}
