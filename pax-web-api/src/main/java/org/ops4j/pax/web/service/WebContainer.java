/* Copyright 2007 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service;

import java.net.URL;
import java.util.Dictionary;
import java.util.EventListener;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletException;

import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/**
 * WebContainer allows bundles to dynamically:<br/>
 * * register and unregister event listeners, for better control over the life cycle of ServletContext, HttpSession and
 * ServletRequest;<br/>
 * * register and unregister filters into the URI namespace of Http Service
 *
 * @author Alin Dreghiciu
 * @since 0.5.2
 */
public interface WebContainer extends HttpService {
	
	/**
	 * Registers a servlet into the URI namespace.
	 * 
	 * <p>
	 * The alias is the name in the URI namespace of the Http Service at which
	 * the registration will be mapped.
	 * 
	 * <p>
	 * An alias must begin with slash ('/') and must not end with slash ('/'),
	 * with the exception that an alias of the form &quot;/&quot; is used to
	 * denote the root alias. See the specification text for details on how HTTP
	 * requests are mapped to servlet and resource registrations.
	 * 
	 * <p>
	 * The Http Service will call the servlet's {@code init} method before
	 * returning.
	 * 
	 * <pre>
	 * httpService.registerServlet(&quot;/myservlet&quot;, servlet, initparams, context);
	 * </pre>
	 * 
	 * <p>
	 * Servlets registered with the same {@code HttpContext} object will share
	 * the same {@code ServletContext}. The Http Service will call the
	 * {@code context} argument to support the {@code ServletContext} methods
	 * {@code getResource},{@code getResourceAsStream} and {@code getMimeType},
	 * and to handle security for requests. If the {@code context} argument is
	 * {@code null}, a default {@code HttpContext} object is used (see
	 * {@link #createDefaultHttpContext()}).
	 * 
	 * @param alias name in the URI namespace at which the servlet is registered
	 * @param servlet the servlet object to register
	 * @param initparams initialization arguments for the servlet or
	 *        {@code null} if there are none. This argument is used by the
	 *        servlet's {@code ServletConfig} object.
	 * @param loadOnStartup ....
	 * @param asyncSupported tells weither this Servlet supports async requests (since Servlet API 3.0)
	 * @param context the {@code HttpContext} object for the registered servlet,
	 *        or {@code null} if a default {@code HttpContext} is to be created
	 *        and used.
	 * @throws NamespaceException if the registration fails because the alias is
	 *         already in use.
	 * @throws javax.servlet.ServletException if the servlet's {@code init}
	 *         method throws an exception, or the given servlet object has
	 *         already been registered at a different alias.
	 * @throws java.lang.IllegalArgumentException if any of the arguments are
	 *         invalid
	 */
	void registerServlet(String alias, Servlet servlet, @SuppressWarnings("rawtypes") Dictionary initParams,
			Integer loadOnStartup, Boolean asyncSupported,
			HttpContext httpContext) throws ServletException,
			NamespaceException;

    /**
     * Registers a servlet.
     *
     * @param servlet     a servlet. Cannot be null.
     * @param urlPatterns url patterns this servlet maps to
     * @param initParams  initialization arguments for the servlet or null if there are none. This argument is used by
     *                    the servlet's ServletConfig object.
     * @param httpContext the http context this servlet is for. If null a default http context will be used.
     *
     * @throws IllegalArgumentException if servlet is null, urlPattern is null or empty, or urlPattern is invalid
     * @throws ServletException         if servlet was already registered
     */
    void registerServlet( Servlet servlet,
                          String[] urlPatterns,
                          Dictionary<String,?> initParams,
                          HttpContext httpContext )
        throws ServletException;
    
