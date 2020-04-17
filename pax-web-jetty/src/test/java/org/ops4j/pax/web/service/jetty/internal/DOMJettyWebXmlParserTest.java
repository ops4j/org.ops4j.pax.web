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

import java.io.ByteArrayInputStream;
import java.io.File;

import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.Test;
import org.ops4j.pax.web.service.jetty.internal.util.DOMJettyWebXmlParser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author achim
 */
public class DOMJettyWebXmlParserTest {

	String xmlinput = "<Configure class=\"org.eclipse.jetty.webapp.WebAppContext\">\n"
			+ "  <Set name=\"virtualHosts\">\n"
			+ "    <Array type=\"java.lang.String\">\n"
			+ "      <Item>127.0.0.1:9090</Item>\n"
			+ "    </Array>\n"
			+ "  </Set>\n" + "</Configure>";

	String xmlinputWithArg = "<Configure class=\"TEST\">\n"
			+ "  <Set name=\"file\">\n" + "    <New class=\"java.io.File\">\n"
			+ "      <Arg>PATH</Arg>\n" + "    </New>\n" + "  </Set>\n"
			+ "</Configure>";
	
	String xmlErrorInput = "<Configure class=\"org.eclipse.jetty.webapp.WebAppContext\">\n" + 
	        "    <Set name=\"contextPath\">/dashboard</Set>\n" + 
	        "    \n" + 
	        "    <Get name=\"errorHandler\">\n" + 
	        "        <Call name=\"addErrorPage\">\n" + 
	        "            <Arg type=\"int\">400</Arg>\n" + 
	        "            <Arg type=\"int\">599</Arg>\n" + 
	        "            <Arg type=\"String\">/error.xhtml</Arg> <!-- ERROR HERE -->\n" + 
	        "        </Call>\n" + 
	        "    </Get>\n" + 
	        "</Configure>";

	/**
	 * Test method for
	 * {@link org.ops4j.pax.web.service.jetty.internal.DOMJettyWebXmlParser#parse(java.lang.Object, java.io.InputStream)}
	 * .
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

	public static class ArgTest {
		private File file;

		public void setFile(final File file) {
			this.file = file;
		}

		public File getFile() {
			return file;
		}
	}

	/**
	 * Test method for
	 * {@link org.ops4j.pax.web.service.jetty.internal.DOMJettyWebXmlParser#parse(java.lang.Object, java.io.InputStream)}
	 * .
	 */
	@Test
	public void testParseWithArg() {
		final DOMJettyWebXmlParser parser = new DOMJettyWebXmlParser();

		final ByteArrayInputStream in = new ByteArrayInputStream(
				xmlinputWithArg.getBytes());

		final ArgTest test = new ArgTest();
		parser.parse(test, in);

		assertNotNull(test.getFile());
		assertEquals(test.getFile().getPath(), "PATH");
	}
	
	@Test
	public void testErrorInput() {
	    final DOMJettyWebXmlParser parser = new DOMJettyWebXmlParser();
	    
	    final ByteArrayInputStream in = new ByteArrayInputStream(xmlErrorInput.getBytes());
	    
	    WebAppContext ctxt = new WebAppContext();
	    parser.parse(ctxt, in);
	    
	    assertNotNull(ctxt);
	    assertEquals("/dashboard", ctxt.getContextPath());
	    assertNotNull(ctxt.getErrorHandler());
	    ErrorHandler errorHandler = ctxt.getErrorHandler();
	    
	    //assertNotNull();
	    
	}
}
