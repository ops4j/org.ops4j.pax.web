/*  Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.internal;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.service.internal.DefaultHttpContextImpl;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class DefaultHttpContextImplTest {

    private Bundle m_bundle;
    private DefaultHttpContextImpl m_contextUnderTest;

    @Before
    public void setUp()
    {
        m_bundle = createMock( Bundle.class );
        m_contextUnderTest = new DefaultHttpContextImpl( m_bundle );
    }

    @Test
    public void handleSecurity() throws IOException {
        // always returns true, request and response does not matter
        assertTrue( m_contextUnderTest.handleSecurity( null, null ) );
    }

    @Test 
    public void getMimeType()
    {
        // always returns null, name does not matter
        assertEquals(null, m_contextUnderTest.getMimeType(null));
    }

    @Test
    public void getResource() throws MalformedURLException {
        URL url = new URL( "file://" );
        expect( m_bundle.getResource( "test" ) ).andReturn( url );
        replay( m_bundle );
        m_contextUnderTest.getResource( "test" );
        verify( m_bundle );
    }

}
