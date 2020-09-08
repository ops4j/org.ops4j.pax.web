/*
 * Copyright 2020 ops4j
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

import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.elements.ContainerInitializerModel;

public class ContainerInitializerModelChange extends Change {

	private final ServerModel serverModel;
	private final List<ContainerInitializerModel> containerInitializerModels = new LinkedList<>();
	private ContainerInitializerModel containerInitializerModel;

	public ContainerInitializerModelChange(OpCode op, ServerModel serverModel, ContainerInitializerModel containerInitializerModel) {
		super(op);
		this.serverModel = serverModel;
		this.containerInitializerModels.add(containerInitializerModel);
		this.containerInitializerModel = containerInitializerModel;
	}

	public ContainerInitializerModelChange(OpCode op, ServerModel serverModel, List<ContainerInitializerModel> containerInitializerModels) {
		super(op);
		this.serverModel = serverModel;
		this.containerInitializerModels.addAll(containerInitializerModels);
	}

	public ServerModel getServerModel() {
		return serverModel;
	}

	public ContainerInitializerModel getContainerInitializerModel() {
		return containerInitializerModel;
	}

	public List<ContainerInitializerModel> getContainerInitializerModels() {
		return containerInitializerModels;
	}

	@Override
	public void accept(BatchVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public String toString() {
		return getKind() + ": " + containerInitializerModel;
	}

}
