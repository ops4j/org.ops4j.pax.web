package org.ops4j.pax.web.service.webapp.proxy;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

import javax.servlet.*;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * A web application context listener that registers event listeners to dispatch to OSGi bridge event listeners
 */
@WebListener
public class ContextListener implements ServletContextListener {

    private ServletContext servletContext;
    private EventDispatcherTracker eventDispatcherTracker = null;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        servletContext = sce.getServletContext();
        addServletContextAttributeListener();
        addServletRequestListener();
        addServletRequestAttributeListener();
        addHttpSessionListener();
        addHttpSessionAttributeListener();
        addHttpSessionIdListener();
        if (eventDispatcherTracker != null) {
            ServletContextListener servletContextListener = eventDispatcherTracker.getServletContextListener();
            if (servletContextListener != null) {
                servletContextListener.contextInitialized(sce);
            }
        }
    }

    private void addServletContextAttributeListener() {
        Object bundleContextAttributeValue = servletContext.getAttribute(BundleContext.class.getName());
        if (eventDispatcherTracker == null && bundleContextAttributeValue != null) {
            startTrackers(bundleContextAttributeValue);
        }
        servletContext.addListener(new ServletContextAttributeListener() {

            private ServletContextAttributeListener getServletContextAttributeListener() {
                if (eventDispatcherTracker == null) {
                    return null;
                }
                return eventDispatcherTracker.getServletContextAttributeListener();
            }

            @Override
            public void attributeAdded(final ServletContextAttributeEvent event) {
                if (event.getName().equals(BundleContext.class.getName())) {
                    startTrackers(event.getValue());
                } else {
                    ServletContextAttributeListener servletContextAttributeListener = getServletContextAttributeListener();
                    if (servletContextAttributeListener != null) {
                        servletContextAttributeListener.attributeAdded(event);
                    }
                }
            }

            @Override
            public void attributeRemoved(final ServletContextAttributeEvent event) {
                if (event.getName().equals(BundleContext.class.getName())) {
                    stopTrackers();
                } else {
                    ServletContextAttributeListener servletContextAttributeListener = getServletContextAttributeListener();
                    if (servletContextAttributeListener != null) {
                        servletContextAttributeListener.attributeRemoved(event);
                    }
                }
            }

            @Override
            public void attributeReplaced(final ServletContextAttributeEvent event) {
                if (event.getName().equals(BundleContext.class.getName())) {
                    stopTrackers();
                    startTrackers(event.getServletContext().getAttribute(event.getName()));
                } else {
                    ServletContextAttributeListener servletContextAttributeListener = getServletContextAttributeListener();
                    if (servletContextAttributeListener != null) {
                        servletContextAttributeListener.attributeReplaced(event);
                    }
                }
            }
        });
    }

    private void addServletRequestListener() {
        servletContext.addListener(new ServletRequestListener() {

            private ServletRequestListener getServletRequestListener() {
                if (eventDispatcherTracker == null) {
                    return null;
                }
                return eventDispatcherTracker.getServletRequestListener();
            }

            @Override
            public void requestDestroyed(ServletRequestEvent sre) {
                ServletRequestListener servletRequestListener = getServletRequestListener();
                if (servletRequestListener != null) {
                    servletRequestListener.requestDestroyed(sre);
                }
            }

            @Override
            public void requestInitialized(ServletRequestEvent sre) {
                ServletRequestListener servletRequestListener = getServletRequestListener();
                if (servletRequestListener != null) {
                    servletRequestListener.requestInitialized(sre);
                }
            }
        });
    }

    private void addServletRequestAttributeListener() {
        servletContext.addListener(new ServletRequestAttributeListener() {

            private ServletRequestAttributeListener getServletRequestAttributeListener() {
                if (eventDispatcherTracker == null) {
                    return null;
                }
                return eventDispatcherTracker.getServletRequestAttributeListener();
            }

            @Override
            public void attributeAdded(ServletRequestAttributeEvent srae) {
                ServletRequestAttributeListener servletRequestAttributeListener = getServletRequestAttributeListener();
                if (servletRequestAttributeListener != null) {
                    getServletRequestAttributeListener().attributeAdded(srae);
                }
            }

            @Override
            public void attributeRemoved(ServletRequestAttributeEvent srae) {
                ServletRequestAttributeListener servletRequestAttributeListener = getServletRequestAttributeListener();
                if (servletRequestAttributeListener != null) {
                    getServletRequestAttributeListener().attributeRemoved(srae);
                }
            }

            @Override
            public void attributeReplaced(ServletRequestAttributeEvent srae) {
                ServletRequestAttributeListener servletRequestAttributeListener = getServletRequestAttributeListener();
                if (servletRequestAttributeListener != null) {
                    getServletRequestAttributeListener().attributeReplaced(srae);
                }
            }
        });
    }


    private void addHttpSessionListener() {
        servletContext.addListener(new HttpSessionListener() {

            private HttpSessionListener getHttpSessionListener() {
                if (eventDispatcherTracker == null) {
                    return null;
                }
                return eventDispatcherTracker.getHttpSessionListener();
            }

            @Override
            public void sessionCreated(HttpSessionEvent se) {
                final HttpSessionListener httpSessionListener = getHttpSessionListener();
                if (httpSessionListener != null) {
                    httpSessionListener.sessionCreated(se);
                }
            }

            @Override
            public void sessionDestroyed(HttpSessionEvent se) {
                final HttpSessionListener httpSessionListener = getHttpSessionListener();
                if (httpSessionListener != null) {
                    httpSessionListener.sessionDestroyed(se);
                }
            }
        });
    }

    private void addHttpSessionAttributeListener() {
        servletContext.addListener(new HttpSessionAttributeListener() {

            private HttpSessionAttributeListener getHttpSessionAttributeListener() {
                if (eventDispatcherTracker == null) {
                    return null;
                }
                return eventDispatcherTracker.getHttpSessionAttributeListener();
            }

            @Override
            public void attributeAdded(HttpSessionBindingEvent event) {
                final HttpSessionAttributeListener httpSessionAttributeListener = getHttpSessionAttributeListener();
                if (httpSessionAttributeListener != null) {
                    httpSessionAttributeListener.attributeAdded(event);
                }
            }

            @Override
            public void attributeRemoved(HttpSessionBindingEvent event) {
                final HttpSessionAttributeListener httpSessionAttributeListener = getHttpSessionAttributeListener();
                if (httpSessionAttributeListener != null) {
                    httpSessionAttributeListener.attributeRemoved(event);
                }
            }

            @Override
            public void attributeReplaced(HttpSessionBindingEvent event) {
                final HttpSessionAttributeListener httpSessionAttributeListener = getHttpSessionAttributeListener();
                if (httpSessionAttributeListener != null) {
                    httpSessionAttributeListener.attributeReplaced(event);
                }
            }
        });
    }

    private void addHttpSessionIdListener() {
        servletContext.addListener(new HttpSessionIdListener() {

            private HttpSessionIdListener getHttpSessionIdListener() {
                if (eventDispatcherTracker == null) {
                    return null;
                }
                return eventDispatcherTracker.getHttpSessionIdListener();
            }

            @Override
            public void sessionIdChanged(HttpSessionEvent event, String oldSessionId) {
                final HttpSessionIdListener httpSessionIdListener = getHttpSessionIdListener();
                if (httpSessionIdListener != null) {
                    httpSessionIdListener.sessionIdChanged(event, oldSessionId);
                }
            }
        });
    }

    private void startTrackers(final Object bundleContextAttributeValue) {
        if (bundleContextAttributeValue instanceof BundleContext && this.eventDispatcherTracker == null) {
            try {
                final BundleContext bundleContext = (BundleContext) bundleContextAttributeValue;
                this.eventDispatcherTracker = new EventDispatcherTracker(bundleContext);
                this.eventDispatcherTracker.open();
            } catch (final InvalidSyntaxException e) {
                // not expected for our simple filter
            }
        }
    }

    private void stopTrackers() {
        if (this.eventDispatcherTracker != null) {
            this.eventDispatcherTracker.close();
            this.eventDispatcherTracker = null;
        }
    }


    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (eventDispatcherTracker != null) {
            ServletContextListener servletContextListener = eventDispatcherTracker.getServletContextListener();
            if (servletContextListener != null) {
                servletContextListener.contextDestroyed(sce);
            }
        }
        stopTrackers();
        servletContext = null;
    }
}
