/*
 * Copyright 2007 Alin Dreghiciu, Achim Nierbeck.
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

import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.pax.swissbox.extender.BundleURLScanner;
import org.ops4j.pax.swissbox.extender.BundleWatcher;
import org.ops4j.pax.web.extender.war.internal.parser.dom.DOMWebXmlParser;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * WAR Extender activator.<br/>
 * Starts an web.xml watcher on installed bundles. When a bundle containing "WEB-INF/web.xml" is started the web.xml
 * will get parsed and an web app will be created. On stop of bundle containing web.xml or stop of this bundle , the
 * created web app will be unregistered.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, December 27, 2007
 */
public class Activator
    implements BundleActivator
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( Activator.class );

    /**
     * Bundle watcher of web.xml.
     */
    private BundleWatcher<URL> m_webXmlWatcher;

	private ServiceTracker eventServiceTracker;

	private ServiceTracker logServiceTracker;

	private WebXmlObserver webXmlObserver;

	private BundleContext bundleContext;

	private ServiceTracker webListenerTracker;

	private ScheduledExecutorService executors;

	private WebEventDispatcher webEventDispatcher;

    /**
     * Starts an web.xml watcher on installed bundles.
     *
     * @see BundleActivator#start(BundleContext)
     */
    public void start( final BundleContext bundleContext )
        throws Exception
    {
        LOG.debug( "Pax Web WAR Extender - Starting" );
        this.bundleContext = bundleContext;
        
		executors = Executors.newScheduledThreadPool(3, new ThreadFactory() {

			private final AtomicInteger count = new AtomicInteger();
			
			public Thread newThread(Runnable r) {
				final Thread t = Executors.defaultThreadFactory().newThread(r);
		        t.setName("WebListenerExecutor" + ": " + count.incrementAndGet());
		        t.setDaemon(true);
		        return t;
			}
		});
		
		webEventDispatcher = new WebEventDispatcher(bundleContext, executors);
        
		Filter filterEvent = bundleContext.createFilter("(objectClass=org.osgi.service.event.EventAdmin)");
		eventServiceTracker = new ServiceTracker(bundleContext, filterEvent, new EventServiceCustomizer());
		eventServiceTracker.open();
		
		Filter filterLog = bundleContext.createFilter("(objectClass=org.osgi.service.log.LogService)");
		logServiceTracker = new ServiceTracker(bundleContext, filterLog, new LogServiceCustomizer());
		logServiceTracker.open();
		
		 
	        webXmlObserver = new WebXmlObserver(
			    new DOMWebXmlParser(),
			    new WebAppPublisher(),
			    webEventDispatcher,
			    bundleContext
			);
			m_webXmlWatcher =
	            new BundleWatcher<URL>(
	                bundleContext,
	                new BundleURLScanner(
                	"Webapp-Root",
                	null,
                	null,
	                    "WEB-INF/",
	                    "web.xml",
                    true // do recurse
	                ),
	                webXmlObserver
	            );
	        m_webXmlWatcher.start();
        LOG.debug( "Pax Web WAR Extender - Started" );
    }

    /**
     * Stops the watcher, fact that will trigger that all registered web appas to be unregistered.
     *
     * @see BundleActivator#stop(BundleContext)
     */
    public void stop( final BundleContext bundleContext )
        throws Exception
    {
        LOG.debug( "Pax Web WAR Extender - Stopping" );
        // Stop the bundle watcher.
        // This will result in unpublish of each web application that was registered during the lifetime of
        // bundle watcher.
        if( m_webXmlWatcher != null )
        {
            m_webXmlWatcher.stop();
            m_webXmlWatcher = null;
        }
        eventServiceTracker.close();
        logServiceTracker.close();
        webEventDispatcher.destroy();
        executors.shutdown();
        LOG.debug( "Pax Web WAR Extender - Stopped" );
    }
    
    private class LogServiceCustomizer implements ServiceTrackerCustomizer {

    	public Object addingService(ServiceReference reference) {
    		Object logService = bundleContext.getService(reference);
    		webEventDispatcher.setLogService(logService);
    		return logService;
    	}
    	
		public void modifiedService(ServiceReference reference, Object service) {
		}

		public void removedService(ServiceReference reference, Object service) {
			webEventDispatcher.setLogService(null);
			bundleContext.ungetService(reference);
		}

	}
    
    private class EventServiceCustomizer implements ServiceTrackerCustomizer {

		public Object addingService(ServiceReference reference) {
			Object eventService = bundleContext.getService(reference);
			webEventDispatcher.setEventAdminService(eventService);
			return eventService;
		}

		public void modifiedService(ServiceReference reference, Object service) {
		}

		public void removedService(ServiceReference reference, Object service) {
			webEventDispatcher.setEventAdminService(null);
			bundleContext.ungetService(reference);
		}
    	
    }

}
