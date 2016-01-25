package org.ops4j.pax.web.service.webapp.proxy;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.*;
import java.util.EventListener;

/**
 * An service tracker to track the bridge EventDispatcher
 */
public final class EventDispatcherTracker
        extends BridgeServiceTracker<EventListener> {
    private ServletContextListener servletContextListener;
    private ServletContextAttributeListener servletContextAttributeListener;
    private ServletRequestListener servletRequestListener;
    private ServletRequestAttributeListener servletRequestAttributeListener;
    private HttpSessionListener httpSessionListener;
    private HttpSessionAttributeListener httpSessionAttributeListener;
    private HttpSessionIdListener httpSessionIdListener;

    public EventDispatcherTracker(final BundleContext context)
            throws InvalidSyntaxException {
        super(context, EventListener.class);
    }

    @Override
    protected void setService(final EventListener service) {
        if (service instanceof ServletContextListener) {
            this.servletContextListener = (ServletContextListener) service;
        }
        if (service instanceof ServletContextAttributeListener) {
            this.servletContextAttributeListener = (ServletContextAttributeListener) service;
        }
        if (service instanceof ServletRequestListener) {
            this.servletRequestListener = (ServletRequestListener) service;
        }
        if (service instanceof ServletRequestAttributeListener) {
            this.servletRequestAttributeListener = (ServletRequestAttributeListener) service;
        }
        if (service instanceof HttpSessionListener) {
            this.httpSessionListener = (HttpSessionListener) service;
        }
        if (service instanceof HttpSessionAttributeListener) {
            this.httpSessionAttributeListener = (HttpSessionAttributeListener) service;
        }
        if (service instanceof HttpSessionIdListener) {
            this.httpSessionIdListener = (HttpSessionIdListener) service;
        }
    }

    public ServletContextListener getServletContextListener() {
        return servletContextListener;
    }

    public ServletContextAttributeListener getServletContextAttributeListener() {
        return servletContextAttributeListener;
    }

    public ServletRequestListener getServletRequestListener() {
        return servletRequestListener;
    }

    public ServletRequestAttributeListener getServletRequestAttributeListener() {
        return servletRequestAttributeListener;
    }

    public HttpSessionListener getHttpSessionListener() {
        return httpSessionListener;
    }

    public HttpSessionAttributeListener getHttpSessionAttributeListener() {
        return httpSessionAttributeListener;
    }

    public HttpSessionIdListener getHttpSessionIdListener() {
        return httpSessionIdListener;
    }

    @Override
    protected void unsetService() {
        servletContextListener = null;
        servletContextAttributeListener = null;
        servletRequestListener = null;
        servletRequestAttributeListener = null;
        httpSessionListener = null;
        httpSessionIdListener = null;
        httpSessionAttributeListener = null;
    }
}