    /**
     * Registers a servlet with enhanced support.
     *
     * @param servlet     a servlet. Cannot be null.
     * @param urlPatterns url patterns this servlet maps to
     * @param initParams  initialization arguments for the servlet or null if there are none. This argument is used by
     *                    the servlet's ServletConfig object.
     * @param loadOnStartup this is used by the Servlet Holder for configuration of how much instances should be loaded on startup.                   
     * @param asyncSupported this is new with Servlet 3.0 and tells wether this servlet supports this type of requests.                   
     * @param httpContext the http context this servlet is for. If null a default http context will be used.
     *
     * @throws IllegalArgumentException if servlet is null, urlPattern is null or empty, or urlPattern is invalid
     * @throws ServletException         if servlet was already registered
     */
    void registerServlet( Servlet servlet,
                          String[] urlPatterns,
                          Dictionary<String,?> initParams,
                          Integer loadOnStartup,
                          Boolean asyncSupported,
                          HttpContext httpContext )
        throws ServletException;

    /**
     * Registers a named servlet.<br/>
     * A named servlet can then be referenced by name while registering a filter.
     *
     * @param servlet     a servlet. Cannot be null.
     * @param servletName servlet name. If null, acts as for the registration method that does not take a servlet name
     * @param urlPatterns url patterns this servlet maps to
     * @param initParams  initialization arguments for the servlet or null if there are none. This argument is used by
     *                    the servlet's ServletConfig object.
     * @param httpContext the http context this servlet is for. If null a default http context will be used.
     *
     * @throws IllegalArgumentException if servlet is null, urlPattern is null or empty, or urlPattern is invalid
     * @throws ServletException         if servlet was already registered
     */
    void registerServlet( Servlet servlet,
                          String servletName,
                          String[] urlPatterns,
                          Dictionary<String,?> initParams,
                          HttpContext httpContext )
        throws ServletException;

    /**
     * Registers a named servlet.<br/>
     * A named servlet can then be referenced by name while registering a filter.
     *
     * @param servlet     a servlet. Cannot be null.
     * @param servletName servlet name. If null, acts as for the registration method that does not take a servlet name
     * @param urlPatterns url patterns this servlet maps to
     * @param initParams  initialization arguments for the servlet or null if there are none. This argument is used by
     *                    the servlet's ServletConfig object.
     * @param loadOnStartup this is used by the Servlet Holder for configuration of how much instances should be loaded on startup.                   
     * @param asyncSupported this is new with Servlet 3.0 and tells wether this servlet supports this type of requests.                   
     * @param httpContext the http context this servlet is for. If null a default http context will be used.
     *
     * @throws IllegalArgumentException if servlet is null, urlPattern is null or empty, or urlPattern is invalid
     * @throws ServletException         if servlet was already registered
     */
    void registerServlet( Servlet servlet,
                          String servletName,
                          String[] urlPatterns,
                          Dictionary<String,?> initParams,
                          Integer loadOnStartup,
                          Boolean asyncSupported,
                          HttpContext httpContext )
        throws ServletException;

    /**
     * Register a Servlet by a given Classname instead of an instance ... See PAXWEB-xxx
     * @param servletClass
     * @param urlPatterns
     * @param initParams
     * @param httpContext
     * @throws ServletException
     */
    void registerServlet(Class<? extends Servlet> servletClass,
	                     String[] urlPatterns, 
	                     Dictionary<String,?> initParams, 
	                     HttpContext httpContext)
	    throws ServletException;
    
    /**
     * Register a Servlet by a given Classname instead of an instance ... See PAXWEB-xxx
     * @param servletClass
     * @param urlPatterns
     * @param initParams
     * @param loadOnStartup this is used by the Servlet Holder for configuration of how much instances should be loaded on startup.                   
     * @param asyncSupported this is new with Servlet 3.0 and tells wether this servlet supports this type of requests.                   
     * @param httpContext
     * @throws ServletException
     */
    void registerServlet(Class<? extends Servlet> servletClass,
	                     String[] urlPatterns, 
	                     Dictionary<String,?> initParams,
	                     Integer loadOnStartup,
                         Boolean asyncSupported,
	                     HttpContext httpContext)
	    throws ServletException;

    /**
     * Unregisters a previously registered servlet.
     *
     * @param servlet the servlet to be unregistered
     *
     * @throws IllegalArgumentException if the servlet is null
     */
    void unregisterServlet( Servlet servlet );

