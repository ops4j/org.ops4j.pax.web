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
 /* 
 * Copyright 2011 Achim Nierbeck.
 *
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
package org.ops4j.pax.web.deployer.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Achim
 */
public class DeployerUtilsTest {

	/**
	 * Test method for
	 * {@link org.ops4j.pax.web.deployer.internal.DeployerUtils#extractNameVersionType(java.lang.String)}
	 * .
	 */
	@Test
	public void testExtractNameVersionType() {

		String[] nameVersion;
		// test war types with version
		nameVersion = DeployerUtils.extractNameVersionType("test-1.0.0.war");
		assertEquals("test", nameVersion[0]);
		assertEquals("1.0.0", nameVersion[1]);

		// test standard war types
		nameVersion = DeployerUtils.extractNameVersionType("test.war");

		assertEquals("test", nameVersion[0]);
		assertEquals("0.0.0", nameVersion[1]);

	}

}
