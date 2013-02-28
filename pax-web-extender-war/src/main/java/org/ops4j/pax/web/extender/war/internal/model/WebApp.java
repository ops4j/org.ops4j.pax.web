/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.war.internal.model;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.war.internal.WebAppVisitor;

/**
 * Root element of web.xml.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, December 27, 2007
 */
public class WebApp
{
    public static final String UNDEPLOYED_STATE = "undeployed";
    public static final String WAITING_STATE = "waiting";
    public static final String DEPLOYED_STATE = "deployed";
	
    /**
     * The URL to the web.xml for the web app.
     */
    private String m_deploymentState;

    /**
     * The URL to the web.xml for the web app.
     */
    private URL m_webXmlURL;

    /**
     * Application display name.
     */
    private String m_displayName;
    /**
     * Context name.
     */
    private String m_contextName;
    /**
     * Root path.
     */
    private String m_rootPath;
    /**
     * Session timeout.
     */
    private String m_sessionTimeout;
    /**
     * Bundle that contains the parsed web.xml.
     * Is not set by the parser but by the webXmlObserver after the parsing is done.
     */
    private Bundle m_bundle;

    /**
     * The http context used during registration of error page.
     * Is not set by the parser but by the registration visitor during registration.
     */
    private HttpContext m_httpContext;

    /**
     * Servlets.
     */
    private final Map<String, WebAppServlet> m_servlets;
    /**
     * Mapping between servlet name and servlet mapping.
     */
    private final Map<String, Set<WebAppServletMapping>> m_servletMappings;
    /**
     * Filters.
     */
    private final Map<String, WebAppFilter> m_filters;
    /**
     * Mapping between filter name and filter mapping.
     */
    private final Map<String, Set<WebAppFilterMapping>> m_filterMappings;
    /**
     * Filters order. List in the order the filters should be applied. When read from an web xml it should respect
     * SRV.6.2.4 section in servlet specs which is the order defined in filter mappings.
     */
    private final List<String> m_orderedFilters;
    /**
     * Context parameters.
     */
    private final Set<WebAppInitParam> m_contextParams;
    /**
     * Mime mappings.
     */
    private final Set<WebAppMimeMapping> m_mimeMappings;
    /**
     * Listeners.
     */
    private final List<WebAppListener> m_listeners;
    /**
     * Error pages.
     */
    private final List<WebAppErrorPage> m_errorPages;
    /**
     * Welcome files.
     */
    private final List<String> m_welcomeFiles;
    /**
     * Virtual Host List.
     */
    private final List<String> m_virtualHostList;
    /**
     * Connectors List
     */
    private final List<String> m_connectorList;
    
    
	/**
	 * SecurityConstraints
	 */
	private final List<WebAppConstraintMapping> m_constraintsMapping;
	
	private final List<WebAppSecurityRole> m_securityRoles;
	
	private final List<WebAppLoginConfig> m_loginConfig;

	private Boolean metaDataComplete;

	private final List<WebAppServletContainerInitializer> servletContainerInitializers;
	
	private URL jettyWebXmlURL;

    /**
     * Creates a new web app.
     */
    public WebApp()
    {
        m_servlets = new HashMap<String, WebAppServlet>();
        m_servletMappings = new HashMap<String, Set<WebAppServletMapping>>();
        m_filters = new LinkedHashMap<String, WebAppFilter>();
        m_filterMappings = new HashMap<String, Set<WebAppFilterMapping>>();
        m_orderedFilters = new ArrayList<String>();
        m_listeners = new ArrayList<WebAppListener>();
        m_errorPages = new ArrayList<WebAppErrorPage>();
        m_contextParams = new HashSet<WebAppInitParam>();
        m_mimeMappings = new HashSet<WebAppMimeMapping>();
        m_welcomeFiles = new ArrayList<String>();
        m_constraintsMapping = new ArrayList<WebAppConstraintMapping>();
        m_securityRoles = new ArrayList<WebAppSecurityRole>();
        m_loginConfig = new ArrayList<WebAppLoginConfig>();
        m_virtualHostList = new ArrayList<String>();
        this.m_connectorList = new ArrayList<String>();
        servletContainerInitializers = new ArrayList<WebAppServletContainerInitializer>();
        metaDataComplete = false;
    }

