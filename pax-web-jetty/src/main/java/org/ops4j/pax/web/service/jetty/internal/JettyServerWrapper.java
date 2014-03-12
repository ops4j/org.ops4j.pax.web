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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.ops4j.pax.swissbox.core.BundleUtils;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.Model;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;

/**
 * Jetty server with a handler collection specific to Pax Web.
 */
class JettyServerWrapper extends Server
{

	private static final Log LOG = LogFactory.getLog( JettyServerWrapper.class );
	
	private static final String WEB_CONTEXT_PATH = "Web-ContextPath";

    private final ServerModel m_serverModel;
    private final Map<HttpContext, HttpServiceContext> m_contexts;
    private Map<String, Object> m_contextAttributes;
    private Integer m_sessionTimeout;
    private String m_sessionCookie;
    private String m_sessionDomain;
    private String m_sessionUrl;
    private String m_sessionWorkerName;

	private File serverConfigDir;

    private URL serverConfigURL;

	private ServiceRegistration servletContextService;

    JettyServerWrapper( ServerModel serverModel )
    {
        this.m_serverModel = serverModel;
        this.m_contexts = new IdentityHashMap<HttpContext, HttpServiceContext>();
        setHandler( new JettyServerHandlerCollection( this.m_serverModel ) );
    }

    /**
     * {@inheritDoc}
     */
    public void configureContext( final Map<String, Object> attributes,
                                  final Integer sessionTimeout,
                                  final String sessionCookie,
                                  final String sessionDomain,
                                  final String sessionUrl,
                                  final String sessionWorkerName)
    {
        this.m_contextAttributes = attributes;
        this.m_sessionTimeout = sessionTimeout;
        this.m_sessionCookie = sessionCookie;
        this.m_sessionDomain = sessionDomain;
        this.m_sessionUrl = sessionUrl;
        this.m_sessionWorkerName = sessionWorkerName;
    }

    HttpServiceContext getContext( final HttpContext httpContext )
    {
        return this.m_contexts.get( httpContext );
    }

    HttpServiceContext getOrCreateContext( final Model model )
    {
        return getOrCreateContext( model.getContextModel() );
    }

    HttpServiceContext getOrCreateContext( final ContextModel model )
    {
        HttpServiceContext context = this.m_contexts.get( model.getHttpContext() );
        if( context == null )
        {
            context = addContext( model );
            this.m_contexts.put( model.getHttpContext(), context );
        }
        return context;
    }

    void removeContext( final HttpContext httpContext )
    {
        HttpServiceContext sch = getContext( httpContext );
        if (sch != null) {
            sch.unregisterService();
            try {
                sch.stop();
            } catch (Throwable t) {
                // Ignore
            }
            sch.getServletHandler().setServer(null);
            sch.getSecurityHandler().setServer(null);
            sch.getSessionHandler().setServer(null);
            sch.getErrorHandler().setServer(null);
            ((HandlerCollection) getHandler()).removeHandler( sch );
            sch.destroy();
        }
        this.m_contexts.remove( httpContext );
    }

