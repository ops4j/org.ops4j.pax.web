package org.ops4j.pax.web.sample.websockethazelcast;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

/**
 * @author Jimmy Pannier
 *
 */
@WebSocket
public class KarafWebSocket {
  
  @OnWebSocketConnect
  public void onOpen(Session session) {
    KarafWebSocketActivator.registerConnection(session);
  }
  
  @OnWebSocketClose
  public void onClose(Session session, int statusCode, String reason) {
    KarafWebSocketActivator.unregisterConnection(session);
  }
  
  @OnWebSocketMessage
  public void onText(Session session, String message) {
    KarafWebSocketActivator.onMessage(message, session);
  }
}
