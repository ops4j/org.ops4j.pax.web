package org.ops4j.pax.web.extender.whiteboard.runtime;

import org.ops4j.pax.web.extender.whiteboard.WebSocketMapping;

public class DefaultWebSocketMapping implements WebSocketMapping {

    private String httpContextId;
    private Boolean extractSharedHttpContext;
    private Object webSocket;

    @Override
    public String getHttpContextId() {
        return httpContextId;
    }

    @Override
    public void setHttpContextId(String httpContextId) {
        this.httpContextId = httpContextId;
    }

    @Override
    public Boolean getSharedContext() {
        return extractSharedHttpContext;
    }

    @Override
    public void setSharedContext(Boolean extractSharedHttpContext) {
        this.extractSharedHttpContext = extractSharedHttpContext;
    }

    @Override
    public Object getWebSocket() {
        return webSocket;
    }

    @Override
    public void setWebSocket(Object published) {
        this.webSocket = published;
    }

}
