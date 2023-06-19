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
package org.ops4j.pax.web.itest.server.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.itest.server.MultiContainerTestSupport;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.internal.HttpServiceEnabled;
import org.ops4j.pax.web.service.internal.StoppableHttpService;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.osgi.framework.Bundle;
import org.ops4j.pax.web.service.http.HttpContext;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.web.itest.server.support.Utils.extractHeaders;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@RunWith(Parameterized.class)
public class ServerControllerResourceRegistrationTest extends MultiContainerTestSupport {

	@Override
	public void initAll() throws Exception {
		configurePort();
	}

	@Test
	public void registerResourcesWithCustomContext() throws Exception {
		ServerController controller = Utils.createServerController(null, port, runtime, getClass().getClassLoader());
		controller.configure();
		controller.start();

		Bundle bundle = mockBundle("App Bundle", false);

		ServerModel server = new ServerModel(new Utils.SameThreadExecutor());

		// no batch at all - everything will be done by HttpService itself
		WebContainer wc = new HttpServiceEnabled(bundle, controller, server, null, controller.getConfiguration());

		File base = new File("target/www");
		FileUtils.deleteDirectory(base);
		base.mkdirs();
		new File(base, "s1").mkdirs();
		new File(base, "s2").mkdirs();
		try (FileWriter fw = new FileWriter(new File(base, "file.txt"))) {
			IOUtils.write("hello1", fw);
		}
		try (FileWriter fw = new FileWriter(new File(base, "/s1/file.txt"))) {
			IOUtils.write("hello2", fw);
		}
		try (FileWriter fw = new FileWriter(new File(base, "/s2/file.txt"))) {
			IOUtils.write("hello3", fw);
		}

		HttpContext context = new HttpContext() {
			@Override
			public URL getResource(String name) {
				try {
					// name should already be secured and normalized and relative to
					return new File("target", name).toURI().toURL();
				} catch (MalformedURLException ignored) {
					return null;
				}
			}

			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				return true;
			}

			@Override
			public String getMimeType(String name) {
				return null;
			}
		};

		wc.registerResources("/r", "www", context);

		String response = httpGET(port, "/r/file.txt");
		assertTrue(response.endsWith("hello1"));
		Map<String, String> headers = extractHeaders(response);
		assertTrue(response.contains("ETag: W/"));
		response = httpGET(port, "/r/file.txt",
				"If-None-Match: " + headers.get("ETag"),
				"If-Modified-Since: " + headers.get("Date"));
		assertTrue(response.contains("HTTP/1.1 304"));
		assertFalse(response.endsWith("hello1"));

		response = httpGET(port, "/r/s1/file.txt");
		assertTrue(response.endsWith("hello2"));

		response = httpGET(port, "/r/s2/file.txt");
		assertTrue(response.endsWith("hello3"));

		// no mapped servlet
		response = httpGET(port, "/r/s3/file.txt");
		assertTrue(response.contains("HTTP/1.1 404"));
		response = httpGET(port, "/r/s3/");
		assertTrue(response.contains("HTTP/1.1 404"));
		response = httpGET(port, "/r/s3");
		assertTrue(response.contains("HTTP/1.1 404"));

		// mapped servlet, but directory access without welcome files. Directory exists (file: URL)
		// so explicit 403
		response = httpGET(port, "/r/s2/");
		assertTrue(response.contains("HTTP/1.1 403"));
		response = httpGET(port, "/r/s2");
		// 302 here, because file: URL is returned from custom context
		assertTrue(response.contains("HTTP/1.1 302"));

		((StoppableHttpService) wc).stop();
		controller.stop();

		ServerModelInternals serverModelInternals = serverModelInternals(server);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(wc);

