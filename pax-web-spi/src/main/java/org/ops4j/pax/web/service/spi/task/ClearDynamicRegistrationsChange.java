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
package org.ops4j.pax.web.service.spi.task;

import java.util.LinkedList;
import java.util.List;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;

/**
 * A change that clears all dynamic registrations, potentially made using methods like
 * {@link javax.servlet.ServletContext#addServlet}
 */
public class ClearDynamicRegistrationsChange extends Change {

	private final List<OsgiContextModel> osgiContextModels = new LinkedList<>();

	public ClearDynamicRegistrationsChange(OpCode op, List<OsgiContextModel> osgiContextModels) {
		super(op);

		this.osgiContextModels.addAll(osgiContextModels);
	}

	@Override
	public List<OsgiContextModel> getContextModels() {
		return osgiContextModels;
	}

	@Override
	public void accept(BatchVisitor visitor) {
		visitor.visitClearDynamicRegistrationsChange(this);
	}

}
