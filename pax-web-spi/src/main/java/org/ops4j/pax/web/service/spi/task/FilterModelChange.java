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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;

public class FilterModelChange extends Change {

	private FilterModel filterModel;
	private final List<FilterModel> filterModels = new LinkedList<>();
	private boolean disabled;
	private final List<OsgiContextModel> newModels = new LinkedList<>();

	public FilterModelChange(OpCode op, FilterModel filterModel, OsgiContextModel ... newModels) {
		this(op, filterModel, false, newModels);
	}

	public FilterModelChange(OpCode op, List<FilterModel> filterModels) {
		super(op);
		this.filterModels.addAll(filterModels);
	}

	public FilterModelChange(OpCode op, FilterModel filterModel, boolean disabled, OsgiContextModel ... newModels) {
		super(op);
		this.filterModel = filterModel;
		this.filterModels.add(filterModel);
		this.disabled = disabled;
		this.newModels.addAll(Arrays.asList(newModels));
	}

	public FilterModel getFilterModel() {
		return filterModel;
	}

	public List<FilterModel> getFilterModels() {
		return filterModels;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public boolean isDynamic() {
		return filterModel != null && filterModel.isDynamic();
	}

	public List<OsgiContextModel> getNewModels() {
		return newModels;
	}

	@Override
	public void uninstall(List<Change> operations) {
		if (getKind() == OpCode.ADD) {
			operations.add(new FilterModelChange(OpCode.DELETE, Collections.singletonList(filterModel)));
		}
	}

	@Override
	public void accept(BatchVisitor visitor) {
		visitor.visitFilterModelChange(this);
	}

	public List<OsgiContextModel> getContextModels() {
		return newModels.size() > 0 ? newModels : filterModel.getContextModels();
	}

	@Override
	public String toString() {
		FilterModel model = filterModel;
		if (model == null && filterModels.size() == 1) {
			model = filterModels.get(0);
		}
		if (model != null) {
			return getKind() + ": " + model + (disabled ? " (disabled)" : " (enabled)");
		} else {
			return getKind() + ": " + filterModels.size() + " filter models";
		}
	}

}
