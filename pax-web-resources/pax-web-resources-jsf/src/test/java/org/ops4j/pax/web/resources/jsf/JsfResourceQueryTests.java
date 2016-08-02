/* Copyright 2016 Marc Schlegel
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
package org.ops4j.pax.web.resources.jsf;

import static org.junit.Assert.*;

import java.util.Optional;

import org.junit.Test;

public class JsfResourceQueryTests {

	@Test
	public void testLocaleAndLibrary() throws Exception {
		JsfResourceQuery query = new JsfResourceQuery("iceland", "library", "resource.png", null);

		Optional<JsfResourceQueryResult> result = query.matches("/META-INF/resources/iceland/library/resource.png");

		assertTrue(result.isPresent());
	}

	@Test
	public void testLocaleAndLibrary_WithoutLocalePath() throws Exception {
		JsfResourceQuery query = new JsfResourceQuery("iceland", "library", "resource.png", null);

		Optional<JsfResourceQueryResult> result = query.matches("/META-INF/resources/library/resource.png");

		assertTrue(result.isPresent());
	}

	@Test
	public void testLocaleAndLibraryWithVersion() throws Exception {
		JsfResourceQuery query = new JsfResourceQuery("iceland", "library", "resource.png", null);

		Optional<JsfResourceQueryResult> result = query.matches("/META-INF/resources/iceland/library/1_1/resource.png");

		assertTrue(result.isPresent());
	}

	@Test
	public void testLocaleAndResourceWithVersion() throws Exception {
		JsfResourceQuery query = new JsfResourceQuery("iceland", "library", "resource.png", null);

		Optional<JsfResourceQueryResult> result = query.matches("/META-INF/resources/iceland/resource.png/2_1.png");

		assertTrue(result.isPresent());
	}

	@Test
	public void testLocaleAndLibraryWithVersionAndResourceVersion() throws Exception {
		JsfResourceQuery query = new JsfResourceQuery("iceland", "library", "resource.png", null);

		Optional<JsfResourceQueryResult> result = query.matches("/META-INF/resources/iceland/library/1_1/resource.png/2_1.png");

		assertTrue(result.isPresent());
	}

	@Test
	public void testLocaleAndLibraryWithVersionAndResourceWithSubfolderAndVersion() throws Exception {
		JsfResourceQuery query = new JsfResourceQuery("iceland", "library", "img/resource.png", null);

		Optional<JsfResourceQueryResult> result = query.matches("/META-INF/resources/iceland/library/1_1/img/resource.png/2_1.png");

		assertTrue(result.isPresent());
	}

	@Test
	public void testLocaleAndLibraryWithVersionAndResourceWithSubfolderAndVersion_False() throws Exception {
		JsfResourceQuery query = new JsfResourceQuery("iceland", "library", "img/resource.png", null);

		Optional<JsfResourceQueryResult> result = query.matches("/META-INF/resources/iceland/library/1_1/img/not_matching_resourcename.png/2_1.png");

		assertFalse(result.isPresent());
	}
}
