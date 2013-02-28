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
package org.ops4j.pax.web.service.jetty.internal;

import java.io.File;
import java.net.URL;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.security.authentication.ClientCertAuthenticator;
import org.eclipse.jetty.security.authentication.DigestAuthenticator;
import org.eclipse.jetty.security.authentication.FormAuthenticator;
import org.eclipse.jetty.security.authentication.SpnegoAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HandlerContainer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.handler.ContextHandler.Context;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.session.AbstractSessionIdManager;
import org.eclipse.jetty.server.session.AbstractSessionManager;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.ops4j.pax.swissbox.core.BundleUtils;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.ops4j.pax.web.service.spi.ServletContextManager;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.Model;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jetty server with a handler collection specific to Pax Web.
 */
class JettyServerWrapper extends Server
{

	private static final Logger LOG = LoggerFactory.getLogger(JettyServerWrapper.class);

	private static final String WEB_CONTEXT_PATH = "Web-ContextPath";

	private static final class ServletContextInfo {
	    
	    private final ServletContextHandler handler;
	    private int refCount;
	    
        public ServletContextInfo(ServletContextHandler handler) {
            super();
            this.handler = handler;
            this.refCount = 1;
        }

        public int incrementRefCount() {
            return ++this.refCount;
        }

        public int decrementRefCount() {
            return --this.refCount;
        }

        public ServletContextHandler getHandler() {
            return handler;
        }
	}
	
    private final ServerModel m_serverModel;
    private final Map<HttpContext, ServletContextInfo> m_contexts = new IdentityHashMap<HttpContext, ServletContextInfo>();
    private Map<String, Object> m_contextAttributes;
    private Integer m_sessionTimeout;
    private String m_sessionCookie;
    private String m_sessionUrl;
    private String m_sessionWorkerName;
    private Boolean m_lazyLoad;
    private String m_storeDirectory;

	private File serverConfigDir;

    private URL serverConfigURL;

	private ServiceRegistration servletContextService;

	private Boolean m_sessionCookieHttpOnly;

    JettyServerWrapper( ServerModel serverModel )
    {
        m_serverModel = serverModel;
        setHandler( new JettyServerHandlerCollection( m_serverModel ) );
//        setHandler( new HandlerCollection(true) );
    }

    /**
     * {@inheritDoc}
     */
    public void configureContext( final Map<String, Object> attributes,
                                  final Integer sessionTimeout,
                                  final String sessionCookie,
                                  final String sessionUrl,
                                  final Boolean sessionCookieHttpOnly, 
                                  final String sessionWorkerName, 
                                  final Boolean lazyLoad, 
                                  final String storeDirectory)
    {
        m_contextAttributes = attributes;
        m_sessionTimeout = sessionTimeout;
        m_sessionCookie = sessionCookie;
        m_sessionUrl = sessionUrl;
        m_sessionCookieHttpOnly = sessionCookieHttpOnly;
        m_sessionWorkerName = sessionWorkerName;
        m_lazyLoad = lazyLoad;
        m_storeDirectory = storeDirectory;
    }

	ServletContextHandler getContext(final HttpContext httpContext) {
		ServletContextInfo servletContextInfo = m_contexts.get(httpContext);
		if (servletContextInfo != null) {
			return servletContextInfo.getHandler();
		}
		return null;
	}

    ServletContextHandler getOrCreateContext( final Model model )
    {
        return getOrCreateContext( model.getContextModel() );
    }

    ServletContextHandler getOrCreateContext( final ContextModel model )
    {
        final HttpContext httpContext = model.getHttpContext();
        
        ServletContextInfo context = m_contexts.get( httpContext );
        if( context == null )
        {
            LOG.debug("Creating new ServletContextHandler for HTTP context [{}] and model [{}]",httpContext,model);
            
            context = new ServletContextInfo(this.addContext( model ));
            m_contexts.put( httpContext, context );
        }
        else {
            
            int nref = context.incrementRefCount();

            if (LOG.isDebugEnabled()) {
                LOG.debug("ServletContextHandler for HTTP context [{}] and model [{}] referenced [{}] times.",
                        new Object[]{httpContext,model,nref});
            }
        }
        return context.getHandler();
    }

