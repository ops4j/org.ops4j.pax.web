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

import java.util.List;

import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;

public class OsgiContextModelChange extends Change {

	private final WebContainerContext context;
	private final OsgiContextModel osgiContextModel;
	private final ServletContextModel servletContextModel;

	public OsgiContextModelChange(OpCode op, WebContainerContext context, OsgiContextModel osgiContextModel,
			ServletContextModel servletContextModel) {
		super(op);
		this.context = context;
		this.osgiContextModel = osgiContextModel;
		this.servletContextModel = servletContextModel;
	}

	@Override
	public void uninstall(List<Change> operations) {
		if (this.getKind() == OpCode.ADD) {
			operations.addAll(osgiContextModel.getUnregistrations());
			operations.add(new OsgiContextModelChange(OpCode.DELETE, null, osgiContextModel, null));
		} else if (this.getKind() == OpCode.ASSOCIATE) {
			operations.add(new OsgiContextModelChange(OpCode.DISASSOCIATE, context, osgiContextModel, null));
		}
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

	@Override
	public void accept(BatchVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public String toString() {
		return getKind() + ": " + osgiContextModel;
	}

}
