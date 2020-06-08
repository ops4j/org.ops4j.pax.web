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
package org.ops4j.pax.web.service.spi.task;

import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;

public class OsgiContextModelChange extends Change {

	private final WebContainerContext context;
	private final OsgiContextModel osgiContextModel;
	private final ServletContextModel servletContextModel;
	// flag indicating that the change is for "default" OsgiContextModel created during initialization
	// of HttpServiceEnabled
	private final boolean defaultHttpContext;

	public OsgiContextModelChange(OpCode op, WebContainerContext context, OsgiContextModel osgiContextModel,
			ServletContextModel servletContextModel) {
		super(op);
		this.context = context;
		this.osgiContextModel = osgiContextModel;
		this.servletContextModel = servletContextModel;
		this.defaultHttpContext = false;
	}

	public OsgiContextModelChange(OpCode op, WebContainerContext context, OsgiContextModel osgiContextModel,
			ServletContextModel servletContextModel, boolean defaultHttpContext) {
		super(op);
		this.context = context;
		this.osgiContextModel = osgiContextModel;
		this.servletContextModel = servletContextModel;
		this.defaultHttpContext = defaultHttpContext;
	}

	public WebContainerContext getContext() {
		return context;
	}

	public OsgiContextModel getOsgiContextModel() {
		return osgiContextModel;
	}

	public ServletContextModel getServletContextModel() {
		return servletContextModel;
	}

	public boolean isDefaultHttpContext() {
		return defaultHttpContext;
	}

	@Override
	public void accept(BatchVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public String toString() {
		return getKind() + ": " + osgiContextModel;
	}

}
