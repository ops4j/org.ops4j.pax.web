package org.ops4j.pax.web.extender.whiteboard;

public interface WebSocketMapping {
    
    /**
     * Getter.
     * 
     * @return id of the http context this filter belongs to
     */
    String getHttpContextId();

    void setHttpContextId(String httpContextId);

    Boolean getSharedContext();
    
    void setSharedContext(Boolean extractSharedHttpContext);

    Object getWebSocket();
    
    void setWebSocket(Object published);

}
