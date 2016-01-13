package org.ops4j.pax.web.service.webapp.bridge;

import javax.servlet.*;
import javax.servlet.http.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by loom on 13.01.16.
 */
public class EventDispatcher implements ServletContextAttributeListener,ServletRequestListener,ServletRequestAttributeListener,HttpSessionListener,HttpSessionAttributeListener,HttpSessionIdListener {

    List<ServletContextAttributeListener> servletContextAttributeListeners = new ArrayList<ServletContextAttributeListener>();
    List<ServletRequestListener> servletRequestListeners = new ArrayList<ServletRequestListener>();
    List<ServletRequestAttributeListener> servletRequestAttributeListeners = new ArrayList<ServletRequestAttributeListener>();
    List<HttpSessionListener> httpSessionListeners = new ArrayList<>();
    List<HttpSessionAttributeListener> httpSessionAttributeListeners = new ArrayList<>();
    List<HttpSessionIdListener> httpSessionIdListeners = new ArrayList<>();

    public List<ServletContextAttributeListener> getServletContextAttributeListeners() {
        return servletContextAttributeListeners;
    }

    public List<ServletRequestListener> getServletRequestListeners() {
        return servletRequestListeners;
    }

    public List<ServletRequestAttributeListener> getServletRequestAttributeListeners() {
        return servletRequestAttributeListeners;
    }

    public List<HttpSessionListener> getHttpSessionListeners() {
        return httpSessionListeners;
    }

    public List<HttpSessionAttributeListener> getHttpSessionAttributeListeners() {
        return httpSessionAttributeListeners;
    }

    public List<HttpSessionIdListener> getHttpSessionIdListeners() {
        return httpSessionIdListeners;
    }

    @Override
    public void attributeAdded(HttpSessionBindingEvent event) {
        for (HttpSessionAttributeListener httpSessionAttributeListener : httpSessionAttributeListeners) {
            httpSessionAttributeListener.attributeAdded(event);
        }
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent event) {
        for (HttpSessionAttributeListener httpSessionAttributeListener : httpSessionAttributeListeners) {
            httpSessionAttributeListener.attributeRemoved(event);
        }
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent event) {
        for (HttpSessionAttributeListener httpSessionAttributeListener : httpSessionAttributeListeners) {
            httpSessionAttributeListener.attributeReplaced(event);
        }
    }

    @Override
    public void sessionIdChanged(HttpSessionEvent event, String oldSessionId) {
        for (HttpSessionIdListener httpSessionIdListener : httpSessionIdListeners) {
            httpSessionIdListener.sessionIdChanged(event, oldSessionId);
        }
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        for (HttpSessionListener httpSessionListener : httpSessionListeners) {
            httpSessionListener.sessionCreated(se);
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        for (HttpSessionListener httpSessionListener : httpSessionListeners) {
            httpSessionListener.sessionDestroyed(se);
        }
    }

    @Override
    public void attributeAdded(ServletContextAttributeEvent event) {
        for (ServletContextAttributeListener servletContextAttributeListener : servletContextAttributeListeners) {
            servletContextAttributeListener.attributeAdded(event);
        }
    }

    @Override
    public void attributeRemoved(ServletContextAttributeEvent event) {
        for (ServletContextAttributeListener servletContextAttributeListener : servletContextAttributeListeners) {
            servletContextAttributeListener.attributeRemoved(event);
        }
    }

    @Override
    public void attributeReplaced(ServletContextAttributeEvent event) {
        for (ServletContextAttributeListener servletContextAttributeListener : servletContextAttributeListeners) {
            servletContextAttributeListener.attributeReplaced(event);
        }
    }

    @Override
    public void attributeAdded(ServletRequestAttributeEvent srae) {
        for (ServletRequestAttributeListener servletRequestAttributeListener : servletRequestAttributeListeners) {
            servletRequestAttributeListener.attributeAdded(srae);
        }
    }

    @Override
    public void attributeRemoved(ServletRequestAttributeEvent srae) {
        for (ServletRequestAttributeListener servletRequestAttributeListener : servletRequestAttributeListeners) {
            servletRequestAttributeListener.attributeRemoved(srae);
        }
    }

    @Override
    public void attributeReplaced(ServletRequestAttributeEvent srae) {
        for (ServletRequestAttributeListener servletRequestAttributeListener : servletRequestAttributeListeners) {
            servletRequestAttributeListener.attributeReplaced(srae);
        }
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        for (ServletRequestListener servletRequestListener : servletRequestListeners) {
            servletRequestListener.requestDestroyed(sre);
        }
    }

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        for (ServletRequestListener servletRequestListener : servletRequestListeners) {
            servletRequestListener.requestInitialized(sre);
        }
    }
}
