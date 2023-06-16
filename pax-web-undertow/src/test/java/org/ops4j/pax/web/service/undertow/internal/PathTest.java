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

import org.junit.jupiter.api.Test;

import static io.undertow.util.CanonicalPathUtils.canonicalize;
import static org.assertj.core.api.Assertions.assertThat;

public class PathTest {

	@Test
	public void securePaths() {
		assertThat(canonicalize("")).isEqualTo("");
		assertThat(canonicalize("/")).isEqualTo("/");
		assertThat(canonicalize("c:")).isEqualTo("c:");
		assertThat(canonicalize("c:/")).isEqualTo("c:/");
		assertThat(canonicalize("c:\\")).isEqualTo("c:\\");
		assertThat(canonicalize("//")).isEqualTo("/"); // Jetty: "//"
		assertThat(canonicalize(".")).isEqualTo("."); // Jetty: ""
		assertThat(canonicalize("..")).isEqualTo(".."); // Jetty: null, meaning we go out of chroot
		assertThat(canonicalize("../..")).isEqualTo(".."); // Jetty: null, meaning we go out of chroot
		assertThat(canonicalize("./././././././.")).isEqualTo("."); // Jetty: ""
		assertThat(canonicalize("../../../../../etc/passwd")).isEqualTo("../etc/passwd"); // Jetty: null, meaning we go out of chroot
		assertThat(canonicalize("path1")).isEqualTo("path1");
		assertThat(canonicalize("./path1")).isEqualTo("./path1"); // Jetty: "path1"
		assertThat(canonicalize("../path1")).isEqualTo("../path1"); // Jetty: null, meaning we go out of chroot
		assertThat(canonicalize("path1/path2")).isEqualTo("path1/path2");
		assertThat(canonicalize("path1/../path2")).isEqualTo("path1/path2"); // Jetty: "path2"
	}

}
