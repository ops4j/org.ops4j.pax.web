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

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.UrlResource;

public class UrlTest {

	@Test
	public void testRelativeURLs() throws Exception {
		URL root1 = new URL("file:/data/tmp");
		URL root2 = new URL("file:/data/tmp/");
		System.out.println(new URL(root1, "sub1"));
		System.out.println(new URL(root2, "sub2"));

		UrlResource res1 = new UrlResource("file:/data/tmp");
		UrlResource res2 = new UrlResource("file:/data/tmp/");
		System.out.println(res1.createRelative("sub1"));
		System.out.println(res2.createRelative("sub2"));
	}

}
