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
import org.ops4j.pax.web.service.spi.model.elements.WelcomeFileModel;

public class WelcomeFileModelChange extends Change {

	private final WelcomeFileModel welcomeFileModel;
	private final List<OsgiContextModel> newModels = new LinkedList<>();

	public WelcomeFileModelChange(OpCode op, WelcomeFileModel model, OsgiContextModel... newModels) {
		super(op);
		this.welcomeFileModel = model;
		this.newModels.addAll(Arrays.asList(newModels));
	}

	public List<OsgiContextModel> getNewModels() {
		return newModels;
	}

	@Override
	public void uninstall(List<Change> operations) {
		if (getKind() == OpCode.ADD) {
			operations.add(new WelcomeFileModelChange(OpCode.DELETE, welcomeFileModel));
		}
	}

	@Override
	public void accept(BatchVisitor visitor) {
		visitor.visit(this);
	}

	public List<OsgiContextModel> getContextModels() {
		return newModels.size() > 0 ? newModels : welcomeFileModel.getContextModels();
	}

	public WelcomeFileModel getWelcomeFileModel() {
		return welcomeFileModel;
	}

}
