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
import org.osgi.framework.Bundle;

import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class WarBasicTest extends MultiContainerTestSupport {

	@Override
	protected boolean enableWarExtender() {
		return true;
	}

	@Override
	protected boolean enableWhiteboardExtender() {
		return false;
	}

	@Test
	public void simplestWabWithoutWebXml() throws Exception {
		Bundle sample1 = mockBundle("sample1", "/sample");
		installWab(sample1);

//		assertThat(httpGET(port, "/s"), endsWith("S(1)"));

		uninstallWab(sample1);

//		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 404"));

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(sample1);

		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

}
