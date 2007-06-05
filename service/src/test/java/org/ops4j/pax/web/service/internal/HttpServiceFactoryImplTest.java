package org.ops4j.pax.web.service.internal;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.ops4j.pax.web.service.internal.HttpServiceFactoryImpl;
import org.ops4j.pax.web.service.internal.StoppableHttpService;

public class HttpServiceFactoryImplTest
{
    private HttpServiceFactoryImpl m_underTest;
    private Bundle m_bundle;
    private ServiceRegistration m_serviceRegistration;
    private StoppableHttpService m_httpService;

    @Before
    public void setUp()
    {
        m_bundle = createMock( Bundle.class );
        m_serviceRegistration = createMock( ServiceRegistration.class );
        m_httpService = createMock( StoppableHttpService.class );
        m_underTest = new HttpServiceFactoryImpl() {
            HttpService createService( Bundle bundle )
            {
                return m_httpService;
            }
        };
    }

    @Test
    public void checkGetServiceFlow()
    {
        // prepare
        replay( m_bundle, m_serviceRegistration, m_httpService );
        // execute
        Object result = m_underTest.getService( m_bundle, m_serviceRegistration );
        assertNotNull( "expect not null", result );
        // verify
        verify( m_bundle, m_serviceRegistration, m_httpService );
    }

    @Test
    public void checkUngetServiceFlow()
    {
        // prepare
        m_httpService.stop();
        replay( m_bundle, m_serviceRegistration, m_httpService );
        // execute
        m_underTest.ungetService( m_bundle, m_serviceRegistration, m_httpService );
        // verify
        verify( m_bundle, m_serviceRegistration, m_httpService );
    }

}
