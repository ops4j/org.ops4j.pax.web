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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;

public class ServletModelChange extends Change {

	private final ServerModel serverModel;
	private ServletModel servletModel;
	private final Map<ServletModel, Boolean> servletModels = new LinkedHashMap<>();
	private boolean disabled;
	private final List<OsgiContextModel> newModels = new LinkedList<>();

	public ServletModelChange(OpCode op, ServerModel serverModel, ServletModel servletModel,
				OsgiContextModel ... newModels) {
		this(op, serverModel, servletModel, false, newModels);
	}

	public ServletModelChange(OpCode op, ServerModel serverModel, Map<ServletModel, Boolean> servletModels) {
		super(op);
		this.serverModel = serverModel;
		this.servletModels.putAll(servletModels);
	}

	public ServletModelChange(OpCode op, ServerModel serverModel, ServletModel servletModel, boolean disabled,
				OsgiContextModel ... newModels) {
		super(op);
		this.serverModel = serverModel;
		this.servletModel = servletModel;
		this.servletModels.put(servletModel, !disabled);
		this.disabled = disabled;
		this.newModels.addAll(Arrays.asList(newModels));
	}

	public ServerModel getServerModel() {
		return serverModel;
	}

	public ServletModel getServletModel() {
		return servletModel;
	}

	public Map<ServletModel, Boolean> getServletModels() {
		return servletModels;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public boolean isDynamic() {
		return servletModel.isDynamic();
	}

	public List<OsgiContextModel> getNewModels() {
		return newModels;
	}

	@Override
	public void accept(BatchVisitor visitor) {
		visitor.visit(this);
	}

	public List<OsgiContextModel> getContextModels() {
		return newModels.size() > 0 ? newModels : servletModel.getContextModels();
	}

	@Override
	public String toString() {
		ServletModel model = servletModel;
		if (model == null && servletModels.size() == 1) {
			model = servletModels.keySet().iterator().next();
		}
		if (model != null) {
			return getKind() + ": " + model + (disabled ? " (disabled)" : " (enabled)");
		} else {
			return getKind() + ": " + servletModels.size() + " servlet models";
		}
	}

}
