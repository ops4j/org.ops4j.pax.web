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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

/**
 * Holds web elements in a global context accross all services (all bundles usng the Http Service).
 *
 * @author Alin Dreghiciu
 */
public class ServerModel
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( ServerModel.class );

    /**
     * Map between aliases used for registering a servlet and the registered servlet model.
     * Used to block registration of an alias more then one time.
     */
    private final Map<String, ServletModel> m_aliasMapping;
    /**
     * Set of all registered servlets.
     * Used to block registration of the same servlet more times.
     */
    private final Set<Servlet> m_servlets;
    /**
     * Mapping between full registration url patterns and servlet model. Full url pattern mean that it has the context
     * name prepended (if context name is set) to the actual url pattern.
     * Used to globally find (against all registered patterns) the right servlet context for the pattern.
     */
    private final Map<String, UrlPattern> m_servletUrlPatterns;
    /**
     * Mapping between full registration url patterns and filter model. Full url pattern mean that it has the context
     * name prepended (if context name is set) to the actual url pattern.
     * Used to globally find (against all registered patterns) the right filter context for the pattern.
     */
    private final ConcurrentMap<String, UrlPattern> m_filterUrlPatterns;
    /**
     * Map between http contexts and the bundle that registred a web element using that http context.
     * Used to block more bundles registering web elements udng the same http context.
     */
    private final ConcurrentMap<HttpContext, Bundle> m_httpContexts;
    /**
     * Servlet lock. Used to sychonchornize on servlet registration/unregistrationhat that works agains 3 maps
     * (m_servlets, m_aliasMaping, m_servletToUrlPattern).
     */
    private final Lock m_servletLock;
    
    /**
     * Constructor.
     */
    public ServerModel()
    {
        m_aliasMapping = new HashMap<String, ServletModel>();
        m_servlets = new HashSet<Servlet>();
        m_servletUrlPatterns = new HashMap<String, UrlPattern>();
        m_filterUrlPatterns = new ConcurrentHashMap<String, UrlPattern>();
        m_httpContexts = new ConcurrentHashMap<HttpContext, Bundle>();
        //TODO add security stuff here (PAXWEB-210)

        m_servletLock = new ReentrantLock();
    }

    /**
     * Registers a servlet model.
     *
     * @param model servlet model to register
     *
     * @throws ServletException   - If servlet is already registered
     * @throws NamespaceException - If servlet alias is already registered
     */
    public void addServletModel( final ServletModel model )
        throws NamespaceException, ServletException
    {
        m_servletLock.lock();
        try
        {
            if( m_servlets.contains( model.getServlet() ) )
            {
                throw new ServletException( "servlet already registered with a different alias" );
            }
            if( model.getAlias() != null )
            {
                final String alias = getFullPath( model.getContextModel(), model.getAlias() );
                if( m_aliasMapping.containsKey( alias ) )
                {
                    throw new NamespaceException( "alias is already in use in this or another context" );
                }
                m_aliasMapping.put( alias, model );
            }
            m_servlets.add( model.getServlet() );
            for( String urlPattern : model.getUrlPatterns() )
            {
                m_servletUrlPatterns.put(
                    model.getId() + urlPattern,
                    new UrlPattern( getFullPath( model.getContextModel(), urlPattern ), model )
                );
            }
        }
        finally
        {
            m_servletLock.unlock();
        }
    }

    /**
     * Unregisters a servlet model.
     *
     * @param model servlet model to unregister
     */
    public void removeServletModel( final ServletModel model )
    {
        m_servletLock.lock();
        try
        {
            if( model.getAlias() != null )
            {
                m_aliasMapping.remove( getFullPath( model.getContextModel(), model.getAlias() ) );
            }
            m_servlets.remove( model.getServlet() );
            if( model.getUrlPatterns() != null )
            {
                for( String urlPattern : model.getUrlPatterns() )
                {
                    m_servletUrlPatterns.remove( model.getId() + urlPattern );
                }
            }
        }
        finally
        {
            m_servletLock.unlock();
        }
    }

    /**
     * Registers a filter model.
     *
     * @param model filter model to register
     */
    public void addFilterModel( final FilterModel model )
    {
        if( model.getUrlPatterns() != null )
        {
            for( String urlPattern : model.getUrlPatterns() )
            {
                final UrlPattern newUrlPattern = new UrlPattern(
                    getFullPath( model.getContextModel(), urlPattern ),
                    model
                );
                final UrlPattern existingPattern = m_filterUrlPatterns.putIfAbsent(
                    model.getId() + urlPattern,
                    newUrlPattern
                );
                if( existingPattern != null )
                {
                    // this should never happen but is a good assertion
                    LOG.error(
                        "Internal error (please report): Cannot associate url mapping " + model.getId() + urlPattern
                        + " to " + newUrlPattern
                        + " because is already associated to " + existingPattern
                    );
                }
            }
        }
    }

    /**
     * Unregister a filter model.
     *
     * @param model filter model to unregister
     */
    public void removeFilterModel( final FilterModel model )
    {
        if( model.getUrlPatterns() != null )
        {
            for( String urlPattern : model.getUrlPatterns() )
            {
                m_filterUrlPatterns.remove( model.getId() + urlPattern );
            }
        }
    }
    
    /**
     * Associates a http context with a bundle if the http service is not already associated to another bundle. This is
     * done in order to prevent sharinh http context between bundles. The implementation is not 100% correct as it can
     * be that at a certain moment in time when this method is called,another thread is processing a release of the
     * http service, process that will deassociate the bundle that releasd the http service, and that bundle could
     * actually be related to the http context that this method is trying to associate. But this is less likely to
     * happen as it should have as precondition that this is happening concurent and that the two bundles are sharing
     * the http context. But this solution has the benefits of not needing synchronization.
     *
     * @param httpContext         http context to be assicated to the bundle
     * @param bundle              bundle to be assiciated with the htp service
     * @param allowReAsssociation if it should allow a context to be reassiciated to a bundle
     *
     * @throws IllegalStateException - If htp context is already associated to another bundle.
     */
    public void associateHttpContext( final HttpContext httpContext,
                                      final Bundle bundle,
                                      final boolean allowReAsssociation )
    {
        final Bundle currentBundle = m_httpContexts.putIfAbsent( httpContext, bundle );
        if( ( !!allowReAsssociation ) && currentBundle != null && currentBundle != bundle )
        {
            throw new IllegalStateException(
                "Http context " + httpContext + " is already associated to bundle " + currentBundle
            );
        }
    }

    /**
     * Deassociate all http context assiciated to the provided bundle. The bellow code is only correct in the context
     * that there is no other thread is calling the association method in the mean time. This should not happen as once
     * a bundle is releasing the HttpService the service is first entering a stopped state ( before the call to this
     * method is made), state that will not perform the registration calls anymore.
     *
     * @param bundle bundle to be deassociated from http contexts
     */
    public void deassociateHttpContexts( final Bundle bundle )
    {
        for( Map.Entry<HttpContext, Bundle> entry : m_httpContexts.entrySet() )
        {
            if( entry.getValue() == bundle )
            {
                m_httpContexts.remove( entry.getKey() );
            }
        }
    }

    public ContextModel matchPathToContext( final String path )
    {
        final boolean debug = LOG.isDebugEnabled();
        if( debug )
        {
            LOG.debug( "Matching [" + path + "]..." );
        }
        // first match servlets
        m_servletLock.lock();
        UrlPattern urlPattern = matchPathToContext( m_servletUrlPatterns.values(), path );
        m_servletLock.unlock();
        // then if there is no matched servlet look for filters
        if( urlPattern == null )
        {
            urlPattern = matchPathToContext( m_filterUrlPatterns.values(), path );
        }
        ContextModel matched = null;
        if( urlPattern != null )
        {
            matched = urlPattern.getModel().getContextModel();
        }
        if( debug )
        {
            if( matched != null )
            {
                LOG.debug( "Path [" + path + "] matched to " + urlPattern );
            }
            else
            {
                LOG.debug( "Path [" + path + "] does not match any context" );
            }
        }
        return matched;
    }

    private static UrlPattern matchPathToContext( final Collection<UrlPattern> urlPatterns, final String path )
    {
        UrlPattern matched = null;
        if( urlPatterns != null )
        {
            for( UrlPattern urlPattern : urlPatterns )
            {
                //LOG.debug( "Matching against " + urlPattern.getPattern() );
                if( matched == null || urlPattern.isBetterMatchThen( matched ) )
                {
                    if( urlPattern.getPattern().matcher( path ).matches() )
                    {
                        matched = urlPattern;
                        //LOG.debug( "Matched. Best match " + matched );
                    }
                    else if( !path.endsWith( "/" ) && urlPattern.getPattern().matcher( path + "/" ).matches() )
                    {
                        matched = urlPattern;
                        //LOG.debug( "Matched. Best match " + matched );
                    }
                }
            }
        }
        return matched;
    }

    /**
     * Returns the full path (including the context name if set)
     *
     * @param model a context model
     * @param path  path to be prepended
     *
     * @return full path
     */
    private static String getFullPath( final ContextModel model, final String path )
    {
        String fullPath = path.trim();
        if( model.getContextName().length() > 0 )
        {
            fullPath = "/" + model.getContextName();
            if( !"/".equals( path.trim() ) )
            {
                fullPath = fullPath + path;
            }
        }
        return fullPath;
    }

    /**
     * Touple of full url pattern and registered model (servlet/filter) for the model.
     */
    private static class UrlPattern
    {

        private final Pattern m_pattern;
        private final Model m_model;

        UrlPattern( final String pattern, final Model model )
        {
            m_model = model;
            String patternToUse = pattern;
            if( !patternToUse.contains( "*" ) )
            {
                patternToUse = patternToUse + ( pattern.endsWith( "/" ) ? "*" : "/*" );
            }
            patternToUse = patternToUse.replace( ".", "\\." );
            patternToUse = patternToUse.replace( "*", ".*" );
            m_pattern = Pattern.compile( patternToUse );
        }

        Pattern getPattern()
        {
            return m_pattern;
        }

        Model getModel()
        {
            return m_model;
        }

        public boolean isBetterMatchThen( final UrlPattern urlPattern )
        {
            return
                urlPattern == null
                || ( this != urlPattern
                     && m_pattern.pattern().length() > urlPattern.m_pattern.pattern().length() );
        }

        @Override
        public String toString()
        {
            return new StringBuffer()
                .append( "{" )
                .append( "pattern=" ).append( m_pattern.pattern() )
                .append( ",model=" ).append( m_model )
                .append( "}" )
                .toString();
        }
    }

}
