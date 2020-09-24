/*
 * Copyright 2020 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.itest.container.jsp;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.container.httpservice.AbstractHttpServiceIntegrationTest;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Achim Nierbeck
 */
public abstract class AbstractJspIntegrationTest extends AbstractContainerTestBase {

	public static final Logger LOG = LoggerFactory.getLogger(AbstractHttpServiceIntegrationTest.class);

	private Bundle hsBundle;

	// TODO: we DON'T stop the bundle in @After, because for now we don't stop the underlying ServletContext
	//       when last servlet from this context is removed. And we can't for now register SCIs (one of them
	//       is needed for JSPs) to a started context.

	@Before
	public void setup() throws Exception {
		for (Bundle b : context.getBundles()) {
			if (sampleURI("helloworld-jsp").equals(b.getLocation())) {
				hsBundle = b;
				break;
			}
		}
		if (hsBundle != null && hsBundle.getState() == Bundle.ACTIVE) {
			return;
		}
		configureAndWaitForServletWithMapping("/images/*",
				() -> hsBundle = installAndStartBundle(sampleURI("helloworld-jsp")));
	}

	@After
	public void cleanup() throws BundleException {
//		if (hsBundle != null) {
//			hsBundle.stop();
//			hsBundle.uninstall();
//		}
	}

	@Test
	public void testSimpleJsp() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://localhost:8181/helloworld/jsp/simple.jsp");
	}

	@Test
	public void testTldJsp() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello World'",
						resp -> resp.contains("Hello World"))
				.doGETandExecuteTest("http://localhost:8181/helloworld/jsp/using-tld.jsp");
	}

	@Test
	public void testPrecompiled() throws Exception {
		String elFactory = System.getProperty("javax.el.ExpressionFactory");
		try {
			// The "context" in which JSPs are served is related to "helloworld-jsp" bundle and unlike in the
			// case where JSPs are compiled (and TCCL is the loader from pax-web-jsp), here, JSP servlet
			// (actually directly compiled JSP page) calls ExpressionFactory.newInstance() within the scope of
			// TCCL of helloworld-jsp bundle.
			// So we need this property ("helloworld-jsp" can't see the /META-INF/services/javax.el.ExpressionFactory
			// inside pax-web-jsp) AND we need org.apache.el Import-Package.
			System.setProperty("javax.el.ExpressionFactory", "org.apache.el.ExpressionFactoryImpl");
			HttpTestClientFactory.createDefaultTestClient()
					.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
							resp -> resp.contains("<h1>Hello World</h1>"))
					.doGETandExecuteTest("http://localhost:8181/helloworld/jspc/simple.jsp");
			HttpTestClientFactory.createDefaultTestClient()
					.withResponseAssertion("Response must contain 'Hello World'",
							resp -> resp.contains("Hello World"))
					.doGETandExecuteTest("http://localhost:8181/helloworld/jspc/using-tld.jsp");
		} finally {
			if (elFactory != null) {
				System.setProperty("javax.el.ExpressionFactory", elFactory);
			}
		}
	}

}
