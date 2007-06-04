package org.ops4j.pax.web.service.internal.ng;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;

public class HttpServiceFactoryImplTest
{
    private HttpServiceFactoryImpl m_underTest;
    private Bundle m_bundle;
    private ServiceRegistration m_serviceRegistration;
    private ServerController m_serverController;
    private RegistrationsCluster m_registrationsCluster;
    private Registrations m_registrations;

    @Before
    public void setUp()
    {
        m_bundle = createMock( Bundle.class );
        m_registrationsCluster = createMock( RegistrationsCluster.class );
        m_registrations = createMock( Registrations.class );
        m_serviceRegistration = createMock( ServiceRegistration.class );
        m_serverController = createMock( ServerController.class );
        m_underTest = new HttpServiceFactoryImpl( m_serverController, m_registrationsCluster );
    }

    @Test
    public void checkGetServiceFlow()
    {
        // prepare
        expect( m_registrationsCluster.create() ).andReturn( m_registrations );
        m_serverController.addListener( (ServerListener) notNull() );
        replay( m_serverController, m_bundle, m_serviceRegistration, m_registrationsCluster, m_registrations );
        // execute
        Object result = m_underTest.getService( m_bundle, m_serviceRegistration );
        assertNotNull( "expect not null", result );
        assertTrue( "expect an HttpService", result instanceof HttpService );
        // verify
        verify( m_serverController, m_bundle, m_serviceRegistration, m_registrationsCluster, m_registrations );
    }

    @Test
    public void checkUngetServiceFlow()
    {
        // prepare
        replay( m_bundle, m_serviceRegistration );
        // execute
        m_underTest.ungetService( m_bundle, m_serviceRegistration, null );
        // verify
        verify( m_bundle, m_serviceRegistration );
    }

}
