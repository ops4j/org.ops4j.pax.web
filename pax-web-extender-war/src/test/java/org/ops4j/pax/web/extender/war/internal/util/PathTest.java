/* Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.war.internal.util;

import org.junit.Test;
import org.ops4j.pax.web.service.spi.util.Path;

import static org.junit.Assert.assertEquals;

public class PathTest {

	/**
	 * Tests that a request with null with not crash with NPE and it returns
	 * null.
	 */
	@Test
	public void replaceSlashesWithNull() {
		assertEquals("Replaced", null, Path.replaceSlashes(null));
	}

	/**
	 * Tests that a request with nothing to be replaced will return the same
	 * string.
	 */
	@Test
	public void replaceSlashes01() {
		assertEquals("Replaced", "/foo/bar/", Path.replaceSlashes("/foo/bar/"));
	}

	/**
	 * Tests that a request that contains only a slash is replaced correctly.
	 */
	@Test
	public void replaceSlashes02() {
		assertEquals("Replaced", "/", Path.replaceSlashes("/"));
	}

	/**
	 * Tests that a request that contains only slashes is replaced correctly.
	 */
	@Test
	public void replaceSlashes03() {
		assertEquals("Replaced", "/", Path.replaceSlashes("/////"));
	}

	/**
	 * Tests that a request that starts slashes is replaced correctly.
	 */
	@Test
	public void replaceSlashes04() {
		assertEquals("Replaced", "/foo/bar", Path.replaceSlashes("///foo/bar"));
	}

	/**
	 * Tests that a request that ends with slashes is replaced correctly.
	 */
	@Test
	public void replaceSlashes05() {
		assertEquals("Replaced", "foo/bar/", Path.replaceSlashes("foo/bar///"));
	}

	/**
	 * Tests that a request that contains slashes is replaced correctly.
	 */
	@Test
	public void replaceSlashes06() {
		assertEquals("Replaced", "foo/bar", Path.replaceSlashes("foo////bar"));
	}

	/**
	 * Tests that a request that various cobinations of slashes is replaced
	 * correctly.
	 */
	@Test
	public void replaceSlashes07() {
		assertEquals("Replaced", "/foo/bar/car/",
				Path.replaceSlashes("/foo////bar//car//"));
	}

	/**
	 * Tests that normalizing a path that contains only slash "/" will return
	 * slash "/"
	 */
	@Test
	public void normalizeResourcePathSlash01() {
		assertEquals("Normalized", "/", Path.normalizeResourcePath("/"));
	}

	/**
	 * Tests that normalizing a path that contains only slash "/" but also
	 * spaces will return slash "/"
	 */
	@Test
	public void normalizeResourcePathSlash02() {
		assertEquals("Normalized", "/", Path.normalizeResourcePath("  /"));
	}

	/**
	 * Tests that normalizing a path that contains only slash "/" but also
	 * spaces will return slash "/"
	 */
	@Test
	public void normalizeResourcePathSlash03() {
		assertEquals("Normalized", "/", Path.normalizeResourcePath("  /   "));
	}

	/**
	 * Tests that normalizing a path that contains only slash "/" but also
	 * spaces will return slash "/"
	 */
	@Test
	public void normalizeResourcePathSlash04() {
		assertEquals("Normalized", "/", Path.normalizeResourcePath("/   "));
	}

}