    /**
     * Setter.
     *
     * @param displayName value to set
     */
    public void setDisplayName( final String displayName )
    {
        m_displayName = displayName;
    }


    private WebAppInitParam getWebAppInitParam(String name) {
        for( WebAppInitParam p : m_contextParams) {
            if( name.equals(p.getParamName())) {
                return p;
            }
        }
        return null;
    }


    /**
     * Setter.
     *
     * @param contextName value to set. Cannot be null.
     *
     * @throws NullArgumentException if context name is null
     */
    public void setContextName( final String contextName )
    {
        NullArgumentException.validateNotNull( contextName, "Context name" );
        m_contextName = contextName;

        // remove the previous setting.
        WebAppInitParam prev = getWebAppInitParam("webapp.context");
        if( prev!=null ) {
            m_contextParams.remove(prev);
        }

        // set the context name into the context params
        final WebAppInitParam initParam = new WebAppInitParam();
        initParam.setParamName( "webapp.context" );
        initParam.setParamValue( contextName );
        m_contextParams.add( initParam );
    }

    public String getContextName( ) {
        return m_contextName;
    }

	public void setRootPath(final String rootPath)
	{
        NullArgumentException.validateNotNull( rootPath, "Root Path" );
        m_rootPath = rootPath;
		
	}
    
	public String getRootPath()
	{
		return m_rootPath;		
	}
	
    /**
     * Setter.
     *
     * @param minutes session timeout
     */
    public void setSessionTimeout( final String minutes )
    {
        m_sessionTimeout = minutes;
    }

    /**
     * Getter.
     *
     * @return session timeout in minutes
     */
    public String getSessionTimeout()
    {
        return m_sessionTimeout;
    }

    /**
     * Getter.
     *
     * @return bundle
     */
    public Bundle getBundle()
    {
        return m_bundle;
    }

    /**
     * Setter.
     *
     * @param bundle value to set
     */
    public void setBundle( Bundle bundle )
    {
        m_bundle = bundle;
    }

    /**
     * Add a servlet.
     *
     * @param servlet to add
     *
     * @throws NullArgumentException if servlet, servlet name or servlet class is null
     */
    public void addServlet( final WebAppServlet servlet )
    {
        NullArgumentException.validateNotNull( servlet, "Servlet" );
        NullArgumentException.validateNotNull( servlet.getServletName(), "Servlet name" );
        if (servlet instanceof WebAppJspServlet)
        	NullArgumentException.validateNotNull(((WebAppJspServlet) servlet).getJspPath(), "JSP-path");
        else
        	NullArgumentException.validateNotNull( servlet.getServletClassName(), "Servlet class" );
        m_servlets.put( servlet.getServletName(), servlet );
        // add aliases for servlet mappings added before servlet
        for( WebAppServletMapping mapping : getServletMappings( servlet.getServletName() ) )
        {
            servlet.addUrlPattern( mapping.getUrlPattern() );
        }
    }

    /**
     * Add a servlet mapping.
     *
     * @param servletMapping to add
     *
     * @throws NullArgumentException if servlet mapping, servlet name or url pattern is null
     */
    public void addServletMapping( final WebAppServletMapping servletMapping )
    {
        NullArgumentException.validateNotNull( servletMapping, "Servlet mapping" );
        NullArgumentException.validateNotNull( servletMapping.getServletName(), "Servlet name" );
        NullArgumentException.validateNotNull( servletMapping.getUrlPattern(), "Url pattern" );
        Set<WebAppServletMapping> servletMappings = m_servletMappings.get( servletMapping.getServletName() );
        if( servletMappings == null )
        {
            servletMappings = new HashSet<WebAppServletMapping>();
            m_servletMappings.put( servletMapping.getServletName(), servletMappings );
        }
        servletMappings.add( servletMapping );
        final WebAppServlet servlet = m_servlets.get( servletMapping.getServletName() );
        // can be that the servlet is not yet added
        if( servlet != null )
        {
            servlet.addUrlPattern( servletMapping.getUrlPattern() );
        }
    }

    /**
     * Returns a servlet mapping by servlet name.
     *
     * @param servletName servlet name
     *
     * @return array of servlet mappings for requested servlet name
     */
    public List<WebAppServletMapping> getServletMappings( final String servletName )
    {
        final Set<WebAppServletMapping> servletMappings = m_servletMappings.get( servletName );
        if( servletMappings == null )
        {
            return new ArrayList<WebAppServletMapping>();
        }
        return new ArrayList<WebAppServletMapping>(servletMappings);
    }

