package org.ops4j.pax.web.service.internal.ng;

import static org.easymock.EasyMock.*;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.service.HttpServiceConfiguration;

public class HttpServerConfigurerImplTest
{
    private HttpServiceServer m_httpServiceServer;
    private HttpServiceConfiguration m_configuration;
    private HttpServiceConfigurerImpl m_underTest;

    @Before
    public void setUp()
    {
        m_httpServiceServer = createMock( HttpServiceServer.class );
        m_configuration = createMock( HttpServiceConfiguration.class );
        m_underTest = new HttpServiceConfigurerImpl( m_httpServiceServer );
    }

    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullServer()
    {
        new HttpServiceConfigurerImpl( null );
    }

    @Test
    public void configureBeforeServerStarted()
    {
       // prepare
        m_httpServiceServer.configure( m_configuration );
        expect( m_httpServiceServer.isStarted() ).andReturn( false );
        m_httpServiceServer.start();
        replay( m_httpServiceServer );
        // run
        m_underTest.configure( m_configuration );
        // verify
        verify ( m_httpServiceServer );
    }

    @Test
    public void configureAfterServerStarted()
    {
       // prepare
        m_httpServiceServer.configure( m_configuration );
        expect( m_httpServiceServer.isStarted() ).andReturn( true );
        replay( m_httpServiceServer );
        // run
        m_underTest.configure( m_configuration );
        // verify
        verify ( m_httpServiceServer );
    }

}
