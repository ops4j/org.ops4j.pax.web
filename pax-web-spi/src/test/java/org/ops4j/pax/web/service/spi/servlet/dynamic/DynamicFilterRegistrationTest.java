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
package org.ops4j.pax.web.service.spi.servlet.dynamic;

import java.util.Collection;
import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.junit.Test;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class DynamicFilterRegistrationTest {

	@Test
	public void flatteningServletNames() {
		FilterModel fm = new FilterModel.Builder().withFilterName("f1").build();
		DynamicFilterRegistration dfr = new DynamicFilterRegistration(fm, null, null);
		dfr.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), true, "s1");
		dfr.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST, DispatcherType.INCLUDE), true, "s2", "s3");
		dfr.addMappingForServletNames(EnumSet.of(DispatcherType.ERROR, DispatcherType.INCLUDE), true, "s2", "s4");

		Collection<String> patterns = dfr.getServletNameMappings();
		assertThat(patterns.size(), equalTo(4));
		assertTrue(patterns.contains("s1"));
		assertTrue(patterns.contains("s2"));
		assertTrue(patterns.contains("s3"));
		assertTrue(patterns.contains("s4"));
	}

	@Test
	public void flatteningUrlPatterns() {
		FilterModel fm = new FilterModel.Builder().withFilterName("f1").build();
		DynamicFilterRegistration dfr = new DynamicFilterRegistration(fm, null, null);
		dfr.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/s1");
		dfr.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST, DispatcherType.INCLUDE), true, "/s2", "/s3");
		dfr.addMappingForUrlPatterns(EnumSet.of(DispatcherType.ERROR, DispatcherType.INCLUDE), true, "/s2", "/s3/*");

		Collection<String> patterns = dfr.getUrlPatternMappings();
		assertThat(patterns.size(), equalTo(4));
		assertTrue(patterns.contains("/s1"));
		assertTrue(patterns.contains("/s2"));
		assertTrue(patterns.contains("/s3"));
		assertTrue(patterns.contains("/s3/*"));
	}

}