    /**
     * Unregisters all previously registered servlet with the given class.
     *
     * @param servletClass the servlet class to be unregistered
     *
     * @throws IllegalArgumentException if the servlet class is null
     */
    void unregisterServlets( Class<? extends Servlet> servletClass );
	
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
     * Registers a servlet filter.
     *
     * @param filter       a servlet filter. If null an IllegalArgumentException is thrown.
     * @param urlPatterns  url patterns this filter maps to
     * @param servletNames servlet names this filter maps to
     * @param initparams   initialization arguments for the filter or null if there are none. This argument is used by
     *                     the filters FilterConfig object.
     * @param httpContext  the http context this filter is for. If null a default http context will be used.
     */
    void registerFilter( Filter filter,
                         String[] urlPatterns,
                         String[] servletNames,
                         Dictionary<String,?> initparams,
                         HttpContext httpContext );

    /**
     * Unregisters a previously registered servlet filter.
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
    void setContextParam( Dictionary<String,?> params, HttpContext httpContext );

    /**
     * Sets the session timeout of the servlet context corresponding to specified http context.
     * This method must be used before any register method that uses the specified http context, otherwise an
     * IllegalStateException will be thrown.
     *
     * @param minutes     session timeout of the servlet context corresponding to specified http context
     * @param httpContext http context. Cannot be null.
     *
     * @throws IllegalArgumentException if http context is null
     * @throws IllegalStateException    if the call is made after the http context was already used into a registration
     */
    void setSessionTimeout( Integer minutes, HttpContext httpContext );

    /**
     * Enable jsp support.
     *
     * @param urlPatterns an array of url patterns this jsp support maps to. If null, a default "*.jsp" will be used
     * @param httpContext the http context for which the jsp support should be enabled. If null a default http context
     *                    will be used.
     *
     * @throws UnsupportedOperationException if optional org.ops4j.pax.web.jsp package is not resolved
     * @since 0.3.0, January 07, 2007
     */
    void registerJsps( String[] urlPatterns, HttpContext httpContext );
    /**
     * Enable jsp support.
     *
     * @param urlPatterns an array of url patterns this jsp support maps to. If null, a default "*.jsp" will be used
     * @param initParams  initialization arguments or null if there are none.
     * @param httpContext the http context for which the jsp support should be enabled. If null a default http context
     *                    will be used.
     *
     * @throws UnsupportedOperationException if optional org.ops4j.pax.web.jsp package is not resolved
     * @since 2.0.0
     */
    void registerJsps( String[] urlPatterns, Dictionary<String,?> initParams, HttpContext httpContext);

    /**
     * Unregister jsps and disable jsp support.
     *
     * @param httpContext the http context for which the jsp support should be disabled
     *
     * @throws IllegalArgumentException      if http context is null or jsp support was not enabled for the http context
     * @throws UnsupportedOperationException if optional org.ops4j.pax.web.jsp package is not resolved
     * @since 0.3.0, January 07, 2007
     */
    void unregisterJsps( HttpContext httpContext );
    
    /**
     * Unregister jsps and disable jsp support.
     * 
     * @param urlPatterns an array of url patterns this jsp support maps to. If null, a default "*.jsp" will be used
     * @param httpContext the http context for which the jsp support should be disabled
     *
     * @throws IllegalArgumentException      if http context is null or jsp support was not enabled for the http context
     * @throws UnsupportedOperationException if optional org.ops4j.pax.web.jsp package is not resolved
     * @since 2.0.0
     */
    void unregisterJsps(String[] urlPatterns, HttpContext httpContext);

    /**
     * Registers an error page to customize the response sent back to the web client in case that an exception or error
     * propagates back to the web container, or the servlet/filter calls sendError() on the response object for a
     * specific status code.
     *
     * @param error       a fully qualified Exception class name or an error status code
     * @param location    the request path that will fill the response page. The location must start with an "/"
     * @param httpContext the http context this error page is for. If null a default http context will be used.
     *
     * @throws IllegalArgumentException if:
     *                                  error is null or empty
     *                                  location is null
     *                                  location does not start with a slash "/"
     * @since 0.3.0, January 12, 2007
     */
    void registerErrorPage( String error, String location, HttpContext httpContext );