    void removeContext( final HttpContext httpContext )
    {
        ServletContextInfo context = m_contexts.get( httpContext );

        if (context == null)
        	return; //stop here context already gone ...
        
        int nref = context.decrementRefCount();
        

        if (nref <= 0) {

            LOG.debug("Removing ServletContextHandler for HTTP context [{}].",httpContext);

            m_contexts.remove( httpContext );

            try {
                if (servletContextService != null) //if null already unregistered!
                    servletContextService.unregister();
            } catch (IllegalStateException e) {
                LOG.info("ServletContext service already removed");
            }
            ((HandlerCollection) getHandler()).removeHandler( getContext( httpContext ) );
        }
        else {
            
            LOG.debug("ServletContextHandler for HTTP context [{}] referenced [{}] times.",
                    httpContext,nref);
        }
    }

    private ServletContextHandler addContext( final ContextModel model )
    {
        Bundle bundle = model.getBundle();
        BundleContext bundleContext = BundleUtils.getBundleContext(bundle);
		ServletContextHandler context = new HttpServiceContext(
												(HandlerContainer) getHandler(),
												model.getContextParams(),
                                                getContextAttributes(bundleContext),
                                                model.getContextName(),
                                                model.getHttpContext(),
                                                model.getAccessControllerContext(),
                                                model.getContainerInitializers(),
                                                model.getJettyWebXmlURL(),
                                                model.getVirtualHosts(),
                                                model.getConnectors()
        );
        context.setClassLoader( model.getClassLoader() );
        Integer sessionTimeout = model.getSessionTimeout();
        if( sessionTimeout == null )
        {
            sessionTimeout = m_sessionTimeout;
        }
        String sessionCookie = model.getSessionCookie();
        if( sessionCookie == null )
        {
            sessionCookie = m_sessionCookie;
        }
        String sessionUrl = model.getSessionUrl();
        if( sessionUrl == null )
        {
            sessionUrl = m_sessionUrl;
        }
        Boolean sessionCookieHttpOnly = model.getSessionCookieHttpOnly();
        if (sessionCookieHttpOnly == null) {
        	sessionCookieHttpOnly = m_sessionCookieHttpOnly;
        }        	
        String workerName = model.getSessionWorkerName();
        if( workerName == null )
        {
            workerName = m_sessionWorkerName;
        }
        configureSessionManager( context, sessionTimeout, sessionCookie, sessionUrl, sessionCookieHttpOnly, workerName, m_lazyLoad, m_storeDirectory );

        if (model.getRealmName() != null && model.getAuthMethod() != null)
        	configureSecurity(context, model.getRealmName(),
        							   model.getAuthMethod(),
        							   model.getFormLoginPage(),
        							   model.getFormErrorPage());

        LOG.debug( "Added servlet context: " + context );
        /*
         * Do not start context here, but register it to be started lazily. This
         * ensures that all servlets, listeners, initializers etc. are registered
         * before the context is started.
         */
        ServletContextManager.addContext(context.getContextPath(), new JettyServletContextWrapper(context));
        
        if( isStarted() )
        {
            try
            {
                LOG.debug( "(Re)starting servlet contexts..." );
                // start the server handler if not already started
                Handler serverHandler = getHandler();
                if( !serverHandler.isStarted() && !serverHandler.isStarting() )
                {
                    serverHandler.start();
                }
                // if the server handler is a handler collection, seems like jetty will not automatically
                // start inner handlers. So, force the start of the created context
                if( !context.isStarted() && !context.isStarting() )
                {
                    LOG.debug( "Registering ServletContext as service. ");
                    Dictionary<String, String> properties = new Hashtable<String, String>();
                    properties.put("osgi.web.symbolicname", bundle.getSymbolicName() );

                    Dictionary<?, ?> headers = bundle.getHeaders();
                    String version = (String) headers.get(Constants.BUNDLE_VERSION);
                    if (version != null && version.length() > 0)
                    	properties.put("osgi.web.version", version);

                    String webContextPath = (String) headers.get(WEB_CONTEXT_PATH);
                    String webappContext = (String) headers.get("Webapp-Context");

                    Context servletContext = context.getServletContext();

                    //This is the default context, but shouldn't it be called default? See PAXWEB-209
                    if ("/".equalsIgnoreCase(context.getContextPath()) && (webContextPath == null || webappContext == null))
                    	webContextPath = context.getContextPath();

                    //makes sure the servlet context contains a leading slash
                    webContextPath =  webContextPath != null ? webContextPath : webappContext;
                    if (webContextPath != null && !webContextPath.startsWith("/"))
                    	webContextPath = "/"+webContextPath;

                    if (webContextPath == null)
                    	LOG.warn("osgi.web.contextpath couldn't be set, it's not configured");

                    properties.put("osgi.web.contextpath", webContextPath );

                    servletContextService = bundleContext.registerService(
                            ServletContext.class.getName(),
                            servletContext,
                            properties
                        );
                    LOG.debug( "ServletContext registered as service. " );

                }
            }
            catch( Exception ignore )
            {
                LOG.error( "Could not start the servlet context for http context ["
                           + model.getHttpContext() + "]", ignore
                );
            }
        }
        return context;
    }

