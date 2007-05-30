package org.ops4j.pax.web.service.internal.ng;

import org.osgi.framework.Bundle;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import org.easymock.EasyMock;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class DefaultHttpContextImplTest
{

    private Bundle m_bundle;
    private DefaultHttpContextImpl m_contextUnderTest;

    @Before
    public void setUp()
    {
        m_bundle = EasyMock.createMock( Bundle.class );
        m_contextUnderTest = new DefaultHttpContextImpl( m_bundle );
    }

    @Test
    public void handleSecurity() throws IOException
    {
        // always returns true, request and response does not matter
        Assert.assertTrue( m_contextUnderTest.handleSecurity( null, null ) );
    }

    @Test
    public void getMimeType()
    {
        // always returns null, name does not matter
        Assert.assertEquals(null, m_contextUnderTest.getMimeType(null));
    }

    @Test
    public void getResource() throws MalformedURLException
    {
        URL url = new URL( "file://" );
        EasyMock.expect( m_bundle.getResource( "test" ) ).andReturn( url );
        EasyMock.replay( m_bundle );
        m_contextUnderTest.getResource( "test" );
        EasyMock.verify( m_bundle );
    }

}
