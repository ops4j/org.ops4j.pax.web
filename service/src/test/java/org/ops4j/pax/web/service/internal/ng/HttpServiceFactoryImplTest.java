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
    private ServiceRegistration m_registration;
    private ServerController m_serverController;
    private RegistrationsCluster m_cluster;

    @Before
    public void setUp()
    {
        m_bundle = createMock( Bundle.class );
        m_registration = createMock( ServiceRegistration.class );
        m_serverController = createMock( ServerController.class );
        m_cluster = createMock( RegistrationsCluster.class );
        m_underTest = new HttpServiceFactoryImpl( m_serverController );
    }

    @Test
    public void checkGetServiceFlow()
    {
        // prepare
        expect ( m_serverController.getRegistrationsCluster() ).andReturn( m_cluster );
        m_serverController.addListener( (ServerListener) notNull() );
        replay( m_serverController, m_bundle, m_registration );
        // execute
        Object result = m_underTest.getService( m_bundle, m_registration );
        assertNotNull( "expect not null", result );
        assertTrue( "expect an HttpService", result instanceof HttpService );
        // verify
        verify( m_serverController, m_bundle, m_registration );
    }

    @Test
    public void checkUngetServiceFlow()
    {
        // prepare
        replay( m_bundle, m_registration );
        // execute
        m_underTest.ungetService( m_bundle, m_registration, null );
        // verify
        verify( m_bundle, m_registration );
    }

}
