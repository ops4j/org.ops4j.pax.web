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

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.swissbox.core.BundleUtils;
import org.ops4j.pax.swissbox.tracker.ReplaceableService;
import org.ops4j.pax.swissbox.tracker.ReplaceableServiceListener;
import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.util.WebContainerUtils;
import org.ops4j.pax.web.service.WebAppDependencyHolder;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.ServletContextManager;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publish/Unpublish a web application.
 *
 * @author Alin Dreghiciu
 * @author Marc Klinger - mklinger[at]nightlabs[dot]de
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
    private final Map<WebApp, ServiceTracker> m_webApps;

    /**
     * Creates a new web app publisher.
     */
    WebAppPublisher()
    {
        m_webApps = Collections.synchronizedMap( new HashMap<WebApp, ServiceTracker>() );
    }

    /**
     * Publish a web application.
     *
     * @param webApp web application to be published.
     *
     * @throws NullArgumentException if web app is null
     */
    public void publish( final WebApp webApp, final WebEventDispatcher eventDispatcher, BundleContext bundleContext )
    {
        NullArgumentException.validateNotNull( webApp, "Web app" );
        LOG.debug( "Publishing web application [" + webApp + "]" );
        final BundleContext webAppBundleContext = BundleUtils.getBundleContext( webApp.getBundle() );
        if( webAppBundleContext != null )
        {
        	try {
				Filter filter = webAppBundleContext.createFilter(String.format("(&(objectClass=%s)(bundle.id=%d))", 
					WebAppDependencyHolder.class.getName(), webApp.getBundle().getBundleId()));
				ServiceTracker dependencyTracker = new ServiceTracker(webAppBundleContext, filter, new WebAppDependencyListener( webApp, eventDispatcher, bundleContext));
				dependencyTracker.open();
	            m_webApps.put( webApp, dependencyTracker );
			}
			catch (InvalidSyntaxException exc) {
				throw new IllegalArgumentException(exc);
			}
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
        final ServiceTracker httpServiceTracker = m_webApps.get( webApp );
        if( httpServiceTracker != null )
        {
            m_webApps.remove( webApp );
            // if the bundle is not active then do nothing as http service already released all the web app
            if( Bundle.ACTIVE == webApp.getBundle().getState() )
            {
                httpServiceTracker.close();
            }
        }
    }

    /**
     * Http Service listener that will register/unregister the web app as soon as an http service becomes
     * available/unavailable.
     */
    public static class WebAppDependencyListener
        implements ServiceTrackerCustomizer
    {

        /**
         * Web app to be registered.
         */
        private final WebApp m_webApp;
        
        private final WebEventDispatcher m_eventDispatcher;
        
        private BundleContext m_bundleContext;
        
        /**
         * Http service in use.
         */
        private HttpService m_httpService;
        
        private WebAppDependencyHolder dependencyHolder;
        

        /**
         * Creates a new http service listener.
         *
         * @param webApp web app to be registered
         *
         * @throws NullArgumentException if web app is null
         */
        WebAppDependencyListener( final WebApp webApp, WebEventDispatcher eventDispatcher, BundleContext bundleContext )
        {
            NullArgumentException.validateNotNull( webApp, "Web app" );
            m_webApp = webApp;
            m_eventDispatcher = eventDispatcher;
            m_bundleContext = bundleContext;
        }

        /**
         * In case that the http service changes, first unregister the web app from the old one (if not null) and then
         * register the web app with the new service.
         *
         * @see ReplaceableServiceListener#serviceChanged(Object, Object)
         */
        public synchronized void modifiedService( ServiceReference reference, Object service)
        {
            unregister();
            dependencyHolder = ((WebAppDependencyHolder)service); 
            m_httpService = dependencyHolder.getHttpService();
            register();
        }

        /**
         * Registers a web app with current http service, if any.
         */
		private void register() {
			if (m_httpService != null) {
				LOG.debug("Registering web application [" + m_webApp + "] from http service ["
					+ m_httpService + "]");
				if (WebContainerUtils.webContainerAvailable(m_httpService)) {
					m_webApp.accept(new RegisterWebAppVisitorWC(dependencyHolder));
				}
				else {
					m_webApp.accept(new RegisterWebAppVisitorHS(m_httpService));
				}

				/*
				 * In Pax Web 2, the servlet context was started on creation, implicitly on
				 * registering the first servlet.
				 * 
				 * In Pax Web 3, we support extensions registering a servlet container initializer
				 * to customize the servlet context, e.g. by decorating servlets. For decorators to
				 * have any effect, the servlet context must not be started when the decorators are
				 * registered.
				 * 
				 * At this point, the servlet context is fully configured, so this is the right time
				 * to start it.
				 */
				ServletContextManager.startContext("/" + m_webApp.getContextName());

				m_webApp.setDeploymentState(WebApp.DEPLOYED_STATE);
				m_eventDispatcher.webEvent(new WebEvent(WebEvent.DEPLOYED, "/"
					+ m_webApp.getContextName(), m_webApp.getBundle(), m_bundleContext.getBundle(),
					m_httpService, m_webApp.getHttpContext()));
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

		@Override
		public Object addingService(ServiceReference reference) {
			WebAppDependencyHolder service = (WebAppDependencyHolder) m_bundleContext.getService(reference);
			dependencyHolder = service;
            m_httpService = service.getHttpService();
            register();
            return service;
		}

		@Override
		public void removedService(ServiceReference reference, Object service) {
			unregister();
		}

    }

}
