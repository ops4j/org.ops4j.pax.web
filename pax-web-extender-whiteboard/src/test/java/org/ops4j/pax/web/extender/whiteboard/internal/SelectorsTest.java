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
package org.ops4j.pax.web.extender.whiteboard.internal;

import java.util.Hashtable;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SelectorsTest {

	@Test
	public void matchMaps() throws Exception {
		Filter filter = FrameworkUtil.createFilter("(a=b)");
		Hashtable<String, String> map = new Hashtable<>();

		map.put("a", "b");
		assertTrue(filter.matchCase(map));

		// if a context has (osgi.http.whiteboard.context.httpservice=xxx), filter without this predicate
		// will match anyway... So we have to find better way of selecting HttpService-based OsgiContextModels
		// only if needed
		map.put("c", "d");
		assertTrue(filter.matchCase(map));

		map.clear();
		assertFalse(filter.matchCase(map));
	}

}
