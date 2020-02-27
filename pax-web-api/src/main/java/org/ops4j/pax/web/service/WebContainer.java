/*
 * Copyright 2007 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service;

import java.util.Dictionary;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.ops4j.pax.web.service.views.PaxWebContainerView;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/**
 * <p>This interface extends {@link HttpService} defined in OSGi CMPN R7, chapter 102 "Http Service
 * specification".</p>
 *
 * <p>In addition to basic registration methods from {@link HttpService}, here we can register all remaining
 * <em>web components</em> defined in Java Servlet Specification 4:<ul>
 *     <li>servlets (using more options)</li>
 *     <li>filters</li>
 *     <li>JSPs (as specialization of servlet)</li>
 *     <li>error pages</li>
 *     <li>welcome files</li>
 *     <li>login configurations</li>
 *     <li>security constraints</li>
 *     <li>servlet container initializers</li>
 *     <li>web sockets</li>
 * </ul></p>
 *
 * <p>All registration methods allow passing an instance of {@link HttpContext} from original Http Service
 * specification to indicate particular <em>context</em> ({@link javax.servlet.ServletContext} from Servlet API)
 * where given web component should be registered. This means that this <em>web container</em> represents
 * entire <em>Java HTTP/Servlet container</em> which organizes web elements (like servlets) in <em>contexts</em>
 * or simply <em>web applications</em>.</p>
 *
 * <p>Note that all "unregister" methods do not specify a context (this behavior is derived from original
 * {@link HttpContext}), so if this service was used to register two servlets under the same alias and for
 * two different contexts, {@link HttpService#unregister(String)} MUST unregister both registrations.</p>
 *
 * <p>Since Pax Web 8, methods different than {@code registerXXX()}/{@code unregisterXXX()} are moved to other
 * interfaces.</p>
 *
 * @author Alin Dreghiciu
 * @since 0.5.2
 */
public interface WebContainer extends HttpService {

	/**
	 * <p>Extension method to provide specialized <em>view</em> of a container to perform different tasks
	 * than registration of <em>web elements/components</em>.</p>
	 *
	 * <p>And even if that may sound weird, this method may be used internally by passing internal
	 * Pax Web interfaces. This way, pax-http-extender-whiteboard may use specialized registration methods
	 * for Whiteboard Service specific tasks (like batch registration of servlets to multiple contexts).</p>
	 *
	 * @param type another interface defined in pax-web-api for container manipulation/configuration.
	 * @param <T>
	 * @return
	 */
	default <T extends PaxWebContainerView> T adapt(Class<T> type) {
		return null;
	}

	// --- transactional access to web container

	/**
	 * <p>Begins a <em>transaction</em> to group multiple registration calls for given {@link HttpContext}.</p>
	 *
	 * <p>No operation related to given <em>transaction</em> will be effective until {@link #end(HttpContext)}
	 * is called. The important thing is that even if some initial operations are added to transaction (like
	 * registration of servlet with alias "/alias"), the validation will be performed only at the end of
	 * <em>transaction</em>, so another registration of servlet for "/alias" may succeed and this transaction will
	 * eventually fail even if the "/alias" registration in given transaction was performed earlier.</p>
	 *
	 * <p>After this call, nothing will be passed to actual server implementation until {@link #end(HttpContext)}
	 * is called. There's no thread affinity for such <em>transaction</em>, so given {@link HttpContext} can
	 * be passed around between threads without problems. However, there's no timeout, so not finished
	 * transactions will hang until the bundle is stopped.</p>
	 *
	 * <p>Technically, given {@link HttpContext} is marked as participating in transaction and all registrations
	 * are done in single model of a context. {@link #end(HttpContext)} passes all registered elements
	 * and context parameters (like session configuration) to actual server, ending the transaction. The benefit
	 * of such approach is that we don't have to take care about the order of operations because when transaction
	 * is {@link #end(HttpContext) ended}, some ordering may be performed.</p>
	 *
	 * <p>Even using {@code null} as {@link HttpContext} will work, because the default (scoped to given bundle)
	 * context will be marked as participating in a <em>transaction</em>.</p>
	 *
	 * @param context
	 * @return
	 */
	void begin(HttpContext context);

	/**
	 * <p>Ends a <em>transaction</em> related to given {@link HttpContext} (or default context if {@code null}
	 * is used). This method triggers invocation of all delayed operations.</p>
	 *
	 * @param context
	 */
	void end(HttpContext context);

