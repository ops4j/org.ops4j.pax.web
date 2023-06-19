/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.itest.server.war;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.itest.server.MultiContainerTestSupport;
import org.ops4j.pax.web.itest.server.support.war.StaticList;
import org.osgi.framework.Bundle;

import jakarta.servlet.ServletContainerInitializer;
import java.io.File;
import java.net.URL;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@RunWith(Parameterized.class)
public class WarListenersTest extends MultiContainerTestSupport {

	@Override
	protected boolean enableWarExtender() {
		return true;
	}

	@Test
	public void wabWithListeners() throws Exception {
		// 1. the WAB bundle
		Bundle wab = mockBundle("wab-listeners", "/c");
		when(wab.getBundleId()).thenReturn(42L);
		configureBundleClassPath(wab, "src/test/resources/bundles/wab-listeners", entries -> {
			entries.add("WEB-INF/classes");
		});
		String webXmlLocation = String.format("bundle://42.0:0%s",
				new File("src/test/resources/bundles/wab-listeners/WEB-INF/web.xml").toURI().getPath());
		when(wab.findEntries("WEB-INF", "web.xml", false))
				.thenReturn(Collections.enumeration(Collections.singletonList(new URL(webXmlLocation))));
		when(wab.loadClass(anyString()))
				.thenAnswer(i -> WarListenersTest.class.getClassLoader().loadClass(i.getArgument(0, String.class)));

		File wabServices = new File("src/test/resources/bundles/wab-listeners/WEB-INF/classes/META-INF/services/");
		when(wab.getResources("META-INF/services/" + ServletContainerInitializer.class.getName())).thenReturn(
				Collections.enumeration(Collections.singletonList(
						new File(wabServices, ServletContainerInitializer.class.getName()).toURI().toURL()))
		);

		// for proper 302 redirect - Undertow doesn't handle such redirect when accessing root of the context.
		when(wab.getEntry("/")).thenReturn(new File("src/test/resources/bundles/wab-listeners/").toURI().toURL());

		StaticList.EVENTS.clear();

		installWab(wab);

		// there should be a /c context that's (by default) redirecting to /wab/
		assertThat(httpGET(port, "/c"), startsWith("HTTP/1.1 302"));

		assertThat(StaticList.EVENTS.size(), equalTo(3));
		assertThat(StaticList.EVENTS.get(0), equalTo("Listener-from-web.xml initialized"));
		assertThat(StaticList.EVENTS.get(1), equalTo("Listener-from-SCI initialized"));
		assertThat(StaticList.EVENTS.get(2), equalTo("Servlet-from-web.xml initialized"));

		uninstallWab(wab);

		assertThat(StaticList.EVENTS.size(), equalTo(6));
		assertThat(StaticList.EVENTS.get(3), equalTo("Servlet-from-web.xml destroyed"));
		assertThat(StaticList.EVENTS.get(4), equalTo("Listener-from-SCI destroyed"));
		assertThat(StaticList.EVENTS.get(5), equalTo("Listener-from-web.xml destroyed"));

		// there should be no /c context at all, so no redirect, just 404
		assertThat(httpGET(port, "/c"), startsWith("HTTP/1.1 404"));

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(wab);

		assertTrue(serverModelInternals.isClean(wab));
		assertTrue(serviceModelInternals.isEmpty());
	}

}
