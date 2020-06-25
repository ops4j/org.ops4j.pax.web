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
package org.ops4j.pax.web.service.tomcat;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.realm.MemoryRealm;
import org.apache.catalina.realm.MessageDigestCredentialHandler;
import org.apache.tomcat.util.digester.Digester;
import org.ops4j.pax.web.annotations.Review;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Review("Is it needed?")
public class OSGiMemoryRealm extends MemoryRealm {

	private static final Logger LOG = LoggerFactory.getLogger(OSGiMemoryRealm.class);

	@Override
	protected void startInternal() throws LifecycleException {

		if (getPathname().startsWith("classpath")) {

			String pathName = getPathname();
			try {
				URL pathUrl = new URL(pathName);
				pathName = pathUrl.getHost();
			} catch (MalformedURLException e) {
				LOG.error("Pathname URL is a malformed URL", e);
			}

			ClassLoader classLoader = getClass().getClassLoader();
			InputStream inputStream = classLoader.getResourceAsStream(pathName);

			if (inputStream == null) {
				Enumeration<URL> resources;
				try {
					resources = classLoader.getResources(pathName);
					while (resources.hasMoreElements()) {
						URL nextElement = resources.nextElement();
						inputStream = nextElement.openStream();
						continue;
					}

				} catch (IOException e) {
					LOG.warn("IOException while iterating over resources", e);
				}
			}

			Digester digester = getDigester();
			try {
				synchronized (digester) {
					digester.push(this);
					digester.parse(inputStream);
				}
				//CHECKSTYLE:OFF
			} catch (Exception e) {
				throw new LifecycleException(
						sm.getString("memoryRealm.readXml"), e);
				//CHECKSTYLE:ON
			} finally {
				digester.reset();
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException e1) {
					}
				}
			}

	        if (getCredentialHandler() == null) {
	            setCredentialHandler(new MessageDigestCredentialHandler());
	        }

			setState(LifecycleState.STARTING);
		} else {
			super.startInternal();
		}

	}

}
