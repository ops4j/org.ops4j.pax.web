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
package org.ops4j.pax.web.itest.base;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author anierbeck
 */
public class VersionUtil {

	private static final String PROJECT_VERSION;
	private static final String MY_FACES_VERSION;
	private static final String KARAF_VERSION;

	static {
		String projectVersion = System.getProperty("ProjectVersion");
		String myFacesVersion = System.getProperty("MyFacesVersion");
		String karafVersion = System.getProperty("KarafVersion");

		try {
			final InputStream is = VersionUtil.class.getClassLoader().getResourceAsStream(
					"META-INF/pax-web-version.properties");
			if (is != null) {
				final Properties properties = new Properties();
				properties.load(is);
				projectVersion = properties.getProperty("pax.web.version", "").trim();
				myFacesVersion = properties.getProperty("myfaces.version", "").trim();
				karafVersion = properties.getProperty("karaf.version", "").trim();
			}
		} catch (IOException ignore) {
			// use default versions
		}

		PROJECT_VERSION = projectVersion;
		MY_FACES_VERSION = myFacesVersion;
		KARAF_VERSION = karafVersion;
	}

	private VersionUtil() {
		//hidden
	}

	public static String getProjectVersion() {
		return PROJECT_VERSION;
	}

	public static String getMyFacesVersion() {
		return MY_FACES_VERSION;
	}

	public static String getKarafVersion() {
		return KARAF_VERSION;
	}
}