	// --- different methods used to retrieve HttpContext

	/**
	 * <p>Creates a default {@link HttpContext} as defined in original {@link HttpService#createDefaultHttpContext()},
	 * but allowing to specify a name. <em>Default</em> means <em>default behaviour</em> (security, resource access)
	 * and not the fact that it's <em>global</em> (or <em>shared</em>) context.</p>
	 *
	 * <p>This allows single bundle (working on {@link org.osgi.framework.Constants#SCOPE_BUNDLE bundle-scoped}
	 * {@link HttpService}) to register web elements into same context without passing {@link HttpContext} around.</p>
	 *
	 * <p>Of course such {@link HttpContext} can later be registered as OSGi service and referenced later using:<ul>
	 *     <li>standard (Whiteboard)
	 *     {@code osgi.http.whiteboard.context.select=(osgi.http.whiteboard.context.name=name)} service registration
	 *     property even if this property is designed to reference {@link ServletContextHelper} instances</li>
	 *     <li>legacy (Pax Web specific) {@code httpContext.id=name} service registration property</li>
	 * </ul>
	 * Legacy Pax Web Whiteboard implementation handles contexts registered with {@code httpContext.id} property.
	 * User can register a {@link HttpContext} with such property and then register a servlet (or filter, or ...)
	 * with the same property to associate it with given context.</p>
	 *
	 * <p>{@link HttpContext} retrieved this way can't be used between bundles and is <strong>not</strong>
	 * registered automatically as OSGi service.</p>
	 *
	 * <p>In OSGi CMPN Whiteboard implementation there's no special API to create instances of
	 * {@link org.osgi.service.http.context.ServletContextHelper} instances.</p>
	 *
	 * @param contextId the ID of the context which is used while registering the {@link HttpContext} as service.
	 * @return {@link HttpContext}
	 */
	HttpContext createDefaultHttpContext(String contextId);

	/**
	 * <p>Creates a default implementation of a {@link MultiBundleWebContainerContext} with default behavior and
	 * {@code shared} name. Each call creates new instance and may be registered as OSGi service and referenced
	 * later by different bundles.</p>
	 *
	 * @return {@link MultiBundleWebContainerContext}
	 */
	MultiBundleWebContainerContext createDefaultSharedHttpContext();

	/**
	 * Creates a default implementation of a {@link MultiBundleWebContainerContext} with default behavior and given
	 * name.
	 *
	 * @param contextId
	 * @return
	 */
	MultiBundleWebContainerContext createDefaultSharedHttpContext(String contextId);

	/**
	 * Retrieves global (singleton), shared, default {@link HttpContext} which provides default behavior and may
	 * be shared by different bundles. This method is deprecated, because shared contexts should be created on demand,
	 * registered as OSGi services (possibly wrapped in {@link org.ops4j.pax.web.service.whiteboard.HttpContextMapping}
	 * together with context path) and referenced by all bundles wanting to contribute web elements to shared context.
	 *
	 * @return the default, <em>global</em> shared {@link HttpContext}
	 *
	 * @deprecated applications should not rely on singleton {@link MultiBundleWebContainerContext}, they should (if
	 *             not using Whiteboard approach) create named, shared context and probably register it as OSGi service
	 *             to be used by different bundles.
	 */
	@Deprecated
	MultiBundleWebContainerContext getDefaultSharedHttpContext();

	/**
	 * When using {@link HttpService} or {@link WebContainer} registration methods, a {@link HttpContext} instances
	 * has to be passed with every call (unless {@code null} used meaning default context). Knowing that some named
	 * {@link HttpContext} (whether shared or not) was created earlier, we can simply create a <em>referenced</em>
	 * context to make things more convenient.
	 *
	 * @param contextId
	 * @return
	 */
	ReferencedWebContainerContext createReferencedContext(String contextId);

	// --- methods used to register a Servlet - with more options than in original HttpService.registerServlet()

