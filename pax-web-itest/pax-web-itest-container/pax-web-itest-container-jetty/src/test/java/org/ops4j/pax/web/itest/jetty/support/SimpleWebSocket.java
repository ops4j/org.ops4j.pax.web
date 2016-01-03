package org.ops4j.pax.web.itest.jetty.support;

import java.util.ArrayList;
import java.util.List;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ClientEndpoint
@ServerEndpoint(value="/simple/")
public class SimpleWebSocket {
    
    List<String> messages = new ArrayList<>();
    
    @OnOpen
    public void onWebSocketConnect(Session sess)
    {
        System.out.println("Socket Connected: " + sess);
    }
    
    @OnMessage
    public void onWebSocketText(String message)
    {
        System.out.println("Received TEXT message: " + message);
    }
    
    @OnClose
    public void onWebSocketClose(CloseReason reason)
    {
        System.out.println("Socket Closed: " + reason);
    }
    
    @OnError
    public void onWebSocketError(Throwable cause)
    {
        cause.printStackTrace(System.err);
    }
    
    public List<String> getMessages() {
        return messages;
    }

}
