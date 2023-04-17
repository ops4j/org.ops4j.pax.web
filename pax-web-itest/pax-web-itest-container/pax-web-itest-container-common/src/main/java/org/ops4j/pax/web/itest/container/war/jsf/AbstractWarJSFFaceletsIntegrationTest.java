/*
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
package org.ops4j.pax.web.itest.container.war.jsf;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;

/**
 * @author achim
 */
public abstract class AbstractWarJSFFaceletsIntegrationTest extends AbstractContainerTestBase {

	private Bundle wab;

	@Before
	public void setUp() throws Exception {
		wab = configureAndWaitForDeploymentUnlessInstalled("trinidad-demo", () -> {
			// we'll override Import-Package to prevent duplicate "javax.faces.webapp" package import (pax-url problem?)
			installAndStartWebBundle("org.apache.myfaces.trinidad", "trinidad-demo", "2.2.1", "trinidad-demo", "/simple",
					uri -> uri + "&Import-Package=javax.servlet.http,javax.xml.parsers,javax.xml.transform,org.w3c.dom,"
							+ "org.xml.sax,org.xml.sax.ext,org.xml.sax.helpers,"
							+ "javax.faces,javax.faces.view,javax.faces.context,javax.el,"
							+ "javax.faces.component,javax.faces.application,javax.faces.render,javax.faces.event,"
							+ "javax.faces.component.visit,javax.faces.component.behavior,javax.faces.el,"
							+ "javax.faces.convert,javax.crypto,javax.naming,javax.faces.lifecycle,javax.faces.model,"
							+ "javax.servlet.jsp.tagext,javax.servlet.jsp,javax.faces.validator"
							+ "&Require-Bundle=org.apache.myfaces.core.impl");
		});
	}

	@Test
	public void testSlash() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Foo - Anywhere, U.S.A.'",
						resp -> resp.contains("Foo - Anywhere, U.S.A."))
				.doGETandExecuteTest("http://127.0.0.1:8181/simple/faces/email/login.jspx");
	}

}
