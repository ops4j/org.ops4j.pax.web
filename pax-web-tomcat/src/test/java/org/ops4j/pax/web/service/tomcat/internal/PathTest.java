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
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class PathTest {

	@Test
	public void securePaths() {
		assertThat(RequestUtil.normalize(""), equalTo("/"));
		assertThat(RequestUtil.normalize("/"), equalTo("/"));
		assertThat(RequestUtil.normalize("c:"), equalTo("/c:"));
		assertThat(RequestUtil.normalize("c:/"), equalTo("/c:/"));
		assertThat(RequestUtil.normalize("c:\\"), equalTo("/c:/"));
		assertThat(RequestUtil.normalize("//"), equalTo("/"));
		assertThat(RequestUtil.normalize("."), equalTo("/"));
		assertThat(RequestUtil.normalize(".."), nullValue());
		assertThat(RequestUtil.normalize("../.."), nullValue());
		assertThat(RequestUtil.normalize("./././././././."), equalTo("/"));
		assertThat(RequestUtil.normalize("../../../../../etc/passwd"), nullValue());
		assertThat(RequestUtil.normalize("path1"), equalTo("/path1"));
		assertThat(RequestUtil.normalize("./path1"), equalTo("/path1"));
		assertThat(RequestUtil.normalize("../path1"), nullValue());
		assertThat(RequestUtil.normalize("path1/path2"), equalTo("/path1/path2"));
		assertThat(RequestUtil.normalize("path1/../path2"), equalTo("/path2"));
	}

}