	/**
	 * <p>Registers a servlet as in {@link HttpService#registerServlet} with two additional parameters:<ul>
	 *     <li>load on startup ({@code <servlet>/<load-on-startup>} element from {@code web.xml})</li>
	 *     <li>async supported ({@code <servlet>/<async-supported>} element from {@code web.xml})</li>
	 * </ul></p>
	 *
	 * @param alias name in the URI namespace at which the servlet is registered (single, exact URI mapping)
	 * @param servlet the servlet object to register
	 * @param initParams initialization arguments for the servlet or {@code null} if there are none.
	 * @param loadOnStartup
	 * @param asyncSupported
	 * @param httpContext {@link HttpContext} to use for registered servlet. If {@code null}, default will be created.
	 * @throws NamespaceException if the registration fails because the alias is already in use.
	 * @throws ServletException
	 */
	void registerServlet(String alias, Servlet servlet, Dictionary<?, ?> initParams,
			Integer loadOnStartup, Boolean asyncSupported, HttpContext httpContext)
			throws ServletException, NamespaceException;

	/**
	 * <p>Registers a servlet as in {@link HttpService#registerServlet} but with servlet URL mappings
	 * (see Servlet API specification, chapter 12.2, "Specification of Mappings") instead of single <em>alias</em>.</p>
	 *
	 * @param servlet the servlet object to register
	 * @param urlPatterns url patterns for servlet mapping
	 * @param initParams initialization arguments for the servlet or {@code null} if there are none.
	 * @param httpContext {@link HttpContext} to use for registered servlet. If {@code null}, default will be created.
	 * @throws ServletException
	 */
	void registerServlet(Servlet servlet, String[] urlPatterns, Dictionary<String, String> initParams,
			HttpContext httpContext) throws ServletException;

	/**
	 * Registers a servlet with servlet URL mappings, load-on-startup and async-support parameters.
	 *
	 * @param servlet the servlet object to register
	 * @param urlPatterns url patterns for servlet mapping
	 * @param initParams initialization arguments for the servlet or {@code null} if there are none.
	 * @param loadOnStartup
	 * @param asyncSupported
	 * @param httpContext {@link HttpContext} to use for registered servlet. If {@code null}, default will be created.
	 * @throws ServletException
	 */
	void registerServlet(Servlet servlet, String[] urlPatterns, Dictionary<String, String> initParams,
			Integer loadOnStartup, Boolean asyncSupported, HttpContext httpContext) throws ServletException;

	/**
	 * <p>Registers a servlet as in {@link HttpService#registerServlet} but with servlet name and URL mappings
	 * (see Servlet API specification, chapter 12.2, "Specification of Mappings") instead of single <em>alias</em>.</p>
	 *
	 * @param servlet the servlet object to register
	 * @param servletName name of the servlet. If not specified, fully qualified name of servlet class will be used
	 * @param urlPatterns url patterns for servlet mapping
	 * @param initParams initialization arguments for the servlet or {@code null} if there are none.
	 * @param httpContext {@link HttpContext} to use for registered servlet. If {@code null}, default will be created.
	 * @throws ServletException
	 */
	void registerServlet(Servlet servlet, String servletName, String[] urlPatterns,
			Dictionary<String, String> initParams,
			HttpContext httpContext) throws ServletException;

	/**
	 * Registers a servlet with servlet name, URL mappings, load-on-startup and async-support parameters.
	 *
	 * @param servlet the servlet object to register
	 * @param servletName name of the servlet. If not specified, fully qualified name of servlet class will be used
	 * @param urlPatterns url patterns for servlet mapping
	 * @param initParams initialization arguments for the servlet or {@code null} if there are none.
	 * @param loadOnStartup
	 * @param asyncSupported
	 * @param httpContext {@link HttpContext} to use for registered servlet. If {@code null}, default will be created.
	 * @throws ServletException
	 */
	void registerServlet(Servlet servlet, String servletName, String[] urlPatterns,
			Dictionary<String, String> initParams,
			Integer loadOnStartup, Boolean asyncSupported, HttpContext httpContext) throws ServletException;

	/**
	 * Registers a servlet with servlet name, URL mappings, load-on-startup, async-support parameters and
	 * {@link MultipartConfigElement multipart configuration}.
	 *
	 * @param servlet the servlet object to register
	 * @param servletName name of the servlet. If not specified, fully qualified name of servlet class will be used
	 * @param urlPatterns url patterns for servlet mapping
	 * @param initParams initialization arguments for the servlet or {@code null} if there are none.
	 * @param loadOnStartup
	 * @param asyncSupported
	 * @param multiPartConfig
	 * @param httpContext {@link HttpContext} to use for registered servlet. If {@code null}, default will be created.
	 * @throws ServletException
	 */
	void registerServlet(Servlet servlet, String servletName, String[] urlPatterns,
			Dictionary<String, String> initParams,
			Integer loadOnStartup, Boolean asyncSupported, MultipartConfigElement multiPartConfig,
			HttpContext httpContext) throws ServletException;

