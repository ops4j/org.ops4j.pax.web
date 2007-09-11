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
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Server;
import org.osgi.framework.Bundle;

public class HttpServiceContextTest
{

    private HttpServiceContext m_underTest;
    private Bundle m_bundle;

    @Before
    public void setUp()
    {
        m_bundle = createMock( Bundle.class );
        m_underTest = new HttpServiceContext( new Server(), "contextPath", 0 );
    }

    @Test
    public void getResource()
    {
        // TODO
    }

    @Test
    public void getResourceAsStream()
    {
        // TODO
    }

    @Test
    public void getMimeType()
    {
        // TODO
    }

}
