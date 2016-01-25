package org.ops4j.pax.web.service.webapp.bridge.internal;

import javax.servlet.ServletContext;
import javax.servlet.http.*;
import java.util.*;

/**
 * This implementation of an HttpSession will actually be stored within the parent (bridged) session as an attribute
 * using a prefix (@see BridgeHttpServletRequestWrapper for the attribute name)
 */
public class BridgeHttpSession implements HttpSession, HttpSessionBindingListener, HttpSessionActivationListener {

    HttpSession parentSession;
    BridgeServletContext bridgeServletContext;
    Map<String,Object> attributes = new LinkedHashMap<String,Object>();

    public BridgeHttpSession(HttpSession parentSession, BridgeServletContext bridgeServletContext) {
        this.parentSession = parentSession;
        this.bridgeServletContext = bridgeServletContext;
        fireSessionCreatedEvent();
    }

    @Override
    public long getCreationTime() {
        return parentSession.getCreationTime();
    }

    @Override
    public String getId() {
        return parentSession.getId();
    }

    @Override
    public long getLastAccessedTime() {
        return parentSession.getLastAccessedTime();
    }

    @Override
    public ServletContext getServletContext() {
        return bridgeServletContext;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        parentSession.setMaxInactiveInterval(interval);
    }

    @Override
    public int getMaxInactiveInterval() {
        return parentSession.getMaxInactiveInterval();
    }

    @Override
    public HttpSessionContext getSessionContext() {
        return parentSession.getSessionContext();
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Object getValue(String name) {
        return getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return new Vector<String>(attributes.keySet()).elements();
    }

    @Override
    public String[] getValueNames() {
        return new ArrayList<String>(attributes.keySet()).toArray(new String[attributes.keySet().size()]);
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (value == null) {
            removeAttribute(name);
            return;
        }
        Object oldValue = attributes.get(name);
        attributes.put(name, value);
        if (oldValue != null) {
            fireSessionAttributeReplaced(name, oldValue);
            fireValueUnbound(name, oldValue);
            fireValueBound(name, value);
        } else {
            fireSessionAttributeAdded(name, value);
            fireValueBound(name, value);
        }
    }

    @Override
    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        Object value = attributes.remove(name);
        if (value != null) {
            fireSessionAttributeRemoved(name, value);
            fireValueUnbound(name, value);
        }
    }

    @Override
    public void removeValue(String name) {
        removeAttribute(name);
    }

    @Override
    public void invalidate() {
        parentSession.invalidate();
    }

    @Override
    public boolean isNew() {
        return false;
    }

    public void fireSessionCreatedEvent() {
        for (HttpSessionListener httpSessionListener : bridgeServletContext.getHttpSessionListeners()) {
            httpSessionListener.sessionCreated(new HttpSessionEvent(this));
        }
    }

    public void fireSessionDestroyedEvent() {
        for (HttpSessionListener httpSessionListener : bridgeServletContext.getHttpSessionListeners()) {
            httpSessionListener.sessionDestroyed(new HttpSessionEvent(this));
        }
    }

    public void fireSessionAttributeAdded(String name, Object value) {
        for (HttpSessionAttributeListener httpSessionAttributeListener : bridgeServletContext.getHttpSessionAttributeListeners()) {
            httpSessionAttributeListener.attributeAdded(new HttpSessionBindingEvent(this, name, value));
        }
    }

    public void fireSessionAttributeReplaced(String name, Object value) {
        for (HttpSessionAttributeListener httpSessionAttributeListener : bridgeServletContext.getHttpSessionAttributeListeners()) {
            httpSessionAttributeListener.attributeReplaced(new HttpSessionBindingEvent(this, name, value));
        }
    }

    public void fireSessionAttributeRemoved(String name, Object value) {
        for (HttpSessionAttributeListener httpSessionAttributeListener : bridgeServletContext.getHttpSessionAttributeListeners()) {
            httpSessionAttributeListener.attributeRemoved(new HttpSessionBindingEvent(this, name, value));
        }
    }

    public void fireValueBound(String name, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof HttpSessionBindingListener) {
            ((HttpSessionBindingListener) value).valueBound(new HttpSessionBindingEvent(this, name, value));
        }
    }

    public void fireValueUnbound(String name, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof HttpSessionBindingListener) {
            ((HttpSessionBindingListener) value).valueUnbound(new HttpSessionBindingEvent(this, name, value));
        }
    }

    public void fireSessionDidActivate(Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof HttpSessionActivationListener) {
            ((HttpSessionActivationListener) value).sessionDidActivate(new HttpSessionEvent(this));
        }
    }

    public void fireSessionWillPassivate(Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof HttpSessionActivationListener) {
            ((HttpSessionActivationListener) value).sessionWillPassivate(new HttpSessionEvent(this));
        }
    }

    @Override
    public void valueBound(HttpSessionBindingEvent event) {
        fireSessionCreatedEvent();
    }

    @Override
    public void valueUnbound(HttpSessionBindingEvent event) {
        for (Map.Entry<String,Object> attributeEntry : attributes.entrySet()) {
            fireValueUnbound(attributeEntry.getKey(), attributeEntry.getValue());
        }
        fireSessionDestroyedEvent();
    }

    @Override
    public void sessionWillPassivate(HttpSessionEvent se) {
        for (Object attributeValue : attributes.values()) {
            fireSessionWillPassivate(attributeValue);
        }
    }

    @Override
    public void sessionDidActivate(HttpSessionEvent se) {
        for (Object attributeValue : attributes.values()) {
            fireSessionDidActivate(attributeValue);
        }
    }
}
