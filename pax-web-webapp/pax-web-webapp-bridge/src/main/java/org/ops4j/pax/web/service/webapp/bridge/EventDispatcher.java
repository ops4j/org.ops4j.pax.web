package org.ops4j.pax.web.service.webapp.bridge;

import org.ops4j.pax.web.service.webapp.bridge.internal.BridgeHttpServletRequestWrapper;
import org.ops4j.pax.web.service.webapp.bridge.internal.BridgeHttpSession;
import org.ops4j.pax.web.service.webapp.bridge.internal.BridgeServer;
import org.ops4j.pax.web.service.webapp.bridge.internal.BridgeServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by loom on 13.01.16.
 */
public class EventDispatcher implements ServletContextListener, ServletContextAttributeListener,ServletRequestListener,ServletRequestAttributeListener,HttpSessionListener,HttpSessionAttributeListener,HttpSessionIdListener {

    private static final Logger logger = LoggerFactory.getLogger(EventDispatcher.class);

    private BridgeServer bridgeServer;

    public void setBridgeServer(BridgeServer bridgeServer) {
        this.bridgeServer = bridgeServer;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        bridgeServer.setProxyServletContext(sce.getServletContext());
        for (BridgeServletContext bridgeServletContext : bridgeServer.getBridgeServletContexts().values()) {
            // for the moment do nothing
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        for (BridgeServletContext bridgeServletContext : bridgeServer.getBridgeServletContexts().values()) {
            try {
                bridgeServletContext.stop();
            } catch (Exception e) {
                logger.warn("Error stopping bridge servlet context " + bridgeServletContext, e);
            }
        }
        this.bridgeServer.setProxyServletContext(null);
    }

    @Override
    public void attributeAdded(HttpSessionBindingEvent event) {
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent event) {
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent event) {
    }

    @Override
    public void sessionIdChanged(HttpSessionEvent event, String oldSessionId) {
        for (BridgeServletContext bridgeServletContext : bridgeServer.getBridgeServletContexts().values()) {
            if (bridgeServletContext.isStarted()) {
                BridgeHttpSession bridgeHttpSession = (BridgeHttpSession) event.getSession().getAttribute(BridgeHttpServletRequestWrapper.PAX_WEB_CONTEXT_SESSION_PREFIX + bridgeServletContext.getContextModel().getContextName());
                for (HttpSessionIdListener httpSessionIdListener : bridgeServletContext.getHttpSessionIdListeners()) {
                    httpSessionIdListener.sessionIdChanged(new HttpSessionEvent(bridgeHttpSession), oldSessionId);
                }
            }
        }
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
    }

    @Override
    public void attributeAdded(ServletContextAttributeEvent event) {
    }

    @Override
    public void attributeRemoved(ServletContextAttributeEvent event) {
    }

    @Override
    public void attributeReplaced(ServletContextAttributeEvent event) {
    }

    @Override
    public void attributeAdded(ServletRequestAttributeEvent srae) {
        for (BridgeServletContext bridgeServletContext : bridgeServer.getBridgeServletContexts().values()) {
            if (bridgeServletContext.isStarted()) {
                for (ServletRequestAttributeListener servletRequestAttributeListener : bridgeServletContext.getServletRequestAttributeListeners()) {
                    servletRequestAttributeListener.attributeAdded(new ServletRequestAttributeEvent(bridgeServletContext, srae.getServletRequest(), srae.getName(), srae.getValue()));
                }
            }
        }
    }

    @Override
    public void attributeRemoved(ServletRequestAttributeEvent srae) {
        for (BridgeServletContext bridgeServletContext : bridgeServer.getBridgeServletContexts().values()) {
            if (bridgeServletContext.isStarted()) {
                for (ServletRequestAttributeListener servletRequestAttributeListener : bridgeServletContext.getServletRequestAttributeListeners()) {
                    servletRequestAttributeListener.attributeRemoved(new ServletRequestAttributeEvent(bridgeServletContext, srae.getServletRequest(), srae.getName(), srae.getValue()));
                }
            }
        }
    }

    @Override
    public void attributeReplaced(ServletRequestAttributeEvent srae) {
        for (BridgeServletContext bridgeServletContext : bridgeServer.getBridgeServletContexts().values()) {
            if (bridgeServletContext.isStarted()) {
                for (ServletRequestAttributeListener servletRequestAttributeListener : bridgeServletContext.getServletRequestAttributeListeners()) {
                    servletRequestAttributeListener.attributeReplaced(new ServletRequestAttributeEvent(bridgeServletContext, srae.getServletRequest(), srae.getName(), srae.getValue()));
                }
            }
        }
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        for (BridgeServletContext bridgeServletContext : bridgeServer.getBridgeServletContexts().values()) {
            if (bridgeServletContext.isStarted()) {
                for (ServletRequestListener servletRequestListener : bridgeServletContext.getServletRequestListeners()) {
                    servletRequestListener.requestDestroyed(new ServletRequestEvent(bridgeServletContext, sre.getServletRequest()));
                }
            }
        }
    }

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        for (BridgeServletContext bridgeServletContext : bridgeServer.getBridgeServletContexts().values()) {
            if (bridgeServletContext.isStarted()) {
                for (ServletRequestListener servletRequestListener : bridgeServletContext.getServletRequestListeners()) {
                    servletRequestListener.requestInitialized(new ServletRequestEvent(bridgeServletContext, sre.getServletRequest()));
                }
            }
        }
    }

}