    /**
     * Unregisters a previous registered error page.
     *
     * @param error       a fully qualified Exception class name or an error status code
     * @param httpContext the http context from which the error page should be unregistered. Cannot be null.
     *
     * @throws IllegalArgumentException if:
     *                                  error is null or empty
     *                                  error page was not registered before
     *                                  httpContext is null
     * @since 0.3.0, January 12, 2007
     */
    void unregisterErrorPage( String error, HttpContext httpContext );

    /**
     * Registers an ordered list of partial URIs. The purpose of this mechanism is to allow the deployer to specify an
     * ordered list of partial URIs for the container to use for appending to URIs when there is a request for a URI
     * that corresponds to a directory entry in the WAR not mapped to a Web component
     *
     * @param welcomeFiles an array of welcome files paths. Paths must not start or end with "/"
     * @param redirect     true if the client should be redirected to welcome file or false if forwarded
     * @param httpContext  the http context this error page is for. If null a default http context will be used.
     *
     * @throws IllegalArgumentException if:
     *                                  welcome files param is null or empty
     *                                  entries in array are null or empty
     *                                  entries in array start or end with "/"
     * @throws IllegalStateException    if welcome files are already registered
     * @since 0.3.0, January 16, 2007
     */
    void registerWelcomeFiles( String[] welcomeFiles, boolean redirect, HttpContext httpContext );

    /**
     * Unregisters previous registered welcome files.
     *
     * @param httpContext the http context from which the welcome files should be unregistered. Cannot be null.
     *
     * @throws IllegalArgumentException if httpContext is null
     * @since 0.3.0, January 16, 2007
     */
    void unregisterWelcomeFiles( HttpContext httpContext );
    
    /**
     * Registers login configuration, with authorization method and realm name.  
     * 
     * @param authMethod
     * @param realmName
     * @param formLoginPage
     * @param formErrorPage 
     * @param httpContext
     */
    void registerLoginConfig(String authMethod, String realmName, String formLoginPage, String formErrorPage, HttpContext httpContext);
    
    /**
     * Unregisters login configuration ....
     * @param httpContext 
     */
    void unregisterLoginConfig(HttpContext httpContext); 
    
    /**
     * Registers constraint mappings....
     * 
     * @param constraintName
     * @param mapping
     * @param url
     * @param dataConstraint
     * @param authentication
     * @param roles
     * @param httpContext
     */
    void registerConstraintMapping(String constraintName, String mapping, String url, String dataConstraint, boolean authentication, List<String> roles, HttpContext httpContext); 
    
    /**
     * Unregisters constraint mappings....
     * @param httpContext 
     */
    void unregisterConstraintMapping(HttpContext httpContext);

    
    /**
     * Register ServletContainerInitializer....
     * 
     * @param servletContainerInitializer
     * @param classes
     * @param httpContext 
     */
    void registerServletContainerInitializer(
    		ServletContainerInitializer servletContainerInitializer,
    		Class<?>[] classes, HttpContext httpContext);
    
    /**
     * @return the default shared http Context
     */
    SharedWebContainerContext getDefaultSharedHttpContext();

	/**
	 * Unregister method for {@link ServletContainerInitializer}s 
	 * 
	 * @param httpContext the http Context to unregister from
	 */
	void unregisterServletContainerInitializer(HttpContext httpContext);

	void registerJettyWebXml(URL jettyWebXmlURL, HttpContext httpContext);
	
	void setVirtualHosts(List<String> virtualHosts, HttpContext httpContext);
	
	void setConnectors(List<String> connectors, HttpContext httpContext);

	void registerJspServlet(String[] urlPatterns, HttpContext httpContext,
			String jspF);
	
	void registerJspServlet(String[] urlPatterns, Dictionary<String,?> dictionary, HttpContext httpContext,
			String jspF);
    /**
     * Start modifying the http context.
     * If this method is called, all changed to the given http context can
     * be bufferered until end() is called.
     * @param httpContext
     */
    void begin(HttpContext httpContext);

    /**
     * Validate changes on the given http context
     * @param httpContext
     */
    void end(HttpContext httpContext);

}
