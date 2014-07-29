package org.ops4j.pax.web.sample.websockethazelcast;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import javax.servlet.annotation.WebServlet;

/**
 * @author Jimmy Pannier
 *
 */
@SuppressWarnings("serial")
@WebServlet(name = "Cloud websocket Servlet", urlPatterns = {"/websocket"})
public class KarafWebSocketServlet extends WebSocketServlet {
  
  @Override
  public void configure(WebSocketServletFactory factory) {
    factory.register(KarafWebSocket.class);
  }
}