package org.ops4j.pax.web.service;

import java.util.Dictionary;
import java.util.EventListener;
import javax.servlet.Filter;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

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
     * Depending on the listener type, the listener will be notified on different life cycle events. The following
     * listeners are supported:<br/>
     * HttpSessionActivationListener, HttpSessionAttributeListener, HttpSessionBindingListener, HttpSessionListener,
     * ServletContextListener, ServletContextAttributeListener, ServletRequestListener, ServletRequestAttributeListener.
     * Check out Servlet specification for details on what type of event the registered listener will be notified.
     *
     * @param listener    an event listener to be registered. If null an IllegalArgumentException is thrown.
     * @param httpContext the http context this listener is for. If null a default http context will be used.
     */
    void registerEventListener( EventListener listener, HttpContext httpContext );

    /**
     * Unregisters a previously registered listener.
     *
     * @param listener the event listener to be unregistered.
     *
     * @throws IllegalArgumentException if the listener is unknown to the http service (never registered or unregistered
     *                                  before) or the listener is null
     */
    void unregisterEventListener( EventListener listener );

    /**
     * Registers a servlet flter.
     *
     * @param filter      a servlet filter. If null an IllegalArgumentException is thrown.
     * @param urlPatterns url patterns this filter maps to
     * @param aliases     servlet / resource aliases this filter maps to
     * @param initparams  initialization arguments for the filter or null if there are none. This argument is used by
     *                    the filters’s FilterConfig object.
     * @param httpContext the http context this filter is for. If null a default http context will be used.
     */
    void registerFilter( Filter filter, String[] urlPatterns, String[] aliases, Dictionary initparams,
                         HttpContext httpContext );

    /**
     * Unregisters a previously registeredservlet filter.
     *
     * @param filter the servlet filter to be unregistered
     *
     * @throws IllegalArgumentException if the filter is unknown to the http service (never registered or unregistered
     *                                  before) or the filter is null
     */
    void unregisterFilter( Filter filter );

    /**
     * Sets context paramaters to be used in the servlet context corresponding to specified http context.
     * This method must be used before any register method that uses the specified http context, otherwise an
     * IllegalStateException will be thrown.
     *
     * @param params      context parameters for the servlet context corresponding to specified http context
     * @param httpContext http context. Cannot be null.
     *
     * @throws IllegalArgumentException if http context is null
     * @throws IllegalStateException    if the call is made after the http context was already used into a registration
     */
    void setContextParam( Dictionary params, HttpContext httpContext );

}
