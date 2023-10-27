/*
 * Copyright 2023 OPS4J.
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
package org.ops4j.pax.web.samples.wf.wab;

import org.ops4j.pax.web.samples.wf.filter.SomeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class SomeListener implements ServletContextListener {

	public static final Logger LOG = LoggerFactory.getLogger(SomeListener.class);

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		LOG.info("SomeListener.contextInitialized()");
		ServletContext context = sce.getServletContext();

		@SuppressWarnings("unchecked")
		List<String> list = (List<String>) context.getAttribute("SomeListener.list");
		if (list != null) {
			throw new RuntimeException("Already initialized");
		}
		context.setAttribute("SomeListener.list",
				Collections.singletonList("SomeListener initialized"));

		SomeFilter filter = new SomeFilter();
		context.addFilter("SomeFilter", filter)
				.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD), true, "/*");
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		LOG.info("SomeListener.contextDestroyed()");
		ServletContext context = sce.getServletContext();

		@SuppressWarnings("unchecked")
		List<String> list = (List<String>) context.getAttribute("SomeListener.list");
		if (list == null) {
			throw new RuntimeException("Not initialized - no SomeListener.list attribute");
		}
		context.removeAttribute("SomeListener.list");
	}

}