	/**
	 * Register a servlet using class instead of an instance and with URL mappings instead of alias.
	 *
	 * @param servletClass the servlet class to instantiate and register
	 * @param urlPatterns url patterns for servlet mapping
	 * @param initParams initialization arguments for the servlet or {@code null} if there are none.
	 * @param httpContext {@link HttpContext} to use for registered servlet. If {@code null}, default will be created.
	 * @throws ServletException
	 */
	void registerServlet(Class<? extends Servlet> servletClass, String[] urlPatterns,
			Dictionary<String, String> initParams, HttpContext httpContext) throws ServletException;

	/**
	 * Register a servlet using class instead of an instance and with URL mappings instead of alias and with
	 * load-on-startup and async-support parameters.
	 *
	 * @param servletClass the servlet class to instantiate and register
	 * @param urlPatterns url patterns for servlet mapping
	 * @param initParams initialization arguments for the servlet or {@code null} if there are none.
	 * @param loadOnStartup
	 * @param asyncSupported
	 * @param httpContext {@link HttpContext} to use for registered servlet. If {@code null}, default will be created.
	 * @throws ServletException
	 */
	void registerServlet(Class<? extends Servlet> servletClass, String[] urlPatterns,
			Dictionary<String, String> initParams,
			Integer loadOnStartup, Boolean asyncSupported, HttpContext httpContext) throws ServletException;

	/**
	 * Register a servlet using class instead of an instance and with URL mappings instead of alias and with
	 * load-on-startup, async-support parameters and {@link MultipartConfigElement multipart configuration}.
	 *
	 * @param servletClass the servlet class to instantiate and register
	 * @param urlPatterns url patterns for servlet mapping
	 * @param initParams initialization arguments for the servlet or {@code null} if there are none.
	 * @param loadOnStartup
	 * @param asyncSupported
	 * @param multiPartConfig
	 * @param httpContext {@link HttpContext} to use for registered servlet. If {@code null}, default will be created.
	 * @throws ServletException
	 */
	void registerServlet(Class<? extends Servlet> servletClass, String[] urlPatterns,
			Dictionary<String, String> initParams,
			Integer loadOnStartup, Boolean asyncSupported, MultipartConfigElement multiPartConfig,
			HttpContext httpContext) throws ServletException;

	// --- methods used to unregister a Servlet

	/**
	 * Unregisters a previously registered servlet. If the same instance was registered in more contexts, it'll be
	 * removed from all of them.
	 *
	 * @param servlet the servlet to be unregistered
	 */
	void unregisterServlet(Servlet servlet);

	/**
	 * Unregister a previously registered servlet by its name. If more servlets were registered using the same name,
	 * all off them will be unregistered.
	 *
	 * @param servletName the servlet identified by it's name.
	 */
	void unregisterServlet(String servletName);

