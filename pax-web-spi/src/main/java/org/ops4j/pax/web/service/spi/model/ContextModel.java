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
package org.ops4j.pax.web.service.spi.model;

import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

/**
 * Models a servlet context related to an http context.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, December 29, 2007
 */
public class ContextModel extends Identity
{

    private final HttpContext m_httpContext;
    private final ClassLoader m_classLoader;
    private final Map<String, String> m_contextParams;
    private String m_contextName;
    /**
     * Welcome files filter. Valid (not null) only if the welcome files are registered.
     */
    private Filter m_welcomeFilesFilter;

    /**
     * Access controller context of the bundle that registered the http context.
     */
    private AccessControlContext m_accessControllerContext;
    /**
     * Registered jsp servlets for this context.
     */
    private Map<Servlet, String[]> m_jspServlets;
    /**
     * Session timeout in minutes.
     */
    private Integer m_sessionTimeout;
    /**
     * Session cookie name.
     */
    private String m_sessionCookie;
    /**
     * Session URL parameter name.
     */
    private String m_sessionUrl;
    /**
     * Session Cookie for HttpOnly 
     */
    private Boolean m_sessionCookieHttpOnly;
    /**
     * Name appended to session id, used to assist session affinity in a load balancer.
     */
    private String m_sessionWorkerName;
    /**
     * Bundle that used the http context to register an web element.
     */
    private final Bundle m_bundle;
	/**
	 * The realm name to use with the http context.
	 */
	private String realmName;
	/**
	 * The authorization method used in this http context.
	 */
	private String authMethod;
	
	/**
	 * Login page for FORM based authentication.
	 */
	private String formLoginPage;
	
	/**
	 * Error page for FORM based authentication.
	 */
	private String formErrorPage;
	
	/**
	 * Container Initializers
	 */
	private Map<ServletContainerInitializer, Set<Class<?>>> containerInitializers;
	
	/**
	 * Jetty Web XML URL
	 */
	private URL jettyWebXmlUrl;	
	
	/**
	 * Virtual Host List
	 */
	private final List<String> virtualHosts;
	
	/**
	 * Connectors List
	 */
	private final List<String>  connectors;

    public ContextModel( final HttpContext httpContext,
                         final Bundle bundle,
                         final ClassLoader classLoader )
    {
        m_bundle = bundle;
        NullArgumentException.validateNotNull( httpContext, "Http context" );
        NullArgumentException.validateNotNull( classLoader, "Class loader" );
        m_classLoader = classLoader;
        m_httpContext = httpContext;
        m_contextParams = new HashMap<String, String>();
        m_jspServlets = new IdentityHashMap<Servlet, String[]>();
        m_contextName = "";
        // capture access controller context of the bundle that registered the context
        // TODO does this work with an extender bundle?
        m_accessControllerContext = AccessController.getContext();
        virtualHosts = new ArrayList<String>();
        connectors = new ArrayList<String>();
    }

    public HttpContext getHttpContext()
    {
        return m_httpContext;
    }

    public ClassLoader getClassLoader()
    {
        return m_classLoader;
    }

	/**
	 * @return the formLoginPage
	 */
	public String getFormLoginPage() {
		return formLoginPage;
	}

	/**
	 * @return the formErrorPage
	 */
	public String getFormErrorPage() {
		return formErrorPage;
	}
    
    @SuppressWarnings("rawtypes")
	public void setContextParams( final Dictionary contextParams )
    {
        m_contextParams.clear();
        if( contextParams != null && !contextParams.isEmpty() )
        {
            final Enumeration keys = contextParams.keys();
            while( keys.hasMoreElements() )
            {
                final Object key = keys.nextElement();
                final Object value = contextParams.get( key );
                if( !( key instanceof String ) || !( value instanceof String ) )
                {
                    throw new IllegalArgumentException( "Context params keys and values must be Strings" );
                }
                m_contextParams.put( (String) key, (String) value );
            }
            m_contextName = m_contextParams.get( WebContainerConstants.CONTEXT_NAME );
        }
        if( m_contextName != null )
        {
            m_contextName = m_contextName.trim();
        }
        else
        {
            m_contextName = "";
        }
    }

    /**
     * Getter.
     *
     * @return map of context params
     */
    public Map<String, String> getContextParams()
    {
        return m_contextParams;
    }

    /**
     * Getter.
     *
     * @return context name
     */
    public String getContextName()
    {
        return m_contextName;
    }

    /**
     * Getter.
     *
     * @return jsp servlet
     */
    public Map<Servlet, String[]> getJspServlets()
    {
        return m_jspServlets;
    }

    /**
     * Getter.
     *
     * @return the access controller context of the bundle that registred the context
     */
    public AccessControlContext getAccessControllerContext()
    {
        return m_accessControllerContext;
    }

