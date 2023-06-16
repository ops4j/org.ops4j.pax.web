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
package org.ops4j.pax.web.service.tomcat.internal;

import org.apache.tomcat.util.http.RequestUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PathTest {

	@Test
	public void securePaths() {
		assertThat(RequestUtil.normalize("")).isEqualTo("/");
		assertThat(RequestUtil.normalize("/")).isEqualTo("/");
		assertThat(RequestUtil.normalize("c:")).isEqualTo("/c:");
		assertThat(RequestUtil.normalize("c:/")).isEqualTo("/c:/");
		assertThat(RequestUtil.normalize("c:\\")).isEqualTo("/c:/");
		assertThat(RequestUtil.normalize("//")).isEqualTo("/");
		assertThat(RequestUtil.normalize(".")).isEqualTo("/");
		assertThat(RequestUtil.normalize("..")).isNull();
		assertThat(RequestUtil.normalize("../..")).isNull();
		assertThat(RequestUtil.normalize("./././././././.")).isEqualTo("/");
		assertThat(RequestUtil.normalize("../../../../../etc/passwd")).isNull();
		assertThat(RequestUtil.normalize("path1")).isEqualTo("/path1");
		assertThat(RequestUtil.normalize("./path1")).isEqualTo("/path1");
		assertThat(RequestUtil.normalize("../path1")).isNull();
		assertThat(RequestUtil.normalize("path1/path2")).isEqualTo("/path1/path2");
		assertThat(RequestUtil.normalize("path1/../path2")).isEqualTo("/path2");
	}

}
