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
package org.ops4j.pax.web.itest.karaf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.utils.client.HttpTestClient;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import static org.junit.Assert.assertTrue;

/**
 * @author achim
 */
@RunWith(PaxExam.class)
public abstract class WarBaseKarafTest extends AbstractKarafTestBase {

	private Bundle wab;

	@Before
	public void setup() throws Exception {
		configureAndWaitForDeploymentUnlessInstalled("war",
				() -> wab = installAndStartWebBundle("war", "/war"));
	}

	@After
	public void tearDown() throws BundleException {
		if (wab != null) {
			wab.stop();
			wab.uninstall();
		}
	}

	protected boolean gzipEncodingEnabled() {
		return false;
	}

	@Test
	public void testWC() throws Exception {
		HttpTestClient client = createTestClientForKaraf()
				.withResponseAssertion("Response must contain text from served by Karaf!",
						resp -> resp.contains("<h1>Hello World</h1>"));
		// can't check existence of headers, because org.apache.hc.client5.http.impl.classic.ContentCompressionExec.execute()
		// removes them
		if (gzipEncodingEnabled()) {
			client.addRequestHeader("Accept-Encoding", "gzip");
		}
		client.doGETandExecuteTest("http://127.0.0.1:8181/war/wc");
	}

	@Test
	public void testGzipWC() throws Exception {
		if (gzipEncodingEnabled()) {
			byte[] response = httpGET(8181, "/war/wc", "Accept-Encoding: gzip");
			int pos = 0;
			while (pos < response.length - 3) {
				if (response[pos] == 0x0d && response[pos + 1] == 0x0a && response[pos + 2] == 0x0d && response[pos + 3] == 0x0a) {
					pos += 4;
					break;
				}
				pos++;
			}
			String fullResponse = new String(response);
			if (fullResponse.contains("Transfer-Encoding: chunked")) {
				ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
				while (true) {
					if (pos >= response.length) {
						break;
					}
					int idx = pos;
					int idx2 = pos;
					while (response[idx2] != 0x0d) {
						idx2++;
					}

					String hex = new String(response, pos, idx2 - idx);
					int chunkSize = Integer.parseInt(hex, 16);
					pos += hex.length() + 2;
					baos2.write(response, pos, chunkSize);
					pos += chunkSize + 2;
				}
				response = baos2.toByteArray();
				pos = 0;
			}
			GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(response, pos, response.length - pos));
			byte[] buf = new byte[64];
			int read;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			while ((read = gzis.read(buf)) > 0) {
				baos.write(buf, 0, read);
			}

			assertTrue(new String(baos.toByteArray(), StandardCharsets.UTF_8).contains("<h1>Hello World</h1>"));
		}
	}

	@Test
	public void testWCExample() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain text served by Karaf!",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/example");

		createTestClientForKaraf()
				.doGETandExecuteTest("http://127.0.0.1:8181/war/images/logo.png");
	}

	@Test
	public void testWCSN() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain text served by Karaf!",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/sn");
	}

	@Test
	public void testSlash() throws Exception {
		createTestClientForKaraf()
				.withReturnCode(404)
				.withResponseAssertion("Response must contain '<h1>Error Page</h1>'",
						resp -> resp.contains("<h1>Error Page</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/");
	}

	@Test
	public void testSubJSP() throws Exception {
		createTestClientForKaraf()
				.withResponseAssertion("Response must contain text served by Karaf!",
						resp -> resp.contains("<h2>Hello World!</h2>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/subjsp");
	}

	@Test
	public void testErrorJSPCall() throws Exception {
		createTestClientForKaraf()
				.withReturnCode(404)
				.withResponseAssertion("Response must contain text from error-page served by Karaf!",
						resp -> resp.contains("<h1>Error Page</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc/error.jsp");
	}

	@Test
	public void testWrongServlet() throws Exception {
		createTestClientForKaraf()
				.withReturnCode(404)
				.withResponseAssertion("Response must contain text from error-page served by Karaf!",
						resp -> resp.contains("<h1>Error Page</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wrong/");
	}

	public static byte[] httpGET(int port, String request, String... headers) throws IOException {
		Socket s = new Socket();
		s.connect(new InetSocketAddress("127.0.0.1", port));

		s.getOutputStream().write((
				"GET " + request + " HTTP/1.1\r\n" +
						"Host: 127.0.0.1:" + port + "\r\n").getBytes());
		for (String header : headers) {
			s.getOutputStream().write((header + "\r\n").getBytes());
		}
		s.getOutputStream().write(("Connection: close\r\n\r\n").getBytes());

		byte[] buf = new byte[64];
		int read;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while ((read = s.getInputStream().read(buf)) > 0) {
			baos.write(buf, 0, read);
		}
		s.getOutputStream().close();
		s.close();

		return baos.toByteArray();
	}

}
