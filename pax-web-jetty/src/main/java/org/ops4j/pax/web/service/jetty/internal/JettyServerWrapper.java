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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.jetty.http.security.Constraint;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.security.authentication.ClientCertAuthenticator;
import org.eclipse.jetty.security.authentication.DigestAuthenticator;
import org.eclipse.jetty.security.authentication.FormAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HandlerContainer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.handler.ContextHandler.Context;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.ops4j.pax.swissbox.core.BundleUtils;
import org.ops4j.pax.web.service.WebContainerConstants;
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

	private static final Logger LOG = LoggerFactory.getLogger( JettyServerWrapper.class );
	
	private static final String WEB_CONTEXT_PATH = "Web-ContextPath";

    private final ServerModel m_serverModel;
    private final Map<HttpContext, ServletContextHandler> m_contexts;
    private Map<String, Object> m_contextAttributes;
    private Integer m_sessionTimeout;
    private String m_sessionCookie;
    private String m_sessionUrl;
    private String m_sessionWorkerName;

	private File serverConfigDir;

	private ServiceRegistration servletContextService;

    JettyServerWrapper( ServerModel serverModel )
    {
        m_serverModel = serverModel;
        m_contexts = new IdentityHashMap<HttpContext, ServletContextHandler>();
//        setHandler( new JettyServerHandlerCollection( m_serverModel ) );
        setHandler( new HandlerCollection(true) );
    }

    /**
     * {@inheritDoc}
     */
    public void configureContext( final Map<String, Object> attributes,
                                  final Integer sessionTimeout,
                                  final String sessionCookie,
                                  final String sessionUrl,
                                  final String sessionWorkerName)
    {
        m_contextAttributes = attributes;
        m_sessionTimeout = sessionTimeout;
        m_sessionCookie = sessionCookie;
        m_sessionUrl = sessionUrl;
        m_sessionWorkerName = sessionWorkerName;
    }

    ServletContextHandler getContext( final HttpContext httpContext )
    {
        return m_contexts.get( httpContext );
    }

    ServletContextHandler getOrCreateContext( final Model model )
    {
        ServletContextHandler context = m_contexts.get( model.getContextModel().getHttpContext() );
        if( context == null )
        {
            context = addContext( model );
            m_contexts.put( model.getContextModel().getHttpContext(), context );
        }
        return context;
    }

    void removeContext( final HttpContext httpContext )
    {
    	try {
    		if (servletContextService != null) //if null already unregistered!
    			servletContextService.unregister();
    	} catch (IllegalStateException e) {
			LOG.info("ServletContext service already removed");
		} 
    	((HandlerCollection) getHandler()).removeHandler( getContext( httpContext ) );
    	
    	m_contexts.remove( httpContext );
        
    }

    private ServletContextHandler addContext( final Model model )
    { 
        Bundle bundle = model.getContextModel().getBundle();
        BundleContext bundleContext = BundleUtils.getBundleContext(bundle);
		ServletContextHandler context = new HttpServiceContext( //TODO: SERVLET_3 this is the creation of the ServiceContext
												(HandlerContainer) getHandler(), 
												model.getContextModel().getContextParams(),
                                                getContextAttributes(bundleContext), 
                                                model.getContextModel().getContextName(), 
                                                model.getContextModel().getHttpContext(), 
                                                model.getContextModel().getAccessControllerContext(),
                                                model.getContextModel().getContainerInitializers()
        );
        context.setClassLoader( model.getContextModel().getClassLoader() );
        Integer sessionTimeout = model.getContextModel().getSessionTimeout();
        if( sessionTimeout == null )
        {
            sessionTimeout = m_sessionTimeout;
        }
        String sessionCookie = model.getContextModel().getSessionCookie();
        if( sessionCookie == null )
        {
            sessionCookie = m_sessionCookie;
        }
        String sessionUrl = model.getContextModel().getSessionUrl();
        if( sessionUrl == null )
        {
            sessionUrl = m_sessionUrl;
        }
        String workerName = model.getContextModel().getSessionWorkerName();
        if( workerName == null )
        {
            workerName = m_sessionWorkerName;
        }
        configureSessionManager( context, sessionTimeout, sessionCookie, sessionUrl, workerName );
        
        if (model.getContextModel().getRealmName() != null && model.getContextModel().getAuthMethod() != null)
        	configureSecurity(context, model.getContextModel().getRealmName(), 
        							   model.getContextModel().getAuthMethod(), 
        							   model.getContextModel().getFormLoginPage(), 
        							   model.getContextModel().getFormErrorPage());
        
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
                           + model.getContextModel().getHttpContext() + "]", ignore
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
     * @param cookie     Session cookie name. Defaults to JSESSIONID.
     * @param url        session URL parameter name. Defaults to jsessionid. If set to null or  "none" no URL
     *                   rewriting will be done.
     * @param workerName name appended to session id, used to assist session affinity in a load balancer
     */
    private void configureSessionManager( final ServletContextHandler context,
                                          final Integer minutes,
                                          final String cookie,
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
                	if (sessionManager instanceof HashSessionManager) {
                		((HashSessionManager)sessionManager).setSessionCookie( cookie );
                		LOG.debug( "Session cookie set to " + cookie + " for context [" + context + "]" );
                	} else {
                		LOG.debug( "SessionManager isn't of type HashSessionManager therefore cookie not set!");
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
}
