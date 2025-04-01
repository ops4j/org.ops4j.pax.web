package org.ops4j.pax.web.extender.samples.whiteboard;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.util.concurrent.atomic.AtomicReference;

/*
 * Copyright 2025 OPS4J.
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
public class TestSCL implements ServletContextListener {

	private final AtomicReference<ServletContext> ref = new AtomicReference<>();

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		ref.set(sce.getServletContext());
	}

	public AtomicReference<ServletContext> getRef() {
		return ref;
	}

}
