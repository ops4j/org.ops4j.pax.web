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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;

public class ServletModelChange extends Change {

	private ServletModel servletModel;
	private final Map<ServletModel, Boolean> servletModels = new LinkedHashMap<>();
	private boolean disabled;
	private final List<OsgiContextModel> newModels = new LinkedList<>();
	private String newModelsInfo;

	public ServletModelChange(OpCode op, ServletModel servletModel, OsgiContextModel ... newModels) {
		this(op, servletModel, false, newModels);
	}

	public ServletModelChange(OpCode op, Map<ServletModel, Boolean> servletModels) {
		super(op);
		this.servletModels.putAll(servletModels);
	}

	public ServletModelChange(OpCode op, ServletModel servletModel, boolean disabled, OsgiContextModel ... newModels) {
		super(op);
		this.servletModel = servletModel;
		this.servletModels.put(servletModel, !disabled);
		this.disabled = disabled;
		this.newModels.addAll(Arrays.asList(newModels));
		if (this.newModels.size() > 0) {
			this.newModelsInfo = this.newModels.stream()
					.map(ocm -> String.format("{%s,%s,%s,%s}", ocm.isWhiteboard() ? "WB" : "HS", ocm.getId(), ocm.getName(), ocm.getContextPath()))
					.collect(Collectors.joining(", ", "[", "]"));
		}
	}

	@Override
	public void uninstall(List<Change> operations) {
		if (this.getKind() == OpCode.ADD) {
			Map<ServletModel, Boolean> models = new HashMap<>();
			models.put(this.servletModel, true);
			operations.add(new ServletModelChange(OpCode.DELETE, models));
		}
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

	public String getNewModelsInfo() {
		return newModelsInfo;
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
			return getKind() + ": " + model + (disabled ? " (disabled)" : " (enabled)")
					+ (newModelsInfo != null ? " (new contexts: " + newModelsInfo : "");
		} else {
			return getKind() + ": " + servletModels.size() + " servlet models";
		}
	}

}