    /**
     * Add a filter.
     *
     * @param filter to add
     *
     * @throws NullArgumentException if filter, filter name or filter class is null
     */
    public void addFilter( final WebAppFilter filter )
    {
        NullArgumentException.validateNotNull( filter, "Filter" );
        NullArgumentException.validateNotNull( filter.getFilterName(), "Filter name" );
        NullArgumentException.validateNotNull( filter.getFilterClass(), "Filter class" );
        m_filters.put( filter.getFilterName(), filter );
        // add url patterns and servlet names for filter mappings added before filter
        for( WebAppFilterMapping mapping : getFilterMappings( filter.getFilterName() ) )
        {
            if( mapping.getUrlPattern() != null && mapping.getUrlPattern().trim().length() > 0 )
            {
                filter.addUrlPattern( mapping.getUrlPattern() );
            }
            if( mapping.getServletName() != null && mapping.getServletName().trim().length() > 0 )
            {
                filter.addServletName( mapping.getServletName() );
            }
        }
    }

    /**
     * Add a filter mapping.
     *
     * @param filterMapping to add
     *
     * @throws NullArgumentException if filter mapping or filter name is null
     */
    public void addFilterMapping( final WebAppFilterMapping filterMapping )
    {
        NullArgumentException.validateNotNull( filterMapping, "Filter mapping" );
        NullArgumentException.validateNotNull( filterMapping.getFilterName(), "Filter name" );

        final String filterName = filterMapping.getFilterName();
        if( !m_orderedFilters.contains( filterName ) )
        {
            m_orderedFilters.add( filterName );
        }
        Set<WebAppFilterMapping> filterMappings = m_filterMappings.get( filterName );
        if( filterMappings == null )
        {
            filterMappings = new HashSet<WebAppFilterMapping>();
            m_filterMappings.put( filterName, filterMappings );
        }
        filterMappings.add( filterMapping );
        final WebAppFilter filter = m_filters.get( filterName );
        // can be that the filter is not yet added
        if( filter != null )
        {
            if( filterMapping.getUrlPattern() != null && filterMapping.getUrlPattern().trim().length() > 0 )
            {
                filter.addUrlPattern( filterMapping.getUrlPattern() );
            }
            if( filterMapping.getServletName() != null && filterMapping.getServletName().trim().length() > 0 )
            {
                filter.addServletName( filterMapping.getServletName() );
            }
        }
    }

    /**
     * Returns filter mappings by filter name.
     *
     * @param filterName filter name
     *
     * @return array of filter mappings for requested filter name
     */
    public List<WebAppFilterMapping> getFilterMappings( final String filterName )
    {
        final Set<WebAppFilterMapping> filterMappings = m_filterMappings.get( filterName );
        if( filterMappings == null )
        {
            return new ArrayList<WebAppFilterMapping>();
        }
        return new ArrayList<WebAppFilterMapping>(filterMappings);
    }

    /**
     * Add a listener.
     *
     * @param listener to add
     *
     * @throws NullArgumentException if listener or listener class is null
     */
    public void addListener( final WebAppListener listener )
    {
        NullArgumentException.validateNotNull( listener, "Listener" );
        NullArgumentException.validateNotNull( listener.getListenerClass(), "Listener class" );
        m_listeners.add( listener );
    }

    /**
     * Add an error page.
     *
     * @param errorPage to add
     *
     * @throws NullArgumentException if error page is null or both error type and exception code is null
     */
    public void addErrorPage( final WebAppErrorPage errorPage )
    {
        NullArgumentException.validateNotNull( errorPage, "Error page" );
        if( errorPage.getErrorCode() == null && errorPage.getExceptionType() == null )
        {
            throw new NullPointerException( "At least one of error type or exception code must be set" );
        }
        m_errorPages.add( errorPage );
    }

    /**
     * Add a welcome file.
     *
     * @param welcomeFile to add
     *
     * @throws NullArgumentException if welcome file is null or empty
     */
    public void addWelcomeFile( final String welcomeFile )
    {
        NullArgumentException.validateNotEmpty( welcomeFile, "Welcome file" );
        m_welcomeFiles.add( welcomeFile );
    }

