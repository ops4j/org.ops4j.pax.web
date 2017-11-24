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

package org.ops4j.pax.web.itest.jetty;

import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.base.support.AnnotatedTestServlet;
import org.ops4j.pax.web.itest.common.AbstractWarIntegrationTest;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WarIntegrationTest extends AbstractWarIntegrationTest {

	@Configuration
	public static Option[] configure() {
		return combine(configureJetty(),
				streamBundle(bundle()
						.add(AnnotatedTestServlet.class)
						.set(Constants.BUNDLE_SYMBOLICNAME, TEST_BUNDLE_SYMBOLIC_NAME)
						.set(Constants.EXPORT_PACKAGE, "*")
						.set(Constants.IMPORT_PACKAGE, "*")
						.set(WEB_CONTEXT_PATH, "destroyable")
						.build(withBnd())).noStart()
		);
	}

    /**
     * this is a manual test only, as it's not possible to check if the init/destroy method of the servlet are called.
     * 
     * It is only available on Jetty
     */
    @Test
    public void testWarStop() throws Exception {

        Bundle bundle = null;

        for (Bundle b : bundleContext.getBundles()) {
            if (TEST_BUNDLE_SYMBOLIC_NAME.equalsIgnoreCase(b.getSymbolicName())) {
                bundle = b;
                break;
            }
        }

        assertNotNull(bundle);
        bundle.start();

        HttpTestClientFactory.createDefaultTestClient()
                .withResponseAssertion("Response must contain 'TEST OK'",
                        resp -> resp.contains("TEST OK"))
                .doGETandExecuteTest("http://127.0.0.1:8181/destroyable/test");

        System.out.println("Stopping Bundle: " + bundle.getSymbolicName());

        bundle.stop();

        System.out.println("Stopped");
    }
}

