/*
 * Copyright 2022 OPS4J.
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

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;

/**
 * Explicit order to stop the context.
 */
public class ContextStopChange extends Change {

	private final String contextPath;
	private final OsgiContextModel osgiContextModel;

	public ContextStopChange(OpCode op, String contextPath) {
		super(op);
		this.contextPath = contextPath;
		this.osgiContextModel = null;
	}

	public ContextStopChange(OpCode op, OsgiContextModel osgiContextModel) {
		super(op);
		this.contextPath = osgiContextModel.getContextPath();
		this.osgiContextModel = osgiContextModel;
	}

	@Override
	public void accept(BatchVisitor visitor) {
		visitor.visitContextStopChange(this);
	}

	public String getContextPath() {
		return contextPath;
	}

	public OsgiContextModel getOsgiContextModel() {
		return osgiContextModel;
	}

}