    /**
     * Return all welcome files.
     *
     * @return an array of all welcome files
     */
    public String[] getWelcomeFiles()
    {
        return m_welcomeFiles.toArray( new String[m_welcomeFiles.size()] );
    }

    /**
     * Add a context param.
     *
     * @param contextParam to add
     *
     * @throws NullArgumentException if context param, param name or param value is null
     */
    public void addContextParam( final WebAppInitParam contextParam )
    {
        NullArgumentException.validateNotNull( contextParam, "Context param" );
        NullArgumentException.validateNotNull( contextParam.getParamName(), "Context param name" );
        NullArgumentException.validateNotNull( contextParam.getParamValue(), "Context param value" );
        m_contextParams.add( contextParam );
    }

    /**
     * Return all context params.
     *
     * @return an array of all context params
     */
    public WebAppInitParam[] getContextParams()
    {
        return m_contextParams.toArray( new WebAppInitParam[m_contextParams.size()] );
    }

    /**
     * Add a mime mapping.
     *
     * @param mimeMapping to add
     *
     * @throws NullArgumentException if mime mapping, extension or mime type is null
     */
    public void addMimeMapping( final WebAppMimeMapping mimeMapping )
    {
        NullArgumentException.validateNotNull( mimeMapping, "Mime mapping" );
        NullArgumentException.validateNotNull( mimeMapping.getExtension(), "Mime mapping extension" );
        NullArgumentException.validateNotNull( mimeMapping.getMimeType(), "Mime mapping type" );
        m_mimeMappings.add( mimeMapping );
    }

    /**
     * Add a security constraint
     * 
     * @param securityConstraint
     * 
     * @throws NullArgumentException if security constraint is null
     */
    public void addConstraintMapping( final WebAppConstraintMapping constraintMapping ) {
    	NullArgumentException.validateNotNull( constraintMapping, "constraint mapping");
    	m_constraintsMapping.add(constraintMapping);
    }
    
    /**
     * @return
     */
    public WebAppConstraintMapping[] getConstraintMappings() {
    	return m_constraintsMapping.toArray(new WebAppConstraintMapping[m_constraintsMapping.size()]);
    }
    
    /**
     * Adds a security role
     * 
     * @param securityRole
     */
    public void addSecurityRole( final WebAppSecurityRole securityRole ) {
    	NullArgumentException.validateNotNull(securityRole, "Security Role");
    	m_securityRoles.add(securityRole);
    }
    
    
    /**
     * @return
     */
    public WebAppSecurityRole[] getSecurityRoles() {
    	return m_securityRoles.toArray(new WebAppSecurityRole[m_securityRoles.size()]);
    }
    
    /**
     * Adds a login config
     * 
     * @param loginConfig
     */
    public void addLoginConfig( final WebAppLoginConfig loginConfig ) {
    	NullArgumentException.validateNotNull(loginConfig, "Login Config");
    	NullArgumentException.validateNotNull(loginConfig.getAuthMethod(), "Login Config Authorization Method");
//    	NullArgumentException.validateNotNull(loginConfig.getRealmName(), "Login Config Realm Name");
    	m_loginConfig.add(loginConfig);
    }
    
    /**
     * @return
     */
    public WebAppLoginConfig[] getLoginConfigs() {
    	return m_loginConfig.toArray(new WebAppLoginConfig[m_loginConfig.size()]);
    }
    
    /**
     * Return all mime mappings.
     *
     * @return an array of all mime mappings
     */
    public WebAppMimeMapping[] getMimeMappings()
    {
        return m_mimeMappings.toArray( new WebAppMimeMapping[m_mimeMappings.size()] );
    }

    /**
     * Getter.
     *
     * @return http context
     */
    public HttpContext getHttpContext()
    {
        return m_httpContext;
    }

    /**
     * Setter.
     *
     * @param httpContext value to set
     */
    public void setHttpContext( HttpContext httpContext )
    {
        m_httpContext = httpContext;
    }

