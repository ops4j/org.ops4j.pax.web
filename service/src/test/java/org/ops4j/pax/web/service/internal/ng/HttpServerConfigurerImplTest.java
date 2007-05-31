package org.ops4j.pax.web.service.internal.ng;

import static org.easymock.EasyMock.*;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.service.HttpServiceConfiguration;

public class HttpServerConfigurerImplTest
{
    private ServerController m_serverController;
    private HttpServiceConfiguration m_configuration;
    private HttpServiceConfigurerImpl m_underTest;

    @Before
    public void setUp()
    {
        m_serverController = createMock( ServerController.class );
        m_configuration = createMock( HttpServiceConfiguration.class );
        m_underTest = new HttpServiceConfigurerImpl( m_serverController );
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
        m_serverController.configure( m_configuration );
        expect( m_serverController.isStarted() ).andReturn( false );
        m_serverController.start();
        replay( m_serverController );
        // run
        m_underTest.configure( m_configuration );
        // verify
        verify ( m_serverController );
    }

    @Test
    public void configureAfterServerStarted()
    {
       // prepare
        m_serverController.configure( m_configuration );
        expect( m_serverController.isStarted() ).andReturn( true );
        replay( m_serverController );
        // run
        m_underTest.configure( m_configuration );
        // verify
        verify ( m_serverController );
    }

}