    /**
     * Getter.
     *
     * @return welcome files filter
     */
    public Filter getWelcomeFilesFilter()
    {
        return m_welcomeFilesFilter;
    }

    /**
     * Setter.
     *
     * @param welcomeFilesFilter value to set
     */
    public void setWelcomeFilesFilter( Filter welcomeFilesFilter )
    {
        m_welcomeFilesFilter = welcomeFilesFilter;
    }

    /**
     * Getter.
     *
     * @return session timeout
     */
    public Integer getSessionTimeout()
    {
        return m_sessionTimeout;
    }

    /**
     * Setter.
     *
     * @param sessionTimeout value to set
     */
    public void setSessionTimeout( Integer sessionTimeout )
    {
        m_sessionTimeout = sessionTimeout;
    }

    /**
     * Getter.
     *
     * @return session cookie name
     */
    public String getSessionCookie()
    {
        return m_sessionCookie;
    }

    /**
     * Setter.
     *
     * @param sessionCookie session cookie name
     */
    public void setSessionCookie( final String sessionCookie )
    {
        m_sessionCookie = sessionCookie;
    }

    /**
     * Getter.
     *
     * @return session url name
     */
    public String getSessionUrl()
    {
        return m_sessionUrl;
    }

    /**
     * Setter.
     *
     * @param sessionUrl session url name
     */
    public void setSessionUrl( final String sessionUrl )
    {
        m_sessionUrl = sessionUrl;
    }
    

	/**
	 * @return session cookie HTTP Only
	 */
	public Boolean getSessionCookieHttpOnly() {
		return m_sessionCookieHttpOnly;
	}
	
	public void setSessionCookieHttpOnly( final Boolean sessionCookieHttpOnly ) {
		m_sessionCookieHttpOnly = sessionCookieHttpOnly;
	}

    /**
     * Getter.
     *
     * @return session worker name
     */
    public String getSessionWorkerName()
    {
        return m_sessionWorkerName;
    }

    /**
     * Setter.
     *
     * @param sessionWorkerName session worker name
     */
    public void setSessionWorkerName( final String sessionWorkerName )
    {
        m_sessionWorkerName = sessionWorkerName;
    }

    /**
     * Getter.
     *
     * @return bundle associated with this context
     */
    public Bundle getBundle()
    {
        return m_bundle;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "id=" ).append( getId() )
            .append( ",name=" ).append( m_contextName )
            .append( ",httpContext=" ).append( m_httpContext )
            .append( ",contextParams=" ).append( m_contextParams )
            .append( ",virtualHosts={" );
        for (String virtualHost: virtualHosts) {
        	sb.append(virtualHost).append(",");
        }
        sb.append("},connectors={");
        for (String connector: connectors) {
        	sb.append(connector).append(",");
        }
        return sb.append( "}}" )
            .toString();
    }

	public void setRealmName(String realmName) {
		this.realmName = realmName;
	}

	public void setAuthMethod(String authMethod) {
		this.authMethod = authMethod;
	}

	public void setFormLoginPage(String formLoginPage) {
		this.formLoginPage = formLoginPage;
	}
	
	public void setFormErrorPage(String formErrorPage) {
		this.formErrorPage = formErrorPage;
	}

	/**
	 * @return the realmName
	 */
	public String getRealmName() {
		return realmName;
	}

	/**
	 * @return the authMethod
	 */
	public String getAuthMethod() {
		return authMethod;
	}

	/**
	 * @return the containerInitializers
	 */
	public Map<ServletContainerInitializer, Set<Class<?>>> getContainerInitializers() {
		return containerInitializers;
	}

	/**
	 * @param containerInitializers the containerInitializers to set
	 */
	public void setContainerInitializers(Map<ServletContainerInitializer, Set<Class<?>>> containerInitializers) {
		this.containerInitializers = containerInitializers;
	}
	
	public void addContainerInitializer(ServletContainerInitializer containerInitializer , Set<Class<?>> classes) {
		if (this.containerInitializers == null)
			containerInitializers = new HashMap<ServletContainerInitializer, Set<Class<?>>>();
		containerInitializers.put(containerInitializer, classes);
	}
	
	public void setVirtualHosts(List<String> virtualHosts) {
		this.virtualHosts.clear();
		this.virtualHosts.addAll(virtualHosts);
	}
	
	public List<String> getVirtualHosts() {
		return virtualHosts;
	}
	
	public void setConnectors(List<String> connectors) {
		this.connectors.clear();
		this.connectors.addAll(connectors);
	}
	
	public List<String> getConnectors() {
		return connectors;
	}

	public void setJettyWebXmlUrl(URL jettyWebXmlUrl) {
		this.jettyWebXmlUrl = jettyWebXmlUrl;
	}

	public URL getJettyWebXmlURL() {
		return jettyWebXmlUrl;
	}
}
