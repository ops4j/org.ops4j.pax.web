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
package org.ops4j.pax.web.itest.common;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * @author achim
 */
public abstract class AbstractWarJSFFaceletsIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

	@Before
	public void setUp() throws Exception {
		Bundle[] bundles = bundleContext.getBundles();
		for (Bundle bundle : bundles) {
			if ("org.apache.myfaces.core.api".equalsIgnoreCase(bundle
					.getSymbolicName())
					|| "org.apache.myfaces.core.impl".equalsIgnoreCase(bundle
					.getSymbolicName())) {
				bundle.stop();
				bundle.start();
			}
		}

		initWebListener();

		String bundlePath = WEB_BUNDLE
//				+ "mvn:org.apache.myfaces.commons/myfaces-commons-facelets-examples20/1.0.2.1/war?"
				+ "mvn:org.apache.myfaces.trinidad/trinidad-demo/2.2.0/war?"
				// +
				// "mvn:org.apache.myfaces.tomahawk/myfaces-example-simple20/1.1.14/war?"
				+ WEB_CONTEXT_PATH + "=/simple";
		// + "&import-package=javax.servlet,javax.servlet.annotation"
		// +
		// ",javax.el,org.xml.sax,org.xml.sax.helpers,javax.xml.parsers,org.w3c.dom,javax.naming";
		installWarBundle = bundleContext.installBundle(bundlePath);

		installWarBundle.start();

		waitForWebListener();
	}

	@After
	public void tearDown() throws BundleException {
		if (installWarBundle != null) {
			installWarBundle.stop();
			installWarBundle.uninstall();
		}
	}


	// http://localhost:8181
	@Test
	public void testSlash() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Please enter your name'",
						resp -> resp.contains("Please enter your name"))
				.doGETandExecuteTest("http://127.0.0.1:8181/simple");
	}
}