	/**
	 * Sets the security authentication method and the realm name on the security handler.
	 * This has to be done before the context is started.
	 *
	 * @param context
	 * @param realmName
	 * @param authMethod
	 * @param formLoginPage
	 * @param formErrorPage
	 */
	private void configureSecurity(ServletContextHandler context,
			String realmName, String authMethod, String formLoginPage, String formErrorPage) {
		final SecurityHandler securityHandler = context.getSecurityHandler();

		Authenticator authenticator = null;
		if (Constraint.__FORM_AUTH.equals(authMethod)) {
			authenticator = new FormAuthenticator();
			securityHandler.setInitParameter(FormAuthenticator.__FORM_LOGIN_PAGE,formLoginPage);
			securityHandler.setInitParameter(FormAuthenticator.__FORM_ERROR_PAGE,formErrorPage);
		} else if (Constraint.__BASIC_AUTH.equals(authMethod))
			authenticator = new BasicAuthenticator();
		else if (Constraint.__DIGEST_AUTH.equals(authMethod))
			authenticator = new DigestAuthenticator();
		else if (Constraint.__CERT_AUTH.equals(authMethod))
			authenticator = new ClientCertAuthenticator();
		else if (Constraint.__CERT_AUTH2.equals(authMethod))
			authenticator = new ClientCertAuthenticator();
                else if (Constraint.__SPNEGO_AUTH.equals(authMethod))
                    authenticator = new SpnegoAuthenticator();
		else
			LOG.warn("UNKNOWN AUTH METHOD: " + authMethod);

		securityHandler.setAuthenticator(authenticator);

		securityHandler.setRealmName(realmName);

	}

	/**
     * Returns a list of servlet context attributes out of configured properties and attribues containing the bundle
     * context associated with the bundle that created the model (web element).
     *
     * @param bundleContext bundle context to be set as attribute
     *
     * @return context attributes map
     */
    private Map<String, Object> getContextAttributes( final BundleContext bundleContext )
    {
        final Map<String, Object> attributes = new HashMap<String, Object>();
        if( m_contextAttributes != null )
        {
            attributes.putAll( m_contextAttributes );
        }
        attributes.put( WebContainerConstants.BUNDLE_CONTEXT_ATTRIBUTE, bundleContext );
        attributes.put( "org.springframework.osgi.web.org.osgi.framework.BundleContext", bundleContext );
        return attributes;
    }

