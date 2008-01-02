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
package org.ops4j.pax.web.service.internal.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.http.NamespaceException;

public class ServiceModel
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( ServiceModel.class );

    private final Map<String, ServletModel> m_aliasMapping;
    private final Set<Servlet> m_servlets;
    private final Map<String, UrlPattern> m_servletUrlPatterns;
    private final Map<String, UrlPattern> m_filterUrlPatterns;

    public ServiceModel()
    {
        m_aliasMapping = new HashMap<String, ServletModel>();
        m_servlets = new HashSet<Servlet>();
        m_servletUrlPatterns = new HashMap<String, UrlPattern>();
        m_filterUrlPatterns = new HashMap<String, UrlPattern>();
    }

    public synchronized void addServletModel( final ServletModel model )
        throws NamespaceException, ServletException
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

    public synchronized void removeServletModel( final ServletModel model )
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

    public synchronized void addFilterModel( final FilterModel model )
    {
        if( model.getUrlPatterns() != null )
        {
            for( String urlPattern : model.getUrlPatterns() )
            {
                m_filterUrlPatterns.put(
                    model.getId() + urlPattern,
                    new UrlPattern( getFullPath( model.getContextModel(), urlPattern ), model )
                );
            }
        }
    }

    public synchronized void removeFilterModel( final FilterModel model )
    {
        if( model.getUrlPatterns() != null )
        {
            for( String urlPattern : model.getUrlPatterns() )
            {
                m_filterUrlPatterns.remove( model.getId() + urlPattern );
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
        UrlPattern urlPattern = matchPathToContext( m_servletUrlPatterns.values(), path );
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
