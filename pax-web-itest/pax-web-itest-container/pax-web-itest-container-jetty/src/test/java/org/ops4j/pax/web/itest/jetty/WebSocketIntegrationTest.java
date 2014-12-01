package org.ops4j.pax.web.itest.jetty;

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.net.URI;
import java.util.Dictionary;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WebSocketIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory.getLogger(WebSocketIntegrationTest.class);

	@Configuration
	public static Option[] configure() {
		return combine(
				configureWebSocketJetty(),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
						.artifactId("websocket-jsr356")
						.type("war")
						.version(VersionUtil.getProjectVersion()),
				mavenBundle().groupId("javax.json")
						.artifactId("javax.json-api").versionAsInProject());
	}

	/**
	 * You will get a list of bundles installed by default plus your testcase,
	 * wrapped into a bundle called pax-exam-probe
	 */
	@Test
	public void listBundles() {
		for (Bundle b : bundleContext.getBundles()) {
			if (b.getState() != Bundle.ACTIVE) {
				fail("Bundle should be active: " + b);
			}

			Dictionary<String,String> headers = b.getHeaders();
			String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
			if (ctxtPath != null) {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName() + " : " + ctxtPath);
			} else {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName());
			}
		}

	}
	
	@Test
	public void testWebsocket() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/websocket/index.html",
				"Chatroom");
		// testClient.testWebPath("ws://127.0.0.1:8181/websocket/chat/", 400);

	}

}