    /**
     * Configures the session time out by extracting the session handlers->sessionManager for the context.
     *
     * @param context    the context for which the session timeout should be configured
     * @param minutes    timeout in minutes
     * @param cookie     Session cookie name. Defaults to JSESSIONID. If set to null or "none" no cookies
     *                   will be used.
     * @param url        session URL parameter name. Defaults to jsessionid. If set to null or "none" no URL
     *                   rewriting will be done.
     * @param sessionCookieHttpOnly configures if the Cookie is valid for http only (not https)
     * @param workerName name appended to session id, used to assist session affinity in a load balancer
     */
    private void configureSessionManager( final ServletContextHandler context,
                                          final Integer minutes,
                                          final String cookie,
                                          final String url,
                                          final Boolean cookieHttpOnly, 
                                          final String workerName, 
                                          final Boolean lazyLoad, 
                                          final String storeDirectory)
    {
        LOG.debug( "configureSessionManager for context [" + context + "] using - timeout:" + minutes
                   + ", cookie:" + cookie + ", url:" + url + ", cookieHttpOnly:" + cookieHttpOnly + ", workerName:" + workerName +", lazyLoad:" + lazyLoad+", storeDirectory: "+storeDirectory
        );

        final SessionHandler sessionHandler = context.getSessionHandler();
        if( sessionHandler != null )
        {
            final SessionManager sessionManager = sessionHandler.getSessionManager();
            if( sessionManager != null )
            {
                if( minutes != null )
                {
                    sessionManager.setMaxInactiveInterval( minutes * 60 );
                    LOG.debug( "Session timeout set to " + minutes + " minutes for context [" + context + "]" );
                }
                if( cookie == null || "none".equals( cookie ) )
                {
                  if (sessionManager instanceof AbstractSessionManager) {
                    ((AbstractSessionManager)sessionManager).setUsingCookies( false );
                    LOG.debug( "Session cookies disabled for context [" + context + "]" );
                  } else {
                    LOG.debug( "SessionManager isn't of type AbstractSessionManager therefore using cookies unchanged!");
                  }
                }
                else
                {
                	if (sessionManager instanceof AbstractSessionManager) {
                		((AbstractSessionManager)sessionManager).setSessionCookie( cookie );
                		LOG.debug( "Session cookie set to " + cookie + " for context [" + context + "]" );
                		
                		((AbstractSessionManager)sessionManager).setHttpOnly(cookieHttpOnly);
                		LOG.debug( "Session cookieHttpOnly set to " + cookieHttpOnly + " for context [" + context + "]" );
                	} else {
                		LOG.debug( "SessionManager isn't of type AbstractSessionManager therefore cookie not set!");
                	}
                }
                if( url != null )
                {
                    sessionManager.setSessionIdPathParameterName( url );
                    LOG.debug( "Session URL set to " + url + " for context [" + context + "]" );
                }
                if( workerName != null )
                {
                    SessionIdManager sessionIdManager = sessionManager.getSessionIdManager();
                    if( sessionIdManager == null )
                    {
                        sessionIdManager = new HashSessionIdManager();
                        sessionManager.setSessionIdManager( sessionIdManager );
                    }
                    if( sessionIdManager instanceof AbstractSessionIdManager )
                    {
                        AbstractSessionIdManager s = (AbstractSessionIdManager) sessionIdManager;
                        s.setWorkerName( workerName );
                        LOG.debug( "Worker name set to " + workerName + " for context [" + context + "]" );
                    }
                }
                //PAXWEB-461
                if( lazyLoad != null) {
            		if (sessionManager instanceof HashSessionManager) {
                        ((HashSessionManager)sessionManager).setLazyLoad(lazyLoad);
            		}
                }
                if (storeDirectory != null) {
            		if (sessionManager instanceof HashSessionManager) {
                        ((HashSessionManager)sessionManager).setStoreDirectory(new File(storeDirectory));
            		}
                }
            }
        }
    }

	/**
	 * @param serverConfigDir the serverConfigDir to set
	 */
	public void setServerConfigDir(File serverConfigDir) {
		this.serverConfigDir = serverConfigDir;
	}

	/**
	 * @return the serverConfigDir
	 */
	public File getServerConfigDir() {
		return serverConfigDir;
	}

    public URL getServerConfigURL() {
        return serverConfigURL;
    }

    public void setServerConfigURL(URL serverConfigURL) {
        this.serverConfigURL = serverConfigURL;
    }
}