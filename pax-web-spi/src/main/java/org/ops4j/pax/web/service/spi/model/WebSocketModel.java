package org.ops4j.pax.web.service.spi.model;


public class WebSocketModel extends Model {

    private Object webSocket;

    public WebSocketModel(ContextModel contextModel, Object webSocket) {
        super(contextModel);
        this.webSocket = webSocket;
    }
    
    public Object getWebSocket() {
        return webSocket;
    }

}
