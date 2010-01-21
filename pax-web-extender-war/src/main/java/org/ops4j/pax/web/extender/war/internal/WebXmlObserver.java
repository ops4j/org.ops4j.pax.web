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
package org.ops4j.pax.web.extender.war.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.lang.PreConditionException;
import org.ops4j.pax.swissbox.extender.BundleObserver;
import org.ops4j.pax.web.extender.war.internal.model.WebApp;

/**
 * Register/unregister web applications once a bundle containing a "WEB-INF/web.xml" gets started or stopped.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, Decemver 27, 2007
 */
class WebXmlObserver
    implements BundleObserver<URL>
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( WebXmlObserver.class );
    /**
     * web.xml parser to be used.
     */
    private final WebXmlParser m_parser;
    /**
     * Web app publisher.
     */
    private final WebAppPublisher m_publisher;
    /**
     * Mapping between the URL of web.xml and the published web app.
     */
    private final Map<URL, WebApp> m_publishedWebApps;

    /**
     * Creates a new web.xml observer.
     *
     * @param parser    parser for web.xml
     * @param publisher web app publisher
     *
     * @throws NullArgumentException if parser or publisher is null
     */
    WebXmlObserver( final WebXmlParser parser, final WebAppPublisher publisher )
    {
        NullArgumentException.validateNotNull( parser, "Web.xml Parser" );
        NullArgumentException.validateNotNull( publisher, "Web App Publisher" );
        m_parser = parser;
        m_publisher = publisher;
        m_publishedWebApps = new HashMap<URL, WebApp>();
    }

    /**
     * Parse the web.xml and publish the corresponding web app.
     * The received list is expected to contain one URL of an web.xml (only frst is used.
     * The web.xml will be parsed and resulting web application structure will be registered with the http service.
     *
     * @throws NullArgumentException if bundle or list of web xmls is null
     * @throws PreConditionException if the list of web xmls is empty or more then one xml
     * @see BundleObserver#addingEntries(Bundle,List)
     */
    public void addingEntries( final Bundle bundle, final List<URL> entries )
    {
        NullArgumentException.validateNotNull( bundle, "Bundle" );
        NullArgumentException.validateNotNull( entries, "List of web.xml's" );
        PreConditionException.validateEqualTo( entries.size(), 1, "Number of xml's" );

        final URL webXmlURL = entries.get( 0 );
        LOG.debug( "Parsing a web application from [" + webXmlURL + "]" );
        InputStream is = null;
        try
        {
            is = webXmlURL.openStream();
            final WebApp webApp = m_parser.parse( is );
            if( webApp != null )
            {
                LOG.debug( "Parsed web app [" + webApp + "]" );
                webApp.setBundle( bundle );
                // set the context name as first looking for a manifest entry named Webapp-Context
                // if not set use bundle symbolic name
                String contextName = (String) bundle.getHeaders().get( "Webapp-Context" );
                if( contextName == null )
                {
                    LOG.debug( "No 'Webapp-Context' manifest attribute specified" );

                    final String symbolicName = bundle.getSymbolicName();
                    if( symbolicName == null )
                    {
                        contextName = String.valueOf( bundle.getBundleId() );
                        LOG.debug( String.format( "Using bundle id [%s] as context name", contextName ) );
                    }
                    else
                    {
                        contextName = symbolicName;
                        LOG.debug( String.format( "Using bundle symbolic name [%s] as context name", contextName ) );
                    }
                }
                if( "/".equals( contextName.trim() ) )
                {
                    contextName = "";
                }

                LOG.info( String.format( "Using [%s] as web application context name", contextName ) );

                webApp.setContextName( contextName );
                m_publisher.publish( webApp );
                m_publishedWebApps.put( webXmlURL, webApp );
            }
        }
        catch( IOException ignore )
        {
            LOG.error( "Could not parse web.xml", ignore );
        }
        finally
        {
            if( is != null )
            {
                try
                {
                    is.close();
                }
                catch( IOException ignore )
                {
                    // just ignore
                }
            }
        }
    }

    /**
     * Unregisters registered web app once that the bundle that contains the web.xml gets stopped.
     * The list of xb.xml's is expected to contain only one entry (only first will be used).
     *
     * @throws NullArgumentException if bundle or list of web xmls is null
     * @throws PreConditionException if the list of web xmls is empty or more then one xml
     * @see BundleObserver#removingEntries(Bundle,List)
     */
    public void removingEntries( final Bundle bundle, final List<URL> entries )
    {
        NullArgumentException.validateNotNull( bundle, "Bundle" );
        NullArgumentException.validateNotNull( entries, "List of web.xml's" );
        PreConditionException.validateEqualTo( 1, entries.size(), "Number of xml's" );

        final URL webXmlURL = entries.get( 0 );
        LOG.debug( "Unregistering web application parsed from [" + webXmlURL + "]" );
        final WebApp toUnpublish = m_publishedWebApps.get( webXmlURL );
        if( toUnpublish != null )
        {
            m_publisher.unpublish( toUnpublish );
        }
        m_publishedWebApps.remove( webXmlURL );
    }
}