    private HttpServiceContext addContext( final ContextModel model )
    { 
        Bundle bundle = model.getBundle();
        BundleContext bundleContext = BundleUtils.getBundleContext(bundle);
        HttpServiceContext context = new HttpServiceContext( (HandlerContainer) getHandler(), model.getContextParams(),
                                                  getContextAttributes(
                                                      bundleContext
                                                  ), model
                .getContextName(), model.getHttpContext(), model
                .getAccessControllerContext()
        );
        context.setClassLoader( model.getClassLoader() );
        Integer sessionTimeout = model.getSessionTimeout();
        if( sessionTimeout == null )
        {
            sessionTimeout = this.m_sessionTimeout;
        }
        String sessionCookie = model.getSessionCookie();
        if( sessionCookie == null )
        {
            sessionCookie = this.m_sessionCookie;
        }
        String sessionDomain = model.getSessionDomain();
        if( sessionDomain == null )
        {
            sessionDomain = this.m_sessionDomain;
        }
        String sessionUrl = model.getSessionUrl();
        if( sessionUrl == null )
        {
            sessionUrl = this.m_sessionUrl;
        }
        String workerName = model.getSessionWorkerName();
        if( workerName == null )
        {
            workerName = this.m_sessionWorkerName;
        }
        configureSessionManager( context, sessionTimeout, sessionCookie, sessionDomain, sessionUrl, workerName );
        
        if (model.getRealmName() != null && model.getAuthMethod() != null) {
          configureSecurity(context, model.getRealmName(),
        							   model.getAuthMethod(),
        							   model.getFormLoginPage(),
        							   model.getFormErrorPage());
        }
        
        LOG.debug( "Added servlet context: " + context );
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
                    context.start();
                    
                    LOG.debug( "Registering ServletContext as service. ");
                    Dictionary<String, String> properties = new Hashtable<String, String>();
                    properties.put("osgi.web.symbolicname", bundle.getSymbolicName() );
                    
                    Dictionary headers = bundle.getHeaders();
                    String version = (String) headers.get(Constants.BUNDLE_VERSION);
                    if (version != null && version.length() > 0) {
                      properties.put("osgi.web.version", version);
                    }

                    String webContextPath = (String) headers.get(WEB_CONTEXT_PATH);
                    String webappContext = (String) headers.get("Webapp-Context");
                    
                    //This is the default context, but shouldn't it be called default? See PAXWEB-209
                    if ("/".equalsIgnoreCase(context.getContextPath()) && (webContextPath == null || webappContext == null)) {
                      webContextPath = context.getContextPath();
                    }
                    
                    //makes sure the servlet context contains a leading slash
                    webContextPath =  webContextPath != null ? webContextPath : webappContext;
                    if (webContextPath != null && !webContextPath.startsWith("/")) {
                      webContextPath = "/"+webContextPath;
                    }
                    
                    if (webContextPath == null) {
                      LOG.warn("osgi.web.contextpath couldn't be set, it's not configured");
                    }

                    context.registerService(bundleContext, properties);
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
		} else if (Constraint.__BASIC_AUTH.equals(authMethod)) {
      authenticator = new BasicAuthenticator();
    } else if (Constraint.__DIGEST_AUTH.equals(authMethod)) {
      authenticator = new DigestAuthenticator();
    } else if (Constraint.__CERT_AUTH.equals(authMethod)) {
      authenticator = new ClientCertAuthenticator();
    } else if (Constraint.__CERT_AUTH2.equals(authMethod)) {
      authenticator = new ClientCertAuthenticator();
    } else if (Constraint.__SPNEGO_AUTH.equals(authMethod)) {
      authenticator = new SpnegoAuthenticator();
    } else {
      LOG.warn("UNKNOWN AUTH METHOD: " + authMethod);
    }

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
        if( this.m_contextAttributes != null )
        {
            attributes.putAll( this.m_contextAttributes );
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
     * @param cookie     Session cookie name. Defaults to JSESSIONID.
     * @param domain     Session cookie domain. Defaults to the hosts fqdn.
     * @param url        session URL parameter name. Defaults to jsessionid. If set to null or  "none" no URL
     *                   rewriting will be done.
     * @param workerName name appended to session id, used to assist session affinity in a load balancer
     */
    private void configureSessionManager( final ServletContextHandler context,
                                          final Integer minutes,
                                          final String cookie,
                                          final String domain,
                                          final String url,
                                          final String workerName )
    {
        LOG.debug( "configureSessionManager for context [" + context + "] using - timeout:" + minutes
                   + ", cookie:" + cookie + ", url:" + url + ", workerName:" + workerName
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
                if( cookie != null )
                {
                    sessionManager.setSessionCookie( cookie );
                    LOG.debug( "Session cookie set to " + cookie + " for context [" + context + "]" );
                }
                if( domain != null )
                {
                    sessionManager.setSessionDomain( domain );
                    LOG.debug( "Session cookie domain set to " + domain + " for context [" + context + "]" );
                }
                if( url != null )
                {
                    sessionManager.setSessionIdPathParameterName( url );
                    LOG.debug( "Session URL set to " + url + " for context [" + context + "]" );
                }
                if( workerName != null )
                {
                    SessionIdManager sessionIdManager = getSessionIdManager(sessionManager);
                    if( sessionIdManager == null )
                    {
                        sessionIdManager = new HashSessionIdManager();
                        setSessionIdManager( sessionManager, sessionIdManager );
                    }
                    if( sessionIdManager instanceof HashSessionIdManager )
                    {
                        HashSessionIdManager s = (HashSessionIdManager) sessionIdManager;
                        s.setWorkerName( workerName );
                        LOG.debug( "Worker name set to " + workerName + " for context [" + context + "]" );
                    }
                }
            }
        }
    }
   
        private void setSessionIdManager(SessionManager sessionManager, SessionIdManager idManager) {
        try {
            //for JETTY 7.5
            sessionManager.getClass().getMethod("setSessionIdManager", SessionIdManager.class)
                .invoke(sessionManager, idManager);
        } catch (Exception e) {
            try {
                //for JETTY <=7.4.x
                sessionManager.getClass().getMethod("setIdManager", SessionIdManager.class)
                    .invoke(sessionManager, idManager);
            } catch (Exception e1) {
                LOG.error("Cannot set the SessionIdManager on [" + sessionManager + "]", e1);
            }
        }
    }
    
    private SessionIdManager getSessionIdManager(SessionManager sessionManager) {
        try {
            //for JETTY 7.5
            return (SessionIdManager) 
                sessionManager.getClass().getMethod("getSessionIdManager")
                .invoke(sessionManager);
        } catch (Exception e) {
            try {
                //for JETTY <=7.4.x
                return (SessionIdManager)
                    sessionManager.getClass().getMethod("getIdManager")
                    .invoke(sessionManager);
            } catch (Exception e1) {
                LOG.error("Cannot get the SessionIdManager on [" + sessionManager + "]", e1);
                return null;
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
		return this.serverConfigDir;
	}

    public URL getServerConfigURL() {
        return this.serverConfigURL;
    }

    public void setServerConfigURL(URL serverConfigURL) {
        this.serverConfigURL = serverConfigURL;
    }

}
