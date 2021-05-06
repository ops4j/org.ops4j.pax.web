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
import java.util.LinkedList;
import java.util.List;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ErrorPageModel;

public class ErrorPageModelChange extends Change {

	private ErrorPageModel errorPageModel;
	private final List<ErrorPageModel> errorPageModels = new LinkedList<>();
	private boolean disabled;
	private final List<OsgiContextModel> newModels = new LinkedList<>();

	public ErrorPageModelChange(OpCode kind, ErrorPageModel model, OsgiContextModel ... newModels) {
		super(kind);
		this.errorPageModel = model;
		this.errorPageModels.add(model);
		this.newModels.addAll(Arrays.asList(newModels));
	}

	public ErrorPageModelChange(OpCode op, List<ErrorPageModel> errorPageModels) {
		super(op);
		this.errorPageModels.addAll(errorPageModels);
	}

	public ErrorPageModelChange(OpCode op, ErrorPageModel filterModel, boolean disabled, OsgiContextModel ... newModels) {
		super(op);
		this.errorPageModel = filterModel;
		this.errorPageModels.add(filterModel);
		this.disabled = disabled;
		this.newModels.addAll(Arrays.asList(newModels));
	}

	public List<OsgiContextModel> getNewModels() {
		return newModels;
	}

	@Override
	public void uninstall(List<Change> operations) {
		if (getKind() == OpCode.ADD) {
			operations.add(new ErrorPageModelChange(OpCode.DELETE, errorPageModels));
		}
	}

	@Override
	public void accept(BatchVisitor visitor) {
		visitor.visit(this);
	}

	public List<OsgiContextModel> getContextModels() {
		return newModels.size() > 0 ? newModels : errorPageModel.getContextModels();
	}

	public ErrorPageModel getErrorPageModel() {
		return errorPageModel;
	}

	public List<ErrorPageModel> getErrorPageModels() {
		return errorPageModels;
	}

	public boolean isDisabled() {
		return disabled;
	}

}
