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
package org.ops4j.pax.web.extender.whiteboard.internal;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.FilterMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.FilterTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.HttpContextMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.HttpContextTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.JspMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ListenerMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ListenerTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ResourceMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ServletMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ServletTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.WelcomeFileMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ErrorPageMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.util.WebContainerUtils;

/**
 * Activates the pax web extender.
 *
 * @author Alin Dreghiciu
 * @since 0.1.0, August 21, 2007
 */
public class Activator
    implements BundleActivator
{

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger( Activator.class );

    /**
     * Extender context.
     */
    private ExtenderContext m_extenderContext;
    /**
     * List of service trackers.
     */
    private List<ServiceTracker> m_trackers;

    /**
     * @see BundleActivator#start(BundleContext)
     */
    public void start( final BundleContext bundleContext )
        throws Exception
    {
        m_extenderContext = new ExtenderContext();
        m_extenderContext.open( bundleContext );
        m_trackers = new ArrayList<ServiceTracker>();

        trackHttpContexts( bundleContext );
        trackServlets( bundleContext );
        trackResources( bundleContext );
        if( WebContainerUtils.WEB_CONATAINER_AVAILABLE )
        {
            trackFilters( bundleContext );
            trackListeners( bundleContext );
            trackJspMappings( bundleContext );
            trackErrorPages( bundleContext );
            trackWelcomeFiles( bundleContext );
        }
        else
        {
            LOG.warn( "Filters tracking has been disabled as the WebContainer (Pax Web) is not available" );
            LOG.warn( "Event Listeners tracking has been disabled as the WebContainer (Pax Web) is not available" );
            LOG.warn( "JSP mappings tracking has been disabled as the WebContainer (Pax Web) is not available" );
        }
        LOG.debug( "Pax Web Extender started" );
    }

    /**
     * @see BundleActivator#stop(BundleContext)
     */
    public void stop( final BundleContext bundleContext )
        throws Exception
    {
        for( ServiceTracker tracker : m_trackers )
        {
            tracker.close();
        }
        m_trackers = null;
        m_extenderContext.close( bundleContext );
        LOG.debug( "Pax Web Extender stopped" );
    }

    /**
     * Track http contexts.
     *
     * @param bundleContext a bundle context
     */
    private void trackHttpContexts( final BundleContext bundleContext )
    {
        final ServiceTracker httpContextTracker = new HttpContextTracker(
            m_extenderContext,
            bundleContext
        );
        httpContextTracker.open();
        m_trackers.add( 0, httpContextTracker );

        final ServiceTracker httpContextMappingTracker = new HttpContextMappingTracker(
            m_extenderContext,
            bundleContext
        );
        httpContextMappingTracker.open();
        m_trackers.add( 0, httpContextMappingTracker );
    }

    /**
     * Track servlets.
     *
     * @param bundleContext a bundle context
     */
    private void trackServlets( final BundleContext bundleContext )
    {
        final ServiceTracker servletTracker = new ServletTracker(
            m_extenderContext,
            bundleContext
        );
        servletTracker.open();
        m_trackers.add( 0, servletTracker );

        final ServiceTracker servletMappingTracker = new ServletMappingTracker(
            m_extenderContext,
            bundleContext
        );
        servletMappingTracker.open();
        m_trackers.add( 0, servletMappingTracker );
    }

    /**
     * Track resources.
     *
     * @param bundleContext a bundle context
     */
    private void trackResources( final BundleContext bundleContext )
    {
        final ServiceTracker resourceMappingTracker = new ResourceMappingTracker(
            m_extenderContext,
            bundleContext
        );
        resourceMappingTracker.open();
        m_trackers.add( 0, resourceMappingTracker );
    }

    /**
     * Track filters.
     *
     * @param bundleContext a bundle context
     */
    private void trackFilters( final BundleContext bundleContext )
    {
        final ServiceTracker filterTracker = new FilterTracker(
            m_extenderContext,
            bundleContext
        );
        filterTracker.open();
        m_trackers.add( 0, filterTracker );

        final ServiceTracker filterMappingTracker = new FilterMappingTracker(
            m_extenderContext,
            bundleContext
        );
        filterMappingTracker.open();
        m_trackers.add( 0, filterMappingTracker );
    }

    /**
     * Track listeners.
     *
     * @param bundleContext a bundle context
     */
    private void trackListeners( final BundleContext bundleContext )
    {
        final ServiceTracker listenerTracker = new ListenerTracker(
            m_extenderContext,
            bundleContext
        );
        listenerTracker.open();
        m_trackers.add( 0, listenerTracker );

        final ServiceTracker listenerMappingTracker = new ListenerMappingTracker(
            m_extenderContext,
            bundleContext
        );
        listenerMappingTracker.open();
        m_trackers.add( 0, listenerMappingTracker );
    }

    /**
     * Track jsps.
     *
     * @param bundleContext a bundle context
     */
    private void trackJspMappings( final BundleContext bundleContext )
    {
        final ServiceTracker jspMappingTracker = new JspMappingTracker(
            m_extenderContext,
            bundleContext
        );
        jspMappingTracker.open();
        m_trackers.add( 0, jspMappingTracker );
    }

    /**
     * Track welcome files
     * @param bundleContext
     */
    private void trackWelcomeFiles( final BundleContext bundleContext )
    {
        final ServiceTracker welcomeFileTracker = new WelcomeFileMappingTracker(
            m_extenderContext,
            bundleContext
        );
        welcomeFileTracker.open(  );
        m_trackers.add( 0, welcomeFileTracker );
    }

    /**
     * Track error pages
     * @param bundleContext
     */
    private void trackErrorPages( final BundleContext bundleContext )
    {
        final ServiceTracker errorPagesTracker = new ErrorPageMappingTracker(
            m_extenderContext,
            bundleContext
        );
        errorPagesTracker.open();
        m_trackers.add( 0, errorPagesTracker );
    }

}
