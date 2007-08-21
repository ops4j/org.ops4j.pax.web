/*
 * Copyright 2007 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.internal;

import javax.servlet.Servlet;
import static org.easymock.EasyMock.*;
import org.junit.Test;
import static org.ops4j.pax.web.extender.internal.Capture.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;

public class ServletTrackerTest
{

    /**
     * Tests that we cannot make a tracker without a bundle context.
     */
    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullBundleContext()
    {
        new ServletTracker( null );
    }

    /**
     * Checks that servltest get recorded even if there is no available service, and after the service gets available
     * it registers the already tracked servlets.
     */
    @Test
    public void servletAvailableWithUnavailableHttpService()
        throws Exception
    {
        Servlet servlet = createMock( Servlet.class );
        BundleContext context = createMock( BundleContext.class );
        Filter filter = createMock( Filter.class );
        ServiceReference reference = createMock( ServiceReference.class );
        HttpService service = createMock( HttpService.class );

        expect( context.createFilter( "(objectClass=" + Servlet.class.getName() + ")" ) ).andReturn( filter );
        Capture<ServiceListener> listenerCapture = new Capture<ServiceListener>();
        context.addServiceListener( capture( listenerCapture ), (String) notNull() );
        expect( context.getServiceReferences( Servlet.class.getName(), null ) ).andReturn( null );
        expect( reference.getProperty( "alias" ) ).andReturn( "anAlias" );
        expect( context.getService( reference ) ).andReturn( servlet );

        replay( servlet, context, filter, reference );
        ServletTracker tracker = new ServletTracker( context );
        tracker.open();
        listenerCapture.getCaptured().serviceChanged( new ServiceEvent( ServiceEvent.REGISTERED, reference ) );
        verify( servlet, context, filter, reference );

        service.registerServlet( "anAlias", servlet, null, null );
        replay( service );
        tracker.available( service );
        verify( service );
    }

    /**
     * Test that if a http service is available it will register the servlets when the servlet is tracked.
     */
    @Test
    public void servletAvailableWithAvailableHttpService()
        throws Exception
    {
        Servlet servlet = createMock( Servlet.class );
        BundleContext context = createMock( BundleContext.class );
        Filter filter = createMock( Filter.class );
        ServiceReference reference = createMock( ServiceReference.class );
        HttpService service = createMock( HttpService.class );

        expect( context.createFilter( "(objectClass=" + Servlet.class.getName() + ")" ) ).andReturn( filter );
        Capture<ServiceListener> listenerCapture = new Capture<ServiceListener>();
        context.addServiceListener( capture( listenerCapture ), (String) notNull() );
        expect( context.getServiceReferences( Servlet.class.getName(), null ) ).andReturn( null );
        expect( reference.getProperty( "alias" ) ).andReturn( "anAlias" );
        expect( context.getService( reference ) ).andReturn( servlet );
        service.registerServlet( "anAlias", servlet, null, null );

        replay( servlet, context, filter, reference, service );
        ServletTracker tracker = new ServletTracker( context );
        tracker.open();
        tracker.available( service );
        listenerCapture.getCaptured().serviceChanged( new ServiceEvent( ServiceEvent.REGISTERED, reference ) );
        verify( servlet, context, filter, reference, service );
    }

    /**
     * test that a servlet that was tracked and becomes unavailable it will be just removed without any problem that
     * the http service was not available.
     */
    @Test
    public void servletUnAvailableWithUnavailableHttpService()
        throws Exception
    {
        Servlet servlet = createMock( Servlet.class );
        BundleContext context = createMock( BundleContext.class );
        Filter filter = createMock( Filter.class );
        ServiceReference reference = createMock( ServiceReference.class );

        expect( context.createFilter( "(objectClass=" + Servlet.class.getName() + ")" ) ).andReturn( filter );
        Capture<ServiceListener> listenerCapture = new Capture<ServiceListener>();
        context.addServiceListener( capture( listenerCapture ), (String) notNull() );
        expect( context.getServiceReferences( Servlet.class.getName(), null ) ).andReturn( null );
        expect( reference.getProperty( "alias" ) ).andReturn( "anAlias" );
        expect( context.getService( reference ) ).andReturn( servlet );
        expect( context.ungetService( reference ) ).andReturn( true );

        replay( servlet, context, filter, reference );
        ServletTracker tracker = new ServletTracker( context );
        tracker.open();
        listenerCapture.getCaptured().serviceChanged( new ServiceEvent( ServiceEvent.REGISTERED, reference ) );
        listenerCapture.getCaptured().serviceChanged( new ServiceEvent( ServiceEvent.UNREGISTERING, reference ) );
        verify( servlet, context, filter, reference );
    }

    /**
     * Tests that if thare are servlets tracked, those servlets will be registered only with one service even if
     * there are more http services available.
     */
    @Test
    public void servletAvailableWithMoreHttpServices()
        throws Exception
    {
        Servlet servlet = createMock( Servlet.class );
        BundleContext context = createMock( BundleContext.class );
        Filter filter = createMock( Filter.class );
        ServiceReference reference = createMock( ServiceReference.class );
        HttpService service1 = createMock( HttpService.class );
        HttpService service2 = createMock( HttpService.class );

        expect( context.createFilter( "(objectClass=" + Servlet.class.getName() + ")" ) ).andReturn( filter );
        Capture<ServiceListener> listenerCapture = new Capture<ServiceListener>();
        context.addServiceListener( capture( listenerCapture ), (String) notNull() );
        expect( context.getServiceReferences( Servlet.class.getName(), null ) ).andReturn( null );
        expect( reference.getProperty( "alias" ) ).andReturn( "anAlias" );
        expect( context.getService( reference ) ).andReturn( servlet );

        replay( servlet, context, filter, reference );
        ServletTracker tracker = new ServletTracker( context );
        tracker.open();
        listenerCapture.getCaptured().serviceChanged( new ServiceEvent( ServiceEvent.REGISTERED, reference ) );
        verify( servlet, context, filter, reference );

        service1.registerServlet( "anAlias", servlet, null, null );
        replay( service1, service2 );
        tracker.available( service1 );
        tracker.available( service2 );
        verify( service1, service2 );
    }

    /**
     * Tests that if there are servlets tracked and an http service gets registred the servlets are registered with the
     * service. Once the service is gone the servlets are unregistred and if a second service becomes available the
     * tracked servlets gets registered with the new available http service.
     */
    @Test
    public void servletAvailableWithJumpingHttpServices()
        throws Exception
    {
        Servlet servlet = createMock( Servlet.class );
        BundleContext context = createMock( BundleContext.class );
        Filter filter = createMock( Filter.class );
        ServiceReference reference = createMock( ServiceReference.class );
        HttpService service1 = createMock( HttpService.class );
        HttpService service2 = createMock( HttpService.class );

        expect( context.createFilter( "(objectClass=" + Servlet.class.getName() + ")" ) ).andReturn( filter );
        Capture<ServiceListener> listenerCapture = new Capture<ServiceListener>();
        context.addServiceListener( capture( listenerCapture ), (String) notNull() );
        expect( context.getServiceReferences( Servlet.class.getName(), null ) ).andReturn( null );
        expect( reference.getProperty( "alias" ) ).andReturn( "anAlias" );
        expect( context.getService( reference ) ).andReturn( servlet );

        replay( servlet, context, filter, reference );
        ServletTracker tracker = new ServletTracker( context );
        tracker.open();
        listenerCapture.getCaptured().serviceChanged( new ServiceEvent( ServiceEvent.REGISTERED, reference ) );
        verify( servlet, context, filter, reference );

        service1.registerServlet( "anAlias", servlet, null, null );
        service1.unregister( "anAlias" );
        service2.registerServlet( "anAlias", servlet, null, null );
        replay( service1, service2 );
        tracker.available( service1 );
        tracker.unavailable( service1 );
        tracker.available( service2 );
        verify( service1, service2 );
    }

    /**
     * Tests that tracked servlets are unregistered once they were registered if the http service that was available
     * goes away.
     */
    @Test
    public void servletAvailableWithAvailableHttpServiceAndUnregisterOfHttpService()
        throws Exception
    {
        Servlet servlet = createMock( Servlet.class );
        BundleContext context = createMock( BundleContext.class );
        Filter filter = createMock( Filter.class );
        ServiceReference reference = createMock( ServiceReference.class );
        HttpService service = createMock( HttpService.class );

        expect( context.createFilter( "(objectClass=" + Servlet.class.getName() + ")" ) ).andReturn( filter );
        Capture<ServiceListener> listenerCapture = new Capture<ServiceListener>();
        context.addServiceListener( capture( listenerCapture ), (String) notNull() );
        expect( context.getServiceReferences( Servlet.class.getName(), null ) ).andReturn( null );
        expect( reference.getProperty( "alias" ) ).andReturn( "anAlias" );
        expect( context.getService( reference ) ).andReturn( servlet );
        service.registerServlet( "anAlias", servlet, null, null );
        service.unregister( "anAlias" );

        replay( servlet, context, filter, reference, service );
        ServletTracker tracker = new ServletTracker( context );
        tracker.open();
        tracker.available( service );
        listenerCapture.getCaptured().serviceChanged( new ServiceEvent( ServiceEvent.REGISTERED, reference ) );
        tracker.unavailable( service );
        verify( servlet, context, filter, reference, service );
    }

    /**
     * Tests that there are tracked servlets that were registered with one http service, the servlets are not
     * unregistred if a different http service becomes unavailable.
     */
    @Test
    public void servletAvailableWithAvailableHttpServiceAndUnregisterOfDifferentHttpService()
        throws Exception
    {
        Servlet servlet = createMock( Servlet.class );
        BundleContext context = createMock( BundleContext.class );
        Filter filter = createMock( Filter.class );
        ServiceReference reference = createMock( ServiceReference.class );
        HttpService service1 = createMock( HttpService.class );
        HttpService service2 = createMock( HttpService.class );

        expect( context.createFilter( "(objectClass=" + Servlet.class.getName() + ")" ) ).andReturn( filter );
        Capture<ServiceListener> listenerCapture = new Capture<ServiceListener>();
        context.addServiceListener( capture( listenerCapture ), (String) notNull() );
        expect( context.getServiceReferences( Servlet.class.getName(), null ) ).andReturn( null );
        expect( reference.getProperty( "alias" ) ).andReturn( "anAlias" );
        expect( context.getService( reference ) ).andReturn( servlet );
        service1.registerServlet( "anAlias", servlet, null, null );

        replay( servlet, context, filter, reference, service1, service2 );
        ServletTracker tracker = new ServletTracker( context );
        tracker.open();
        tracker.available( service1 );
        listenerCapture.getCaptured().serviceChanged( new ServiceEvent( ServiceEvent.REGISTERED, reference ) );
        tracker.unavailable( service2 );
        verify( servlet, context, filter, reference, service1, service2 );
    }

    /**
     * Tests that nothing happens if there are tracked servlets that were not registred with any http service and an
     * http service is gone.
     */
    @Test
    public void servletAvailableWithUnAvailableHttpServiceAndUnregisterOfHttpService()
        throws Exception
    {
        Servlet servlet = createMock( Servlet.class );
        BundleContext context = createMock( BundleContext.class );
        Filter filter = createMock( Filter.class );
        ServiceReference reference = createMock( ServiceReference.class );
        HttpService service = createMock( HttpService.class );

        expect( context.createFilter( "(objectClass=" + Servlet.class.getName() + ")" ) ).andReturn( filter );
        Capture<ServiceListener> listenerCapture = new Capture<ServiceListener>();
        context.addServiceListener( capture( listenerCapture ), (String) notNull() );
        expect( context.getServiceReferences( Servlet.class.getName(), null ) ).andReturn( null );
        expect( reference.getProperty( "alias" ) ).andReturn( "anAlias" );
        expect( context.getService( reference ) ).andReturn( servlet );

        replay( servlet, context, filter, reference, service );
        ServletTracker tracker = new ServletTracker( context );
        tracker.open();
        listenerCapture.getCaptured().serviceChanged( new ServiceEvent( ServiceEvent.REGISTERED, reference ) );
        tracker.unavailable( service );
        verify( servlet, context, filter, reference, service );
    }

}
