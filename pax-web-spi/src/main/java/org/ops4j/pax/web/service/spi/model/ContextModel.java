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

import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.WebContainerConstants;

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
     * Registered jsp servlet for this context. Can be null as long as jsp support was not enabled.
     */
    private Servlet m_jspServlet;
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

	/**
	 * Error page for FORM based authentication.
	 */
	private String formErrorPage;

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
        m_contextName = "";
        // capture access controller context of the bundle that registered the context
        // TODO does this work with an extender bundle?
        m_accessControllerContext = AccessController.getContext();
    }

    public HttpContext getHttpContext()
    {
        return m_httpContext;
    }

    public ClassLoader getClassLoader()
    {
        return m_classLoader;
    }

    @SuppressWarnings("rawtypes")
	public void setContextParams( final Dictionary contextParams )
    {
        if( contextParams != null && !contextParams.isEmpty() )
        {
            m_contextParams.clear();
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
        }
        m_contextName = m_contextParams.get( WebContainerConstants.CONTEXT_NAME );
        if( m_contextName != null )
        {
            m_contextName = m_contextName.trim();
        }
        else
        {
            m_contextName = "";
        }
        // TODO validate context name (no "/" ?)
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
    public Servlet getJspServlet()
    {
        return m_jspServlet;
    }

    /**
     * Setter.
     *
     * @param jspServlet value to set
     */
    public void setJspServlet( final Servlet jspServlet )
    {
        m_jspServlet = jspServlet;
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
        return new StringBuilder()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "id=" ).append( getId() )
            .append( ",name=" ).append( m_contextName )
            .append( ",httpContext=" ).append( m_httpContext )
            .append( ",contextParams=" ).append( m_contextParams )
            .append( "}" )
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

}
