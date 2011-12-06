/* Copyright 2011 Achim Nierbeck.
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
package org.ops4j.pax.web.service.jetty.internal;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.service.jetty.internal.util.DOMJettyWebXmlParser;

/**
 * @author achim
 *
 */
public class DOMJettyWebXmlParserTest {

	
	String xmlinput = "<Configure class=\"org.eclipse.jetty.webapp.WebAppContext\">\n" + 
			"  <Set name=\"virtualHosts\">\n" + 
			"    <Array type=\"java.lang.String\">\n" + 
			"      <Item>127.0.0.1:9090</Item>\n" + 
			"    </Array>\n" + 
			"  </Set>\n" + 
			"</Configure>";
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		
	}

	/**
	 * Test method for {@link org.ops4j.pax.web.service.jetty.internal.DOMJettyWebXmlParser#parse(java.lang.Object, java.io.InputStream)}.
	 */
	@Test
	public void testParse() {
		DOMJettyWebXmlParser parser = new DOMJettyWebXmlParser();
		
		ByteArrayInputStream in = new ByteArrayInputStream(xmlinput.getBytes());
		
		ServletContextHandler context = new ServletContextHandler();
		parser.parse(context, in);
		
		assertNotNull(context);
		assertNotNull(context.getVirtualHosts());
	}

}
