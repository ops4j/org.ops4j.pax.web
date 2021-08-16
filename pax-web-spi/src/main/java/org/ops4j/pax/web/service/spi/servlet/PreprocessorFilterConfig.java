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
package org.ops4j.pax.web.service.spi.servlet;

import java.util.Collections;
import java.util.Enumeration;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import org.ops4j.pax.web.service.spi.model.elements.FilterModel;

public class PreprocessorFilterConfig implements FilterConfig {

	private final FilterModel model;
	private final OsgiServletContext context;
	private boolean initCalled = false;

	public PreprocessorFilterConfig(FilterModel model, OsgiServletContext context) {
		this.model = model;
		this.context = context;
	}

	@Override
	public String getFilterName() {
		return model.getName();
	}

	@Override
	public ServletContext getServletContext() {
		return context;
	}

	@Override
	public String getInitParameter(String name) {
		return model.getInitParams().get(name);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return Collections.enumeration(model.getInitParams().keySet());
	}

	public void setInitCalled(boolean initCalled) {
		this.initCalled = initCalled;
	}

	public boolean isInitCalled() {
		return initCalled;
	}

}
