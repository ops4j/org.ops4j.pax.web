package org.ops4j.pax.web.service.internal.ng;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Connector;

public class JettyFactoryImplTest
{

    private JettyFactoryImpl m_underTest;

    @Before
    public void setUp()
    {
        m_underTest = new JettyFactoryImpl();
    }

    @Test
    public void createServer()
    {
        assertNotNull( "server is not null", m_underTest.createServer() );
    }

    @Test
    public void createConnector()
    {
        Connector connector = m_underTest.createConnector( 80 ); 
        assertNotNull( "connector is not null", connector );
        assertEquals( 80, connector.getPort());
    }

}
