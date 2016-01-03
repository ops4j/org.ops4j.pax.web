package org.ops4j.pax.web.extender.whiteboard.internal.element;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.whiteboard.WebSocketMapping;
import org.ops4j.pax.web.extender.whiteboard.internal.util.WebContainerUtils;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

public class WebSocketElement implements WebElement {

    private WebSocketMapping mapping;

    public WebSocketElement(WebSocketMapping mapping) {
        NullArgumentException.validateNotNull(mapping, "Websocket mapping");
        this.mapping = mapping;
    }

    @Override
    public void register(HttpService httpService, HttpContext httpContext) throws Exception {
        if (WebContainerUtils.isWebContainer(httpService)) {
            ((WebContainer) httpService).registerWebSocket(mapping.getWebSocket(), httpContext);
        } else {
            throw new UnsupportedOperationException(
                    "Internal error: In use HttpService is not an WebContainer (from Pax Web)");
        }
    }

    @Override
    public void unregister(HttpService httpService, HttpContext httpContext) {
        if (WebContainerUtils.isWebContainer(httpService)) {
            ((WebContainer)httpService).unregisterWebSocket(mapping.getWebSocket(), httpContext);
        }
    }

    @Override
    public String getHttpContextId() {
        return mapping.getHttpContextId();
    }

}
