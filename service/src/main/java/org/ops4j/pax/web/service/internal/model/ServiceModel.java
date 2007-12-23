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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

    public ServiceModel()
    {
        m_aliasMapping = new HashMap<String, ServletModel>();
        m_servlets = new HashSet<Servlet>();
    }

    public synchronized void addServletModel( final ServletModel model )
        throws NamespaceException, ServletException
    {
        if( m_aliasMapping.containsKey( model.getAlias() ) )
        {
            throw new NamespaceException( "alias is already in use in another context" );
        }
        if( m_servlets.contains( model.getServlet() ) )
        {
            throw new ServletException( "servlet already registered with a different alias" );
        }
        m_aliasMapping.put( model.getAlias(), model );
        m_servlets.add( model.getServlet() );
    }

    public synchronized void removeServletModel( final ServletModel model )
    {
        m_aliasMapping.remove( model.getAlias() );
        m_servlets.remove( model.getServlet() );
    }

    public ServletModel getServletModelMatchingAlias( final String alias )
    {
        final boolean debug = LOG.isDebugEnabled();
        if( debug )
        {
            LOG.debug( "Matching [" + alias + "]..." );
        }
        ServletModel matched = m_aliasMapping.get( alias );
        if( matched == null && !"/".equals( alias.trim() ) )
        {
            // next, try for a substring by removing the last "/" and everything to the right of the last "/"
            String substring = alias.substring( 0, alias.lastIndexOf( "/" ) ).trim();
            if( substring.length() > 0 )
            {
                matched = getServletModelMatchingAlias( substring );
            }
            else
            {
                matched = getServletModelMatchingAlias( "/" );
            }
        }
        else if( debug )
        {
            LOG.debug( "Alias [" + alias + "] matched to " + matched );
        }
        return matched;
    }

}
