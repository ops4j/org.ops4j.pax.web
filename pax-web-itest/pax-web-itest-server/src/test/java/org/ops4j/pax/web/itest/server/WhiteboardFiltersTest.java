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
package org.ops4j.pax.web.itest.server;

import javax.servlet.Filter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultFilterMapping;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.whiteboard.FilterMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@RunWith(Parameterized.class)
public class WhiteboardFiltersTest extends MultiContainerTestSupport {

	@Test
	public void twoWaysToRegisterFilter() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		// 1. Whiteboard registration as javax.servlet.Filter OSGi service

		ServiceReference<Filter> filterRef = mockFilterReference(sample1, "filter1",
				() -> new Utils.MyIdFilter("1"), 0L, 0, "/s");
		FilterModel model = getFilterCustomizer().addingService(filterRef);
		assertThat(httpGET(port, "/s?terminate=1"), endsWith(">F(1)<F(1)"));

		getFilterCustomizer().removedService(filterRef, model);
		assertThat(httpGET(port, "/s?terminate=1"), startsWith("HTTP/1.1 404"));

		// 2. Whiteboard registration as Pax Web specific org.ops4j.pax.web.service.whiteboard.FilterMapping
		//    OSGi service

		DefaultFilterMapping fm = new DefaultFilterMapping();
		fm.setFilter(new Utils.MyIdFilter("2"));
		fm.setUrlPatterns(new String[] { "/t" });
		ServiceReference<FilterMapping> filterMappingRef = mockReference(sample1, FilterMapping.class,
				null, () -> fm);
		FilterModel model2 = getFilterMappingCustomizer().addingService(filterMappingRef);
		assertThat(httpGET(port, "/t?terminate=2"), endsWith(">F(2)<F(2)"));

		getFilterMappingCustomizer().removedService(filterMappingRef, model2);
		assertThat(httpGET(port, "/t?terminate=2"), startsWith("HTTP/1.1 404"));

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(sample1);

		assertTrue(serverModelInternals.isClean(whiteboardBundle));
		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

}
