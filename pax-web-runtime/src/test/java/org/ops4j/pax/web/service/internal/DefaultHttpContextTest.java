/* Copyright 2007 Alin Dreghiciu.
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

import org.ops4j.pax.web.service.spi.context.DefaultHttpContext;
import org.osgi.framework.Bundle;

public class DefaultHttpContextTest {

	private Bundle bundle;
	private DefaultHttpContext contextUnderTest;
//
//	@Before
//	public void setUp() {
//		bundle = createMock(Bundle.class);
//		contextUnderTest = new DefaultHttpContext(bundle, null);
//	}
//
//	@Test
//	public void handleSecurity() throws IOException {
//		// always returns true, request and response does not matter
//		assertTrue(contextUnderTest.handleSecurity(null, null));
//	}
//
//	@Test
//	public void getMimeType() {
//		// always returns null, name does not matter
//		assertEquals(null, contextUnderTest.getMimeType(null));
//	}
//
//	@Test
//	public void getResource() throws MalformedURLException {
//		URL url = new URL("file://");
//		expect(bundle.getResource("test")).andReturn(url);
//		replay(bundle);
//		contextUnderTest.getResource("test");
//		verify(bundle);
//	}

}