		assertTrue(serverModelInternals.isClean(bundle));
		assertTrue(serviceModelInternals.isEmpty());
	}

	@Test
	public void registerResourcesWithCustomContextAndWelcomeFiles() throws Exception {
		ServerController controller = Utils.createServerController(null, port, runtime, getClass().getClassLoader());
		controller.configure();
		controller.start();

		Bundle bundle = mockBundle("App Bundle", false);

		ServerModel server = new ServerModel(new Utils.SameThreadExecutor());

		// no batch at all - everything will be done by HttpService itself
		WebContainer wc = new HttpServiceEnabled(bundle, controller, server, null, controller.getConfiguration());

		File base = new File("target/www");
		FileUtils.deleteDirectory(base);
		base.mkdirs();
		new File(base, "s1").mkdirs();
		new File(base, "s2").mkdirs();
		try (FileWriter fw = new FileWriter(new File(base, "file.txt"))) {
			IOUtils.write("hello1", fw);
		}
		try (FileWriter fw = new FileWriter(new File(base, "/s1/file.txt"))) {
			IOUtils.write("hello2", fw);
		}
		try (FileWriter fw = new FileWriter(new File(base, "/s2/file.txt"))) {
			IOUtils.write("hello3", fw);
		}
		try (FileWriter fw = new FileWriter(new File(base, "file.md"))) {
			IOUtils.write("hello1.md", fw);
		}
		try (FileWriter fw = new FileWriter(new File(base, "/s1/file.md"))) {
			IOUtils.write("hello2.md", fw);
		}
		try (FileWriter fw = new FileWriter(new File(base, "/s2/file.md"))) {
			IOUtils.write("hello3.md", fw);
		}

		HttpContext context = new HttpContext() {
			@Override
			public URL getResource(String name) {
				try {
					// name should already be secured and normalized and relative to
					return new File("target", name).toURI().toURL();
				} catch (MalformedURLException ignored) {
					return null;
				}
			}

			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				return true;
			}

			@Override
			public String getMimeType(String name) {
				return null;
			}
		};

		wc.registerResources("/r", "www", context);

		String response = httpGET(port, "/r");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = httpGET(port, "/r/");
		assertTrue(response.startsWith("HTTP/1.1 403"));

		response = httpGET(port, "/r/s1");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = httpGET(port, "/r/s1/");
		assertTrue(response.startsWith("HTTP/1.1 403"));

		response = httpGET(port, "/r/s2");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = httpGET(port, "/r/s2/");
		assertTrue(response.startsWith("HTTP/1.1 403"));

		response = httpGET(port, "/r/s3");
		assertTrue(response.startsWith("HTTP/1.1 404"));
		response = httpGET(port, "/r/s3/");
		assertTrue(response.startsWith("HTTP/1.1 404"));

		wc.registerWelcomeFiles(new String[] { "file.txt", "file.md" }, false, context);

		response = httpGET(port, "/r");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = httpGET(port, "/r/");
		assertTrue(response.endsWith("hello1"));

		response = httpGET(port, "/r/s1");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = httpGET(port, "/r/s1/");
		assertTrue(response.endsWith("hello2"));

		response = httpGET(port, "/r/s2");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = httpGET(port, "/r/s2/");
		assertTrue(response.endsWith("hello3"));

		response = httpGET(port, "/r/s3");
		assertTrue(response.startsWith("HTTP/1.1 404"));
		// there's no www/s3/file.txt physical resource, so a dispatcher will be used for
		// /r/s3/file.txt which will result in HTTP 404
		response = httpGET(port, "/r/s3/");
		assertTrue(response.startsWith("HTTP/1.1 404"));

		wc.unregisterWelcomeFiles(new String[] { "file.txt" }, context);

		response = httpGET(port, "/r");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = httpGET(port, "/r/");
		assertTrue(response.endsWith("hello1.md"));

		response = httpGET(port, "/r/s1");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = httpGET(port, "/r/s1/");
		assertTrue(response.endsWith("hello2.md"));

		response = httpGET(port, "/r/s2");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = httpGET(port, "/r/s2/");
		assertTrue(response.endsWith("hello3.md"));

		response = httpGET(port, "/r/s3");
		assertTrue(response.startsWith("HTTP/1.1 404"));
		response = httpGET(port, "/r/s3/");
		assertTrue(response.startsWith("HTTP/1.1 404"));

		wc.unregisterWelcomeFiles(new String[] { "file.md" }, context);

		response = httpGET(port, "/r");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = httpGET(port, "/r/");
		assertTrue(response.startsWith("HTTP/1.1 403"));

		response = httpGET(port, "/r/s1");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = httpGET(port, "/r/s1/");
		assertTrue(response.startsWith("HTTP/1.1 403"));

		response = httpGET(port, "/r/s2");
		assertTrue(response.startsWith("HTTP/1.1 302"));
		response = httpGET(port, "/r/s2/");
		assertTrue(response.startsWith("HTTP/1.1 403"));

		response = httpGET(port, "/r/s3");
		assertTrue(response.startsWith("HTTP/1.1 404"));
		response = httpGET(port, "/r/s3/");
		assertTrue(response.startsWith("HTTP/1.1 404"));

		((StoppableHttpService) wc).stop();
		controller.stop();

		ServerModelInternals serverModelInternals = serverModelInternals(server);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(wc);

		assertTrue(serverModelInternals.isClean(bundle));
		assertTrue(serviceModelInternals.isEmpty());
	}

	@Test
	public void registerResourcesWithDefaultContext() throws Exception {
		ServerController controller = Utils.createServerController(null, port, runtime, getClass().getClassLoader());
		controller.configure();
		controller.start();

		Bundle bundle = mockBundle("App Bundle", false);

		ServerModel server = new ServerModel(new Utils.SameThreadExecutor());

		// no batch at all - everything will be done by HttpService itself
		WebContainer wc = new HttpServiceEnabled(bundle, controller, server, null, controller.getConfiguration());

		File base = new File("target/www");
		FileUtils.deleteDirectory(base);
		base.mkdirs();
		new File(base, "s1").mkdirs();
		new File(base, "s2").mkdirs();
		try (FileWriter fw = new FileWriter(new File(base, "file.txt"))) {
			IOUtils.write("hello1", fw);
		}
		try (FileWriter fw = new FileWriter(new File(base, "/s1/file.txt"))) {
			IOUtils.write("hello2", fw);
		}
		try (FileWriter fw = new FileWriter(new File(base, "/s2/file.txt"))) {
			IOUtils.write("hello3", fw);
		}

		when(bundle.getResource("www/file.txt")).thenReturn(new File(base, "file.txt").toURI().toURL());
		when(bundle.getResource("www/s1/file.txt")).thenReturn(new File(base, "s1/file.txt").toURI().toURL());
		when(bundle.getResource("www/s2/file.txt")).thenReturn(new File(base, "s2/file.txt").toURI().toURL());

		wc.registerResources("/r", "www", null);

		String response = httpGET(port, "/r/file.txt");
		assertTrue(response.endsWith("hello1"));
		Map<String, String> headers = extractHeaders(response);
		assertTrue(response.contains("ETag: W/"));
		response = httpGET(port, "/r/file.txt",
				"If-None-Match: " + headers.get("ETag"),
				"If-Modified-Since: " + headers.get("Date"));
		assertTrue(response.contains("HTTP/1.1 304"));
		assertFalse(response.endsWith("hello1"));

		response = httpGET(port, "/r/s1/file.txt");
		assertTrue(response.endsWith("hello2"));

		response = httpGET(port, "/r/s2/file.txt");
		assertTrue(response.endsWith("hello3"));

		// no mapped servlet
		response = httpGET(port, "/r/s3/file.txt");
		assertTrue(response.contains("HTTP/1.1 404"));
		response = httpGET(port, "/r/s3/");
		assertTrue(response.contains("HTTP/1.1 404"));
		response = httpGET(port, "/r/s3");
		assertTrue(response.contains("HTTP/1.1 404"));

		// mapped servlet, but directory access without welcome files. bundle.getResource() not mocked
		// for such path, so 404
		response = httpGET(port, "/r/s2/");
		assertTrue(response.contains("HTTP/1.1 404"));
		response = httpGET(port, "/r/s2");
		// 404 here, because we didn't mock accessing r/s2
		assertTrue(response.contains("HTTP/1.1 404"));

		((StoppableHttpService) wc).stop();
		controller.stop();

		ServerModelInternals serverModelInternals = serverModelInternals(server);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(wc);

		assertTrue(serverModelInternals.isClean(bundle));
		assertTrue(serviceModelInternals.isEmpty());
	}

}
