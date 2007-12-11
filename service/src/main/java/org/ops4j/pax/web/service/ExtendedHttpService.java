package org.ops4j.pax.web.service;

import java.util.EventListener;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.HttpContext;

/**
 * xtended Http Service allows bundles to dynamically:<br/>
 * * register and unregister event listeners, for better control over the life cycle of ServletContext, HttpSession and
 * ServletRequest;<br/>
 * * register and unregister filters into the URI namespace of Http Service
 *
 * @author Alin Dreghiciu
 * @since 0.5.2
 */
public interface ExtendedHttpService
    extends HttpService
{

    /**
     * Registers an event listener.
     * Depending on the listener type, the listener will be notified on different life cycle events. The following listeners are supported:
     * HttpSessionActivationListener, HttpSessionAttributeListener, HttpSessionBindingListener, HttpSessionListener, ServletContextListener, ServletContextAttributeListener, ServletRequestListener, ServletRequestAttributeListener.
     * Check out Servlet specification for details on what type of event the registered listener will be notified.
     *
     * @param listener an event listener to be registered. If null an IllegalArgumentException is thrown.
     * @param httpContext the http context this listener is for. If null a default http context will be used.
     */
    void registerEventListener( EventListener listener, HttpContext httpContext);

    /**
     * Unregisters a previously registered listener.
     * If the listener was not registered before or was already unregistered the method will return silently without
     * throwing any exception.
     *
     * @param listener the event listener to be unregistered.  If null an IllegalArgumentException is thrown.
     */
    void unregisterEventListener( EventListener listener );

}
