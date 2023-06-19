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
package org.ops4j.pax.web.itest.server.whiteboard;

import java.io.File;
import java.io.FileWriter;
import java.util.Hashtable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultResourceMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultWelcomeFileMapping;
import org.ops4j.pax.web.itest.server.MultiContainerTestSupport;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.model.elements.WelcomeFileModel;
import org.ops4j.pax.web.service.whiteboard.ResourceMapping;
import org.ops4j.pax.web.service.whiteboard.WelcomeFileMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.servlet.context.ServletContextHelper;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.web.itest.server.support.Utils.extractHeaders;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@RunWith(Parameterized.class)
public class WhiteboardWelcomeFilesTest extends MultiContainerTestSupport {

	@Test
	public void onlyOneWayToRegisterWelcomeFiles() throws Exception {
		File base = new File("target/www");
		FileUtils.deleteDirectory(base);
		base.mkdirs();
		try (FileWriter fw = new FileWriter(new File(base, "file.txt"))) {
			IOUtils.write("hello1", fw);
		}

		Bundle sample1 = mockBundle("sample1");
		when(sample1.getEntry("resources")).thenReturn(base.toURI().toURL());
		when(sample1.getEntry("resources/")).thenReturn(base.toURI().toURL());
		when(sample1.getEntry("resources/file.txt")).thenReturn(new File(base, "file.txt").toURI().toURL());

		// 1. Whiteboard registration as Pax Web specific org.ops4j.pax.web.service.whiteboard.WelcomeFileMapping
		//    OSGi service

		DefaultResourceMapping rm = new DefaultResourceMapping();
		rm.setUrlPatterns(new String[] { "/resources/*", "/x/*" });
		rm.setPath("/resources");
		ServiceReference<ResourceMapping> resourceMappingRef = mockReference(sample1, ResourceMapping.class,
				null, () -> rm);
		ServletModel rmModel = getResourceMappingCustomizer().addingService(resourceMappingRef);
		assertThat(httpGET(port, "/resources/file.txt"), endsWith("hello1"));
		assertThat(httpGET(port, "/x/file.txt"), endsWith("hello1"));
		assertThat(httpGET(port, "/x"), startsWith("HTTP/1.1 302"));
		assertThat(httpGET(port, "/x/"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/resources"), startsWith("HTTP/1.1 302"));
		assertThat(httpGET(port, "/resources/"), startsWith("HTTP/1.1 403"));

		DefaultWelcomeFileMapping mapping = new DefaultWelcomeFileMapping();
		mapping.setWelcomeFiles(new String[] { "file.txt" });
		mapping.setRedirect(false);
		ServiceReference<WelcomeFileMapping> mappingRef = mockReference(sample1, WelcomeFileMapping.class,
				null, () -> mapping);
		WelcomeFileModel wfmModel = getWelcomeFileMappingCustomizer().addingService(mappingRef);
		// this is redirect NOT affected by DefaultWelcomeFileMapping.setRedirect()
		assertThat(httpGET(port, "/x"), startsWith("HTTP/1.1 302"));
		assertThat(httpGET(port, "/x/"), endsWith("hello1"));
		assertThat(httpGET(port, "/resources"), startsWith("HTTP/1.1 302"));
		assertThat(httpGET(port, "/resources/"), endsWith("hello1"));

		getWelcomeFileMappingCustomizer().removedService(mappingRef, wfmModel);
		assertThat(httpGET(port, "/x"), startsWith("HTTP/1.1 302"));
		assertThat(httpGET(port, "/x/"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/resources"), startsWith("HTTP/1.1 302"));
		assertThat(httpGET(port, "/resources/"), startsWith("HTTP/1.1 403"));

		DefaultWelcomeFileMapping mapping2 = new DefaultWelcomeFileMapping();
		mapping2.setWelcomeFiles(new String[] { "file.txt" });
		mapping2.setRedirect(true);
		ServiceReference<WelcomeFileMapping> mapping2Ref = mockReference(sample1, WelcomeFileMapping.class,
				null, () -> mapping2);
		WelcomeFileModel wfmModel2 = getWelcomeFileMappingCustomizer().addingService(mapping2Ref);
		// this is redirect NOT affected by DefaultWelcomeFileMapping.setRedirect()
		assertThat(httpGET(port, "/x"), startsWith("HTTP/1.1 302"));
		String response = httpGET(port, "/x/");
		assertThat(response, startsWith("HTTP/1.1 302"));
		assertTrue(extractHeaders(response).get("Location").endsWith("/x/file.txt"));
		assertThat(httpGET(port, "/resources"), startsWith("HTTP/1.1 302"));
		assertThat(httpGET(port, "/resources/"), startsWith("HTTP/1.1 302"));

		getWelcomeFileMappingCustomizer().removedService(mapping2Ref, wfmModel2);
		assertThat(httpGET(port, "/x"), startsWith("HTTP/1.1 302"));
		assertThat(httpGET(port, "/x/"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/resources"), startsWith("HTTP/1.1 302"));
		assertThat(httpGET(port, "/resources/"), startsWith("HTTP/1.1 403"));

		getResourceMappingCustomizer().removedService(resourceMappingRef, rmModel);
		assertThat(httpGET(port, "/resources/file.txt"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/x/file.txt"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/x"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/x/"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/resources"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/resources/"), startsWith("HTTP/1.1 404"));

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(sample1);

		assertTrue(serverModelInternals.isClean(whiteboardBundle));
		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

	@Test
	public void onlyOneWayToRegisterWelcomeFilesInCustomContext() throws Exception {
		File base = new File("target/www");
		FileUtils.deleteDirectory(base);
		base.mkdirs();
		try (FileWriter fw = new FileWriter(new File(base, "file.txt"))) {
			IOUtils.write("hello1", fw);
		}

		Bundle sample1 = mockBundle("sample1");
		when(sample1.getEntry("resources")).thenReturn(base.toURI().toURL());
		when(sample1.getEntry("resources/")).thenReturn(base.toURI().toURL());
		when(sample1.getEntry("resources/file.txt")).thenReturn(new File(base, "file.txt").toURI().toURL());

		ServletContextHelper helper = new ServletContextHelper(sample1) {
		};
		getServletContextHelperCustomizer().addingService(mockServletContextHelperReference(sample1, "c1",
				() -> helper, 0L, 0, "/c"));

		// 1. Whiteboard registration as Pax Web specific org.ops4j.pax.web.service.whiteboard.WelcomeFileMapping
		//    OSGi service

		DefaultResourceMapping rm = new DefaultResourceMapping();
		rm.setUrlPatterns(new String[] { "/resources/*", "/x/*" });
		rm.setPath("/resources");
		Hashtable<String, Object> props = new Hashtable<>();
		// mind that we can't specify the selector using Whiteboard properties, while registering the legacy "mapping"
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, String.format("(%s=c1)",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME));
		// we have to do it in the mapping itself
		rm.setContextSelectFilter(String.format("(%s=c1)", HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME));
		ServiceReference<ResourceMapping> resourceMappingRef = mockReference(sample1, ResourceMapping.class,
				props, () -> rm);
		ServletModel rmModel = getResourceMappingCustomizer().addingService(resourceMappingRef);
		assertThat(httpGET(port, "/c/resources/file.txt"), endsWith("hello1"));
		assertThat(httpGET(port, "/c/x/file.txt"), endsWith("hello1"));
		assertThat(httpGET(port, "/c/x"), startsWith("HTTP/1.1 302"));
		assertThat(httpGET(port, "/c/x/"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/c/resources"), startsWith("HTTP/1.1 302"));
		assertThat(httpGET(port, "/c/resources/"), startsWith("HTTP/1.1 403"));

		DefaultWelcomeFileMapping mapping = new DefaultWelcomeFileMapping();
		mapping.setWelcomeFiles(new String[] { "file.txt" });
		mapping.setRedirect(false);
		mapping.setContextSelectFilter(String.format("(%s=c1)", HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME));
		ServiceReference<WelcomeFileMapping> mappingRef = mockReference(sample1, WelcomeFileMapping.class,
				props, () -> mapping);
		WelcomeFileModel wfmModel = getWelcomeFileMappingCustomizer().addingService(mappingRef);
		// this is redirect NOT affected by DefaultWelcomeFileMapping.setRedirect()
		assertThat(httpGET(port, "/c/x"), startsWith("HTTP/1.1 302"));
		assertThat(httpGET(port, "/c/x/"), endsWith("hello1"));
		assertThat(httpGET(port, "/c/resources"), startsWith("HTTP/1.1 302"));
		assertThat(httpGET(port, "/c/resources/"), endsWith("hello1"));

		getWelcomeFileMappingCustomizer().removedService(mappingRef, wfmModel);
		assertThat(httpGET(port, "/c/x"), startsWith("HTTP/1.1 302"));
		assertThat(httpGET(port, "/c/x/"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/c/resources"), startsWith("HTTP/1.1 302"));
		assertThat(httpGET(port, "/c/resources/"), startsWith("HTTP/1.1 403"));

		DefaultWelcomeFileMapping mapping2 = new DefaultWelcomeFileMapping();
		mapping2.setWelcomeFiles(new String[] { "file.txt" });
		mapping2.setRedirect(true);
		mapping2.setContextSelectFilter(String.format("(%s=c1)", HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME));
		ServiceReference<WelcomeFileMapping> mapping2Ref = mockReference(sample1, WelcomeFileMapping.class,
				props, () -> mapping2);
		WelcomeFileModel wfmModel2 = getWelcomeFileMappingCustomizer().addingService(mapping2Ref);
		// this is redirect NOT affected by DefaultWelcomeFileMapping.setRedirect()
		assertThat(httpGET(port, "/c/x"), startsWith("HTTP/1.1 302"));
		String response = httpGET(port, "/c/x/");
		assertThat(response, startsWith("HTTP/1.1 302"));
		assertTrue(extractHeaders(response).get("Location").endsWith("/c/x/file.txt"));
		assertThat(httpGET(port, "/c/resources"), startsWith("HTTP/1.1 302"));
		assertThat(httpGET(port, "/c/resources/"), startsWith("HTTP/1.1 302"));

		getWelcomeFileMappingCustomizer().removedService(mapping2Ref, wfmModel2);
		assertThat(httpGET(port, "/c/x"), startsWith("HTTP/1.1 302"));
		assertThat(httpGET(port, "/c/x/"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/c/resources"), startsWith("HTTP/1.1 302"));
		assertThat(httpGET(port, "/c/resources/"), startsWith("HTTP/1.1 403"));

		getResourceMappingCustomizer().removedService(resourceMappingRef, rmModel);
		assertThat(httpGET(port, "/c/resources/file.txt"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/c/x/file.txt"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/c/x"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/c/x/"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/c/resources"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/c/resources/"), startsWith("HTTP/1.1 404"));

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(sample1);

		assertTrue(serverModelInternals.isClean(whiteboardBundle));
		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

	@Test
	public void onlyOneWayToRegisterWelcomeFilesInCustomContextAndDefaultWelcomeFiles() throws Exception {
		File base = new File("target/www");
		FileUtils.deleteDirectory(base);
		base.mkdirs();
		try (FileWriter fw = new FileWriter(new File(base, "file.txt"))) {
			IOUtils.write("hello1", fw);
		}

		Bundle sample1 = mockBundle("sample1");
		when(sample1.getEntry("resources")).thenReturn(base.toURI().toURL());
		when(sample1.getEntry("resources/")).thenReturn(base.toURI().toURL());
		when(sample1.getEntry("resources/file.txt")).thenReturn(new File(base, "file.txt").toURI().toURL());

		ServletContextHelper helper = new ServletContextHelper(sample1) {
		};
		getServletContextHelperCustomizer().addingService(mockServletContextHelperReference(sample1, "c1",
				() -> helper, 0L, 0, "/c"));

		// 1. Whiteboard registration as Pax Web specific org.ops4j.pax.web.service.whiteboard.WelcomeFileMapping
		//    OSGi service

		DefaultResourceMapping rm = new DefaultResourceMapping();
		rm.setUrlPatterns(new String[] { "/" });
		rm.setPath("/resources");
		Hashtable<String, Object> props = new Hashtable<>();
		// mind that we can't specify the selector using Whiteboard properties, while registering the legacy "mapping"
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, String.format("(%s=c1)",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME));
		// we have to do it in the mapping itself
		rm.setContextSelectFilter(String.format("(%s=c1)", HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME));
		ServiceReference<ResourceMapping> resourceMappingRef = mockReference(sample1, ResourceMapping.class,
				props, () -> rm);
		ServletModel rmModel = getResourceMappingCustomizer().addingService(resourceMappingRef);
		assertThat(httpGET(port, "/c/file.txt"), endsWith("hello1"));
		assertThat(httpGET(port, "/c"), startsWith("HTTP/1.1 302"));
		assertThat(httpGET(port, "/c/"), startsWith("HTTP/1.1 403"));

		DefaultWelcomeFileMapping mapping = new DefaultWelcomeFileMapping();
		mapping.setWelcomeFiles(new String[] { "file.txt" });
		mapping.setRedirect(false);
		mapping.setContextSelectFilter(String.format("(%s=c1)", HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME));
		ServiceReference<WelcomeFileMapping> mappingRef = mockReference(sample1, WelcomeFileMapping.class,
				props, () -> mapping);
		WelcomeFileModel wfmModel = getWelcomeFileMappingCustomizer().addingService(mappingRef);
		// this is redirect NOT affected by DefaultWelcomeFileMapping.setRedirect()
		assertThat(httpGET(port, "/c"), startsWith("HTTP/1.1 302"));
		assertThat(httpGET(port, "/c/"), endsWith("hello1"));

		getWelcomeFileMappingCustomizer().removedService(mappingRef, wfmModel);
		assertThat(httpGET(port, "/c"), startsWith("HTTP/1.1 302"));
		assertThat(httpGET(port, "/c/"), startsWith("HTTP/1.1 403"));

		DefaultWelcomeFileMapping mapping2 = new DefaultWelcomeFileMapping();
		mapping2.setWelcomeFiles(new String[] { "file.txt" });
		mapping2.setRedirect(true);
		mapping2.setContextSelectFilter(String.format("(%s=c1)", HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME));
		ServiceReference<WelcomeFileMapping> mapping2Ref = mockReference(sample1, WelcomeFileMapping.class,
				props, () -> mapping2);
		WelcomeFileModel wfmModel2 = getWelcomeFileMappingCustomizer().addingService(mapping2Ref);
		// this is redirect NOT affected by DefaultWelcomeFileMapping.setRedirect()
		assertThat(httpGET(port, "/c"), startsWith("HTTP/1.1 302"));
		String response = httpGET(port, "/c/");
		assertThat(response, startsWith("HTTP/1.1 302"));
		assertTrue(extractHeaders(response).get("Location").endsWith("/c/file.txt"));

		getWelcomeFileMappingCustomizer().removedService(mapping2Ref, wfmModel2);
		assertThat(httpGET(port, "/c"), startsWith("HTTP/1.1 302"));
		assertThat(httpGET(port, "/c/"), startsWith("HTTP/1.1 403"));

		getResourceMappingCustomizer().removedService(resourceMappingRef, rmModel);
		assertThat(httpGET(port, "/c/file.txt"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/c"), startsWith("HTTP/1.1 302"));
		assertThat(httpGET(port, "/c/"), startsWith("HTTP/1.1 404"));

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(sample1);

		assertTrue(serverModelInternals.isClean(whiteboardBundle));
		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

}
