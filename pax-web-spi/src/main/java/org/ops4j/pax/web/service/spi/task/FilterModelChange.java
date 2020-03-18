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

import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.ServiceModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;

public class FilterModelChange extends Change {

	private final ServerModel serverModel;
	private final ServiceModel serviceModel;
	private final FilterModel filterModel;
	private final boolean disabled;

	public FilterModelChange(OpCode op, ServiceModel serviceModel, FilterModel filterModel) {
		super(op);
		this.serverModel = null;
		this.serviceModel = serviceModel;
		this.filterModel = filterModel;
		this.disabled = false;
	}

	public FilterModelChange(OpCode op, ServerModel serverModel, FilterModel filterModel) {
		this(op, serverModel, filterModel, false);
	}

	public FilterModelChange(OpCode op, ServerModel serverModel, FilterModel filterModel, boolean disabled) {
		super(op);
		this.serverModel = serverModel;
		this.serviceModel = null;
		this.filterModel = filterModel;
		this.disabled = disabled;
	}

	public ServerModel getServerModel() {
		return serverModel;
	}

	public ServiceModel getServiceModel() {
		return serviceModel;
	}

	public FilterModel getFilterModel() {
		return filterModel;
	}

	public boolean isDisabled() {
		return disabled;
	}

	@Override
	public void accept(BatchVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public String toString() {
		return getKind() + ": " + filterModel + (disabled ? " (disabled)" : " (enabled)");
	}

}
