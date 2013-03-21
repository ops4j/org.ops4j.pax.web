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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.swissbox.core.BundleUtils;
import org.ops4j.pax.swissbox.tracker.ReplaceableService;
import org.ops4j.pax.swissbox.tracker.ReplaceableServiceListener;
import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.util.WebContainerUtils;
import org.ops4j.pax.web.service.WebContainer;

/**
 * Publish/Unpublish a web application.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, December 27, 2007
 */
class WebAppPublisher
{

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger( WebAppPublisher.class );
    /**
     * In use web apps.
     */
    private final Map<WebApp, ReplaceableService<HttpService>> m_webApps;

    /**
     * Creates a new web app publisher.
     */
    WebAppPublisher()
    {
        m_webApps = Collections.synchronizedMap( new HashMap<WebApp, ReplaceableService<HttpService>>() );
    }

    /**
     * Publish a web application.
     *
     * @param webApp web application to be published.
     *
     * @throws NullArgumentException if web app is null
     */
    public void publish( final WebApp webApp )
    {
        NullArgumentException.validateNotNull( webApp, "Web app" );
        LOG.debug( "Publishing web application [" + webApp + "]" );
        final BundleContext bundleContext = BundleUtils.getBundleContext( webApp.getBundle() );
        if( bundleContext != null )
        {
            try
            {
                if( webApp.getBundle().loadClass( HttpService.class.getName() ) != HttpService.class )
                {
                    LOG.warn("WebApp for bundle [" + webApp.getBundle() + "] is not compatible with the current extender");
                    return;
                }
            }
            catch (ClassNotFoundException e)
            {
                // Ignore, we hope it's safe
            }
            final ReplaceableService<HttpService> httpServiceTracker = new ReplaceableService<HttpService>(
                bundleContext,
                HttpService.class,
                new HttpServiceListener( webApp )
            );
            httpServiceTracker.start();
            m_webApps.put( webApp, httpServiceTracker );
        }
        else
        {
            LOG.warn( "Bundle context could not be discovered for bundle [" + webApp.getBundle() + "]"
                      + "Skipping publishing of web application [" + webApp + "]"
            );
        }
    }

    /**
     * Unpublish a web application.
     *
     * @param webApp web aplication to be unpublished
     *
     * @throws NullArgumentException if web app is null
     */
    public void unpublish( final WebApp webApp )
    {
        NullArgumentException.validateNotNull( webApp, "Web app" );
        LOG.debug( "Unpublishing web application [" + webApp + "]" );
        final ReplaceableService<HttpService> httpServiceTracker = m_webApps.get( webApp );
        if( httpServiceTracker != null )
        {
            m_webApps.remove( webApp );
            // if the bundle is not active then do nothing as http service already released all the web app
            if( Bundle.ACTIVE == webApp.getBundle().getState() )
            {
                httpServiceTracker.stop();
            }
        }
    }

    /**
     * Http Service listener that will register/unregister the web app as soon as an http service becomes
     * available/unavailable.
     */
    public static class HttpServiceListener
        implements ReplaceableServiceListener<HttpService>
    {

        /**
         * Web app to be registered.
         */
        private final WebApp m_webApp;
        /**
         * Http service in use.
         */
        private HttpService m_httpService;

        /**
         * Creates a new http service listener.
         *
         * @param webApp web app to be registered
         *
         * @throws NullArgumentException if web app is null
         */
        HttpServiceListener( final WebApp webApp )
        {
            NullArgumentException.validateNotNull( webApp, "Web app" );
            m_webApp = webApp;
        }

        /**
         * In case that the http service changes, first unregister the web app from the old one (if not null) and then
         * register the web app with the new service.
         *
         * @see ReplaceableServiceListener#serviceChanged(Object, Object)
         */
        public synchronized void serviceChanged( final HttpService oldHttpService, final HttpService newHttpService )
        {
            unregister();
            m_httpService = newHttpService;
            register();
        }

        /**
         * Registers a web app with current http service, if any.
         */
        private void register()
        {
            if( m_httpService != null )
            {
                LOG.debug(
                    "Registering web application [" + m_webApp + "] from http service [" + m_httpService + "]"
                );
                if( WebContainerUtils.webContainerAvailable( m_httpService ) )
                {
                    m_webApp.accept( new RegisterWebAppVisitorWC( (WebContainer) m_httpService ) );
                }
                else
                {
                    m_webApp.accept( new RegisterWebAppVisitorHS( m_httpService ) );
                }
            }
        }

        /**
         * Unregisters a web app from current http service, if any.
         */
        private void unregister()
        {
            if( m_httpService != null )
            {
                LOG.debug(
                    "Unregistering web application [" + m_webApp + "] from http service [" + m_httpService + "]"
                );
                if( WebContainerUtils.webContainerAvailable( m_httpService ) )
                {
                    m_webApp.accept( new UnregisterWebAppVisitorWC( (WebContainer) m_httpService ) );
                }
                else
                {
                    m_webApp.accept( new UnregisterWebAppVisitorHS( m_httpService ) );
                }
            }
        }

    }

}
