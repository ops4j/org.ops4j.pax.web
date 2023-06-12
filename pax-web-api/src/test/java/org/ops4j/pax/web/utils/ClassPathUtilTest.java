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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClassPathUtilTest {

	private final ClassLoader cl = getClass().getClassLoader();

	@Test
	public void findJUnitClassResources() throws IOException {
		// org.junit.jupiter.api.Disabled
		assertThat(ClassPathUtil.findEntries(cl, "org", "Disabled.class", false).size()).isEqualTo(0);
		assertThat(ClassPathUtil.findEntries(cl, "org/junit/jupiter/api", "api/Disabled.class", false).size()).isEqualTo(0);
		assertThat(ClassPathUtil.findEntries(cl, "org/junit/jupiter/api", "api/Disabled.class", true).size()).isEqualTo(0);

		assertCorrectClass(ClassPathUtil.findEntries(cl, "org", "Disabled.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org/", "Disabled.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org", "api/Disabled.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org", "*/Disabled.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org", "api/Disab*.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org", "api/Disabled.*", true));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "/org", "api/Disabled.*", true));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org/junit/jupiter", "api/Disabled.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org/junit/jupiter", "api/Disabled.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org/junit/jupiter/", "api/Disabled.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org/junit/jupiter", "/api/Disabled.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "org/junit/jupiter/", "/api/Disabled.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "/org/junit/jupiter/", "/api/Disabled.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(cl, "/", "DisplayNameGeneration.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(cl, null, "DisplayNameGeneration.class", true));
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
			if (url.toExternalForm().contains("org/junit/jupiter/junit-jupiter-api")) {
				if ("jar".equals(url.getProtocol())) {
					roots[0] = url;
				} else {
					roots[0] = new URL("jar:" + url.toExternalForm() + "!/");
				}
				break;
			}
		}

		assertThat(ClassPathUtil.findEntries(null, roots, "org", "Disabled.class", false).size()).isEqualTo(0);
		assertThat(ClassPathUtil.findEntries(null, roots, "org/junit/jupiter/api", "api/Disabled.class", false).size()).isEqualTo(0);
		assertThat(ClassPathUtil.findEntries(null, roots, "org/junit/jupiter/api", "api/Disabled.class", true).size()).isEqualTo(0);

		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "org", "Disabled.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "org/", "Disabled.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "org", "api/Disabled.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "org", "*/Disabled.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "org", "api/Disab*.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "org", "api/Disabled.*", true));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "/org", "api/Disabled.*", true));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "org/junit/jupiter", "api/Disabled.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "org/junit/jupiter", "api/Disabled.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "org/junit/jupiter/", "api/Disabled.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "org/junit/jupiter", "/api/Disabled.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "org/junit/jupiter/", "/api/Disabled.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "/org/junit/jupiter/", "/api/Disabled.class", false));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, "/", "DisplayNameGeneration.class", true));
		assertCorrectClass(ClassPathUtil.findEntries(null, roots, null, "DisplayNameGeneration.class", true));
	}

	private static void assertCorrectClass(List<URL> urls) {
		assertThat(urls.size()).isEqualTo(1);
		try (InputStream is = urls.get(0).openConnection().getInputStream()) {
			byte[] clazz = IOUtils.toByteArray(is);
			assertThat(clazz[0]).isEqualTo((byte) 0xCA);
			assertThat(clazz[1]).isEqualTo((byte) 0xFE);
			assertThat(clazz[2]).isEqualTo((byte) 0xBA);
			assertThat(clazz[3]).isEqualTo((byte) 0xBE);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}
