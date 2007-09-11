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

import static org.easymock.EasyMock.*;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import static org.ops4j.pax.web.extender.internal.Capture.capture;

public class HttpServiceTrackerTest
{

    /**
     * Tests that we can not make a tracker without bundle context.
     */
    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullBundleContext()
        throws Exception
    {
        new HttpServiceTracker( null, new HttpServiceListener[]{ createMock( HttpServiceListener.class ) } );
    }

    /**
     * Tests that we can not make a tracker without an array of listeners (empty allowed).
     */
    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullListeners()
        throws Exception
    {
        new HttpServiceTracker( createMock( BundleContext.class ), null );
    }

    /**
     * Tests that listenres get notified if an http service becomes available.
     */
    @Test
    public void serviceAvailable()
        throws Exception
    {
        HttpService service = createMock( HttpService.class );
        BundleContext context = createMock( BundleContext.class );
        HttpServiceListener listener = createMock( HttpServiceListener.class );
        Filter filter = createMock( Filter.class );
        ServiceReference reference = createMock( ServiceReference.class );

        expect( context.createFilter( "(objectClass=" + HttpService.class.getName() + ")" ) ).andReturn( filter );
        Capture<ServiceListener> listenerCapture = new Capture<ServiceListener>();
        context.addServiceListener( capture( listenerCapture ), (String) notNull() );
        expect( context.getServiceReferences( HttpService.class.getName(), null ) ).andReturn( null );
        expect( context.getService( reference ) ).andReturn( service );
        listener.available( service );

        replay( service, context, listener, filter, reference );
        HttpServiceTracker tracker = new HttpServiceTracker( context, new HttpServiceListener[]{ listener } );
        tracker.open();
        listenerCapture.getCaptured().serviceChanged( new ServiceEvent( ServiceEvent.REGISTERED, reference ) );
        verify( service, context, listener, filter, reference );
    }

    /**
     * Tests that listeners gets notified that the http service is gone and there is not another service tracked.
     */
    @Test
    public void serviceUnavailableAndNoReplacement()
        throws Exception
    {
        HttpService service = createMock( HttpService.class );
        BundleContext context = createMock( BundleContext.class );
        HttpServiceListener listener = createMock( HttpServiceListener.class );
        Filter filter = createMock( Filter.class );
        ServiceReference reference = createMock( ServiceReference.class );

        expect( context.createFilter( "(objectClass=" + HttpService.class.getName() + ")" ) ).andReturn( filter );
        Capture<ServiceListener> listenerCapture = new Capture<ServiceListener>();
        context.addServiceListener( capture( listenerCapture ), (String) notNull() );
        expect( context.getServiceReferences( HttpService.class.getName(), null ) ).andReturn( null );
        expect( context.getService( reference ) ).andReturn( service );
        listener.available( service );
        listener.unavailable( service );
        expect( context.ungetService( reference ) ).andReturn( true );

        replay( service, context, listener, filter, reference );
        HttpServiceTracker tracker = new HttpServiceTracker( context, new HttpServiceListener[]{ listener } );
        tracker.open();
        listenerCapture.getCaptured().serviceChanged( new ServiceEvent( ServiceEvent.REGISTERED, reference ) );
        listenerCapture.getCaptured().serviceChanged( new ServiceEvent( ServiceEvent.UNREGISTERING, reference ) );
        verify( service, context, listener, filter, reference );
    }

    /**
     * Tests that listeners gets notified that the http service is gone and because there was another service that was
     * tracked in the meantime, the listeners get's notified that a new service becomes available.
     */
    @Test
    public void serviceUnavailableWithReplacement()
        throws Exception
    {
        HttpService service1 = createMock( HttpService.class );
        HttpService service2 = createMock( HttpService.class );
        BundleContext context = createMock( BundleContext.class );
        HttpServiceListener listener = createMock( HttpServiceListener.class );
        Filter filter = createMock( Filter.class );
        ServiceReference reference1 = createMock( ServiceReference.class );
        ServiceReference reference2 = createMock( ServiceReference.class );

        expect( context.createFilter( "(objectClass=" + HttpService.class.getName() + ")" ) ).andReturn( filter );
        Capture<ServiceListener> listenerCapture = new Capture<ServiceListener>();
        context.addServiceListener( capture( listenerCapture ), (String) notNull() );
        expect( context.getServiceReferences( HttpService.class.getName(), null ) ).andReturn( null );
        expect( context.getService( reference1 ) ).andReturn( service1 );
        expect( context.getService( reference2 ) ).andReturn( service2 );
        listener.available( service1 );
        listener.unavailable( service1 );
        expect( context.ungetService( reference1 ) ).andReturn( true );
        listener.available( service2 );

        replay( service1, service2, context, listener, filter, reference1, reference2 );
        HttpServiceTracker tracker = new HttpServiceTracker( context, new HttpServiceListener[]{ listener } );
        tracker.open();
        listenerCapture.getCaptured().serviceChanged( new ServiceEvent( ServiceEvent.REGISTERED, reference1 ) );
        listenerCapture.getCaptured().serviceChanged( new ServiceEvent( ServiceEvent.REGISTERED, reference2 ) );
        listenerCapture.getCaptured().serviceChanged( new ServiceEvent( ServiceEvent.UNREGISTERING, reference1 ) );
        verify( service1, service2, context, listener, filter, reference1, reference2 );
    }

}
