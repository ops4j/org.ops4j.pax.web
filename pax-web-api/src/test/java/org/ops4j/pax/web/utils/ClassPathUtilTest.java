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
package org.ops4j.pax.web.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassPathUtilTest {

	private final ClassLoader cl = getClass().getClassLoader();

	@Test
	public void findJUnitClassResources() throws IOException {
		assertThat(ClassPathUtil.findEntries(cl, "org", "RunWith.class", false).size(), equalTo(0));
		assertThat(ClassPathUtil.findEntries(cl, "org/junit/runner", "runner/RunWith.class", false).size(), equalTo(0));
		assertThat(ClassPathUtil.findEntries(cl, "org/junit/runner", "runner/RunWith.class", true).size(), equalTo(0));

		assertCorrectClass(ClassPathUtil.findEntries(cl, "org", "RunWith.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org/", "RunWith.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org", "runner/RunWith.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org", "*/RunWith.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org", "runner/RunW*.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org", "runner/RunWith.*", true));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "/org", "runner/RunWith.*", true));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org/junit", "runner/RunWith.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org/junit", "runner/RunWith.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org/junit/", "runner/RunWith.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org/junit", "/runner/RunWith.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org/junit/", "/runner/RunWith.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "/org/junit/", "/runner/RunWith.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "/", "RunWith.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(cl, null, "RunWith.class", true));
	}

	@Test
	public void findFileBasedClassResources() throws IOException {
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org/ops4j", "ClassPathUtilTest.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "/", "ClassPathUtilTest.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(cl, null, "ClassPathUtilTest.class", true));

		assertCorrectClass(ClassPathUtil.findEntries(cl, "org/ops4j/pax/web/utils", "ClassPathUtilTest.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org/ops4j/pax/web/utils/", "ClassPathUtilTest.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "/org/ops4j/pax/web/utils", "ClassPathUtilTest.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "/org/ops4j/pax/web/utils/", "ClassPathUtilTest.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "/org/ops4j/pax/web/utils/", "/ClassPathUtilTest.class", false));

		assertCorrectClass(ClassPathUtil.findEntries(cl, "/org/ops4j/pax/web/", "/ClassPathUtilTest.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "/org/ops4j/pax/web", "/ClassPathUtilTest.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org/ops4j/pax/web", "/ClassPathUtilTest.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org/ops4j/pax/web", "ClassPathUtilTest.class", true));
	}

	@Test
	public void findResourcesInJARs() throws IOException {
		URL[] roots = new URL[] { null };

		URL[] urls = ClassPathUtil.getURLs(cl);
		if (urls.length == 1) {
			urls = ClassPathUtil.jarToItsClassPath(urls[0]);
		}
		for (URL url : urls) {
			if (url.toExternalForm().contains("junit/junit")) {
				if ("jar".equals(url.getProtocol())) {
					roots[0] = url;
				} else {
					roots[0] = new URL("jar:" + url.toExternalForm() + "!/");
				}
				break;
			}
		}

		assertThat(ClassPathUtil.findEntries(null, roots, "org", "RunWith.class", false).size(), equalTo(0));
		assertThat(ClassPathUtil.findEntries(null, roots, "org/junit/runner", "runner/RunWith.class", false).size(), equalTo(0));
		assertThat(ClassPathUtil.findEntries(null, roots, "org/junit/runner", "runner/RunWith.class", true).size(), equalTo(0));

		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "org", "RunWith.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "org/", "RunWith.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "org", "runner/RunWith.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "org", "*/RunWith.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "org", "runner/RunW*.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "org", "runner/RunWith.*", true));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "/org", "runner/RunWith.*", true));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "org/junit", "runner/RunWith.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "org/junit", "runner/RunWith.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "org/junit/", "runner/RunWith.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "org/junit", "/runner/RunWith.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "org/junit/", "/runner/RunWith.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "/org/junit/", "/runner/RunWith.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "/", "RunWith.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, null, "RunWith.class", true));
	}

	private void assertCorrectClass(List<URL> urls) {
		assertThat(urls.size(), equalTo(1));
		try (InputStream is = urls.get(0).openConnection().getInputStream()) {
			byte[] clazz = IOUtils.toByteArray(is);
			assertThat(clazz[0], equalTo((byte) 0xCA));
			assertThat(clazz[1], equalTo((byte) 0xFE));
			assertThat(clazz[2], equalTo((byte) 0xBA));
			assertThat(clazz[3], equalTo((byte) 0xBE));
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}