    /**
     * Accepts a visitor for inner elements.
     *
     * @param visitor visitor
     */
    public void accept( final WebAppVisitor visitor )
    {
        visitor.visit( this ); //First do everything else
        
        for( WebAppListener listener : m_listeners )
        {
            visitor.visit( listener );
        }
        if( !m_filters.isEmpty() )
        {
            // first visit the filters with a filter mapping in mapping order
            final List<WebAppFilter> remainingFilters = new ArrayList<WebAppFilter>( m_filters.values() );
            for( String filterName : m_orderedFilters )
            {
                final WebAppFilter filter = m_filters.get( filterName );
                visitor.visit( filter );
                remainingFilters.remove( filter );
            }
            // then visit filters without a mapping order in the order they were added
            for( WebAppFilter filter : remainingFilters )
            {
                visitor.visit( filter );
            }
        }
        if( !m_servlets.isEmpty() ) //TODO: SERVLET_3 - this could be the place for the ServletContainerInitializer
        {
            for( WebAppServlet servlet : getSortedWebAppServlet() ) //Fix for PAXWEB-205
            {
                visitor.visit( servlet );
            }
        }
        if ( !m_constraintsMapping.isEmpty() ) //Added for PAXWEB-210 - might be a to late for initialization
        {
        	for (WebAppConstraintMapping constraintMapping : m_constraintsMapping) {
        		visitor.visit(constraintMapping);				
			}
        	
        }

        for( WebAppErrorPage errorPage : m_errorPages )
        {
            visitor.visit( errorPage );
        }

        visitor.end();
    }

    static final Comparator<WebAppServlet> WebAppServletComparator = new Comparator<WebAppServlet>() {
		public int compare(WebAppServlet servlet1, WebAppServlet servlet2) {
			return 
				servlet1.getLoadOnStartup() - servlet2.getLoadOnStartup();
		}
    };
    
   
    
    private Collection<WebAppServlet> getSortedWebAppServlet() {
    	List <WebAppServlet> servlets = new ArrayList<WebAppServlet>( m_servlets.values() );
    	Collections.sort( servlets,  WebAppServletComparator );
    	
    	return servlets;
    }

    
    @Override
    public String toString()
    {
        return new StringBuffer()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "displayName=" ).append( m_displayName )
            .append( ",contextName=" ).append( m_contextName )    
            .append( ",m_httpContext=" ).append( m_httpContext )
            .append( "}" )
            .toString();
    }

    public URL getWebXmlURL() {
        return m_webXmlURL;
    }

    public void setWebXmlURL(URL m_webXmlURL) {
        this.m_webXmlURL = m_webXmlURL;
    }
    

	public void setJettyWebXmlURL(URL jettyWebXmlURL) {
		this.jettyWebXmlURL = jettyWebXmlURL;
	}

	public URL getJettyWebXmlURL() {
		return jettyWebXmlURL;
	}
	
	public void setVirtualHostList(List<String> virtualHostList) {
		this.m_virtualHostList.clear();
		this.m_virtualHostList.addAll(virtualHostList);
	}
	
	public List<String> getVirtualHostList() {
		return m_virtualHostList;
	}
	
	public void setConnectorList(List<String> connectorList) {
		this.m_connectorList.clear();
		this.m_connectorList.addAll(connectorList);
	}
	
	public List<String> getConnectorList() {
		return m_connectorList;
	}
	
    public String getDeploymentState() {
        return m_deploymentState;
    }

    public void setDeploymentState(String deploymentState) {
        this.m_deploymentState = deploymentState;
    }

	public void setMetaDataComplete(Boolean metaDataComplete) {
		this.metaDataComplete = metaDataComplete;
	}
	
	public Boolean getMetaDataComplete() {
		return metaDataComplete;
	}

	public WebAppServlet findServlet(String servletName) {
		if (this.m_servlets.containsKey(servletName)) {
			return this.m_servlets.get(servletName);
		} else {
			return null;
		}
	}

	public WebAppFilter findFilter(String filterName) {
		if (this.m_filters.containsKey(filterName))
			return this.m_filters.get(filterName);
		else
			return null;
	}

	public void addServletContainerInitializer(
			WebAppServletContainerInitializer servletContainerInitializer) {
		NullArgumentException.validateNotNull(servletContainerInitializer, "ServletContainerInitializer");
    	this.servletContainerInitializers.add(servletContainerInitializer);
	}

	public List<WebAppServletContainerInitializer> getServletContainerInitializers() {
		return servletContainerInitializers;
	}

}

