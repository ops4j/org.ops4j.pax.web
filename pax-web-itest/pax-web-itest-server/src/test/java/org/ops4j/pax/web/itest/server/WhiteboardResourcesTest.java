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
package org.ops4j.pax.web.itest.server;

import java.io.File;
import java.io.FileWriter;
import java.util.Hashtable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultResourceMapping;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.whiteboard.ResourceMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@RunWith(Parameterized.class)
public class WhiteboardResourcesTest extends MultiContainerTestSupport {

	@Test
	public void twoWaysToRegisterResources() throws Exception {
		File base = new File("target/www");
		FileUtils.deleteDirectory(base);
		base.mkdirs();
		try (FileWriter fw = new FileWriter(new File(base, "file.txt"))) {
			IOUtils.write("hello1", fw);
		}

		Bundle sample1 = mockBundle("sample1");
		when(sample1.getEntry("resources/file.txt")).thenReturn(new File(base, "file.txt").toURI().toURL());

		// 1. Whiteboard registration as an OSGi service of any objectClass

		Hashtable<String, Object> properties = new Hashtable<>();
		// OSGi CMPN Whiteboard properties
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, "/files/*");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, "/resources");
		ServiceReference<Object> resourcesRef = mockReference(sample1, Object.class, properties, Object::new, 0L, 0);
		ServletModel model = getResourceCustomizer().addingService(resourcesRef);
		assertThat(httpGET(port, "/files/file.txt"), endsWith("hello1"));
		assertThat(httpGET(port, "/files/other.txt"), startsWith("HTTP/1.1 404"));

		getResourceCustomizer().removedService(resourcesRef, model);
		assertThat(httpGET(port, "/files/file.txt"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/files/other.txt"), startsWith("HTTP/1.1 404"));

		// 2. Whiteboard registration as Pax Web specific org.ops4j.pax.web.service.whiteboard.ResourceMapping
		//    OSGi service

		DefaultResourceMapping rm = new DefaultResourceMapping();
		rm.setUrlPatterns(new String[] { "/resources/*", "/x/*" });
		rm.setPath("/resources");
		ServiceReference<ResourceMapping> resourceMappingRef = mockReference(sample1, ResourceMapping.class,
				null, () -> rm);
		ServletModel model2 = getResourceMappingCustomizer().addingService(resourceMappingRef);
		assertThat(httpGET(port, "/resources/file.txt"), endsWith("hello1"));
		assertThat(httpGET(port, "/x/file.txt"), endsWith("hello1"));
		assertThat(httpGET(port, "/resources/other.txt"), startsWith("HTTP/1.1 404"));

		getResourceMappingCustomizer().removedService(resourceMappingRef, model2);
		assertThat(httpGET(port, "/resources/file.txt"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/resources/other.txt"), startsWith("HTTP/1.1 404"));

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(sample1);

		assertTrue(serverModelInternals.isClean(whiteboardBundle));
		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

}
