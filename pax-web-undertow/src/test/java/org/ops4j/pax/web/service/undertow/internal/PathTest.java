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
package org.ops4j.pax.web.service.undertow.internal;

import org.junit.Test;

import static io.undertow.util.CanonicalPathUtils.canonicalize;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class PathTest {

	@Test
	public void securePaths() {
		assertThat(canonicalize(""), equalTo(""));
		assertThat(canonicalize("/"), equalTo("/"));
		assertThat(canonicalize("c:"), equalTo("c:"));
		assertThat(canonicalize("c:/"), equalTo("c:/"));
		assertThat(canonicalize("c:\\"), equalTo("c:\\"));
		assertThat(canonicalize("//"), equalTo("/")); // Jetty: "//"
		assertThat(canonicalize("."), equalTo(".")); // Jetty: ""
		assertThat(canonicalize(".."), equalTo("..")); // Jetty: null, meaning we go out of chroot
		assertThat(canonicalize("../.."), equalTo("..")); // Jetty: null, meaning we go out of chroot
		assertThat(canonicalize("./././././././."), equalTo(".")); // Jetty: ""
		assertThat(canonicalize("../../../../../etc/passwd"), equalTo("../etc/passwd")); // Jetty: null, meaning we go out of chroot
		assertThat(canonicalize("path1"), equalTo("path1"));
		assertThat(canonicalize("./path1"), equalTo("./path1")); // Jetty: "path1"
		assertThat(canonicalize("../path1"), equalTo("../path1")); // Jetty: null, meaning we go out of chroot
		assertThat(canonicalize("path1/path2"), equalTo("path1/path2"));
		assertThat(canonicalize("path1/../path2"), equalTo("path1/path2")); // Jetty: "path2"
	}

}