	/**
	 * Unregisters all previously registered servlet with the given class. If more servlets were registered with
	 * the same class, all of them will be unregistered.
	 *
	 * @param servletClass the servlet class to be unregistered
	 */
	void unregisterServlets(Class<? extends Servlet> servletClass);






//	/**
//	 * Registers an event listener. Depending on the listener type, the listener
//	 * will be notified on different life cycle events. The following listeners
//	 * are supported:
//	 * HttpSessionActivationListener, HttpSessionAttributeListener,
//	 * HttpSessionBindingListener, HttpSessionListener, ServletContextListener,
//	 * ServletContextAttributeListener, ServletRequestListener,
//	 * ServletRequestAttributeListener. Check out Servlet specification for
//	 * details on what type of event the registered listener will be notified.
//	 *
//	 * @param listener    an event listener to be registered. If null an
//	 *                    IllegalArgumentException is thrown.
//	 * @param httpContext the http context this listener is for. If null a default http
//	 *                    context will be used.
//	 */
//	void registerEventListener(EventListener listener, HttpContext httpContext);
//
//	/**
//	 * Unregisters a previously registered listener.
//	 *
//	 * @param listener the event listener to be unregistered.
//	 * @throws IllegalArgumentException if the listener is unknown to the http service (never
//	 *                                  registered or unregistered before) or the listener is null
//	 */
//	void unregisterEventListener(EventListener listener);
//
//	/**
//	 * Registers a servlet filter.
//	 *
//	 * @param filter       a servlet filter. If null an IllegalArgumentException is
//	 *                     thrown.
//	 * @param urlPatterns  url patterns this filter maps to
//	 * @param servletNames servlet names this filter maps to
//	 * @param initparams   initialization arguments for the filter or null if there are
//	 *                     none. This argument is used by the filters FilterConfig
//	 *                     object.
//	 * @param httpContext  the http context this filter is for. If null a default http
//	 *                     context will be used.
//	 */
//	void registerFilter(Filter filter, String[] urlPatterns,
//						String[] servletNames, Dictionary<String, ?> initparams, HttpContext httpContext);
//
//
//	void registerFilter(Filter filter, String[] urlPatterns, String[] servletNames,
//	Dictionary<String, String> initParams,
//						Boolean asyncSupported, HttpContext httpContext);
//
//	/**
//	 * Registers a servlet filter.
//	 *
//	 * @param filterClass  a servlet filter class. If null an IllegalArgumentException is
//	 *                     thrown.
//	 * @param urlPatterns  url patterns this filter maps to
//	 * @param servletNames servlet names this filter maps to
//	 * @param initParams   initialization arguments for the filter or null if there are
//	 *                     none. This argument is used by the filters FilterConfig
//	 *                     object.
//	 * @param httpContext  the http context this filter is for. If null a default http
//	 *                     context will be used.
//	 */
//	void registerFilter(Class<? extends Filter> filterClass,
//						String[] urlPatterns, String[] servletNames,
//						Dictionary<String, String> initParams, HttpContext httpContext);
//
//	/**
//	 * Registers a servlet filter.
//	 *
//	 * @param filterClass    a servlet filter class. If null an IllegalArgumentException is
//	 *                       thrown.
//	 * @param urlPatterns    url patterns this filter maps to
//	 * @param servletNames   servlet names this filter maps to
//	 * @param initParams     initialization arguments for the filter or null if there are
//	 *                       none. This argument is used by the filters FilterConfig
//	 *                       object.
//	 * @param asyncSupported flag if the filter supports async calls.
//	 * @param httpContext    the http context this filter is for. If null a default http
//	 *                       context will be used.
//	 */
//	void registerFilter(Class<? extends Filter> filterClass, String[] urlPatterns, String[] servletNames,
//						Dictionary<String, String> initParams, boolean asyncSupported, HttpContext httpContext);
//
//	/**
//	 * Unregisters a previously registered servlet filter.
//	 *
//	 * @param filter the servlet filter to be unregistered
//	 * @throws IllegalArgumentException if the filter is unknown to the http service (never
//	 *                                  registered or unregistered before) or the filter is null
//	 */
//	void unregisterFilter(Filter filter);
//
//	/**
//	 * Unregisters a previously registered servlet filter.
//	 *
//	 * @param filterClass the servlet filter to be unregistered, found by the Filter class
//	 * @throws IllegalArgumentException if the filter is unknown to the http service (never
//	 *                                  registered or unregistered before) or the filter is null
//	 */
//	void unregisterFilter(Class<? extends Filter> filterClass);
//
//	/**
//	 * Unregisters a previously registered servlet filter.
//	 *
//	 * @param filterName the servlet filter name to be unregistered
//	 * @throws IllegalArgumentException if the filter is unknown to the http service (never
//	 *                                  registered or unregistered before) or the filter is null
//	 */
//	void unregisterFilter(String filterName);
//
//	/**
//	 * Sets context paramaters to be used in the servlet context corresponding
//	 * to specified http context. This method must be used before any register
//	 * method that uses the specified http context, otherwise an
//	 * IllegalStateException will be thrown.
//	 *
//	 * @param params      context parameters for the servlet context corresponding to
//	 *                    specified http context
//	 * @param httpContext http context. Cannot be null.
//	 * @throws IllegalArgumentException if http context is null
//	 * @throws IllegalStateException    if the call is made after the http context was already used
//	 *                                  into a registration
//	 */
//	void setContextParam(Dictionary<String, ?> params, HttpContext httpContext);
//
//	/**
//	 * Sets the session timeout of the servlet context corresponding to
//	 * specified http context. This method must be used before any register
//	 * method that uses the specified http context, otherwise an
//	 * IllegalStateException will be thrown.
//	 *
//	 * @param minutes     session timeout of the servlet context corresponding to
//	 *                    specified http context
//	 * @param httpContext http context. Cannot be null.
//	 * @throws IllegalArgumentException if http context is null
//	 * @throws IllegalStateException    if the call is made after the http context was already used
//	 *                                  into a registration
//	 */
//	void setSessionTimeout(Integer minutes, HttpContext httpContext);
//
//	/**
//	 * Enable jsp support.
//	 *
//	 * @param urlPatterns an array of url patterns this jsp support maps to. If null, a
//	 *                    default "*.jsp" will be used
//	 * @param httpContext the http context for which the jsp support should be enabled.
//	 *                    If null a default http context will be used.
//	 * @throws UnsupportedOperationException if optional org.ops4j.pax.web.jsp package is not resolved
//	 * @since 0.3.0, January 07, 2007
//	 */
//	void registerJsps(String[] urlPatterns, HttpContext httpContext);
//
//	/**
//	 * Enable jsp support.
//	 *
//	 * @param urlPatterns an array of url patterns this jsp support maps to. If null, a
//	 *                    default "*.jsp" will be used
//	 * @param initParams  initialization arguments or null if there are none.
//	 * @param httpContext the http context for which the jsp support should be enabled.
//	 *                    If null a default http context will be used.
//	 * @throws UnsupportedOperationException if optional org.ops4j.pax.web.jsp package is not resolved
//	 * @since 2.0.0
//	 */
//	void registerJsps(String[] urlPatterns, Dictionary<String, ?> initParams,
//					  HttpContext httpContext);
//
//	/**
//	 * Unregister jsps and disable jsp support.
//	 *
//	 * @param httpContext the http context for which the jsp support should be disabled
//	 * @throws IllegalArgumentException      if http context is null or jsp support was not enabled for
//	 *                                       the http context
//	 * @throws UnsupportedOperationException if optional org.ops4j.pax.web.jsp package is not resolved
//	 * @since 0.3.0, January 07, 2007
//	 */
//	void unregisterJsps(HttpContext httpContext);
//
//	/**
//	 * Unregister jsps and disable jsp support.
//	 *
//	 * @param urlPatterns an array of url patterns this jsp support maps to. If null, a
//	 *                    default "*.jsp" will be used
//	 * @param httpContext the http context for which the jsp support should be disabled
//	 * @throws IllegalArgumentException      if http context is null or jsp support was not enabled for
//	 *                                       the http context
//	 * @throws UnsupportedOperationException if optional org.ops4j.pax.web.jsp package is not resolved
//	 * @since 2.0.0
//	 */
//	void unregisterJsps(String[] urlPatterns, HttpContext httpContext);
//
//	/**
//	 * Registers an error page to customize the response sent back to the web
//	 * client in case that an exception or error propagates back to the web
//	 * container, or the servlet/filter calls sendError() on the response object
//	 * for a specific status code.
//	 *
//	 * @param error       a fully qualified Exception class name or an error status code
//	 * @param location    the request path that will fill the response page. The
//	 *                    location must start with an "/"
//	 * @param httpContext the http context this error page is for. If null a default
//	 *                    http context will be used.
//	 * @throws IllegalArgumentException if: error is null or empty location is null location does not
//	 *                                  start with a slash "/"
//	 * @since 0.3.0, January 12, 2007
//	 */
//	void registerErrorPage(String error, String location,
//						   HttpContext httpContext);
//
//	/**
//	 * Unregisters a previous registered error page.
//	 *
//	 * @param error       a fully qualified Exception class name or an error status code
//	 * @param httpContext the http context from which the error page should be
//	 *                    unregistered. Cannot be null.
//	 * @throws IllegalArgumentException if: error is null or empty error page was not registered
//	 *                                  before httpContext is null
//	 * @since 0.3.0, January 12, 2007
//	 */
//	void unregisterErrorPage(String error, HttpContext httpContext);
//
//	/**
//	 * Registers an ordered list of partial URIs. The purpose of this mechanism
//	 * is to allow the deployer to specify an ordered list of partial URIs for
//	 * the container to use for appending to URIs when there is a request for a
//	 * URI that corresponds to a directory entry in the WAR not mapped to a Web
//	 * component
//	 *
//	 * @param welcomeFiles an array of welcome files paths. Paths must not start or end
//	 *                     with "/"
//	 * @param redirect     true if the client should be redirected to welcome file or
//	 *                     false if forwarded
//	 * @param httpContext  the http context this error page is for. If null a default
//	 *                     http context will be used.
//	 * @throws IllegalArgumentException if: welcome files param is null or empty entries in array are
//	 *                                  null or empty entries in array start or end with "/"
//	 * @throws IllegalStateException    if welcome files are already registered
//	 * @since 0.3.0, January 16, 2007
//	 */
//	void registerWelcomeFiles(String[] welcomeFiles, boolean redirect,
//							  HttpContext httpContext);
//
//	/**
//	 * Unregisters previous registered welcome files.
//	 *
//	 * @param httpContext the http context from which the welcome files should be
//	 *                    unregistered. Cannot be null.
//	 * @throws IllegalArgumentException if httpContext is null
//	 * @since 0.3.0, January 16, 2007
//	 */
//
//	void unregisterWelcomeFiles(String[] welcomeFiles, HttpContext httpContext);
//
//	/**
//	 * Registers login configuration, with authorization method and realm name.
//	 *
//	 * @param authMethod
//	 * @param realmName
//	 * @param formLoginPage
//	 * @param formErrorPage
//	 * @param httpContext
//	 */
//	void registerLoginConfig(String authMethod, String realmName,
//							 String formLoginPage, String formErrorPage, HttpContext httpContext);
//
//	/**
//	 * Unregisters login configuration ....
//	 *
//	 * @param httpContext
//	 */
//	void unregisterLoginConfig(HttpContext httpContext);
//
//	/**
//	 * Registers constraint mappings....
//	 *
//	 * @param constraintName
//	 * @param mapping
//	 * @param url
//	 * @param dataConstraint
//	 * @param authentication
//	 * @param roles
//	 * @param httpContext
//	 */
//	void registerConstraintMapping(String constraintName, String mapping,
//								   String url, String dataConstraint, boolean authentication,
//								   List<String> roles, HttpContext httpContext);
//
//	/**
//	 * Unregisters constraint mappings....
//	 *
//	 * @param httpContext
//	 */
//	void unregisterConstraintMapping(HttpContext httpContext);
//
//	/**
//	 * Register ServletContainerInitializer....
//	 *
//	 * @param servletContainerInitializer
//	 * @param classes
//	 * @param httpContext
//	 */
//	void registerServletContainerInitializer(
//			ServletContainerInitializer servletContainerInitializer,
//			Class<?>[] classes, HttpContext httpContext);
//
//
//	/**
//	 * Unregister method for {@link ServletContainerInitializer}s
//	 *
//	 * @param httpContext the http Context to unregister from
//	 */
//	void unregisterServletContainerInitializer(HttpContext httpContext);
//
//
//
//	void registerJspServlet(String[] urlPatterns, HttpContext httpContext,
//							String jspF);
//
//	void registerJspServlet(String[] urlPatterns,
//							Dictionary<String, ?> dictionary, HttpContext httpContext,
//							String jspF);

//
//	void registerJspConfigTagLibs(String tagLibLocation, String tagLibUri, HttpContext httpContext);
//
//	void registerJspConfigPropertyGroup(List<String> includeCodes,
//										List<String> includePreludes, List<String> urlPatterns, Boolean elIgnored,
//										Boolean scriptingInvalid,
//										Boolean isXml, HttpContext httpContext);
//
//	void registerWebSocket(Object webSocket, HttpContext httpContext);
//
//	void unregisterWebSocket(Object webSocket, HttpContext httpContext);
//
////	RequestInfoDTO calculateRequestInfoDTO(String path, Iterator<WhiteboardElement> iterator);
////
////	RuntimeDTO createWhiteboardRuntimeDTO(Iterator<WhiteboardElement> iterator);
//
////	WebContainerDTO getWebcontainerDTO();
//
//	void setSessionCookieConfig(String domain, String name, Boolean httpOnly, Boolean secure, String path,
//	Integer maxAge, HttpContext httpContext);

}
