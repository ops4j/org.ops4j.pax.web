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
import org.ops4j.pax.web.service.spi.model.elements.WebSocketModel;

public class WebSocketModelChange extends Change {

	private WebSocketModel wsModel;
	private final Map<WebSocketModel, Boolean> wsModels = new LinkedHashMap<>();
	private boolean disabled;
	private final List<OsgiContextModel> newModels = new LinkedList<>();
	private String newModelsInfo;

	public WebSocketModelChange(OpCode op, WebSocketModel wsModel, OsgiContextModel ... newModels) {
		this(op, wsModel, false, newModels);
	}

	public WebSocketModelChange(OpCode op, Map<WebSocketModel, Boolean> wsModels) {
		super(op);
		this.wsModels.putAll(wsModels);
	}

	public WebSocketModelChange(OpCode op, WebSocketModel wsModel, boolean disabled, OsgiContextModel ... newModels) {
		super(op);
		this.wsModel = wsModel;
		this.wsModels.put(wsModel, !disabled);
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
			Map<WebSocketModel, Boolean> models = new HashMap<>();
			models.put(this.wsModel, true);
			operations.add(new WebSocketModelChange(OpCode.DELETE, models));
		}
	}

	public WebSocketModel getWebSocketModel() {
		return wsModel;
	}

	public Map<WebSocketModel, Boolean> getWebSocketModels() {
		return wsModels;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public List<OsgiContextModel> getNewModels() {
		return newModels;
	}

	public String getNewModelsInfo() {
		return newModelsInfo;
	}

	@Override
	public void accept(BatchVisitor visitor) {
		visitor.visitWebSocketModelChange(this);
	}

	public List<OsgiContextModel> getContextModels() {
		return newModels.size() > 0 ? newModels : wsModel.getContextModels();
	}

	@Override
	public String toString() {
		WebSocketModel model = wsModel;
		if (model == null && wsModels.size() == 1) {
			model = wsModels.keySet().iterator().next();
		}
		if (model != null) {
			return getKind() + ": " + model + (disabled ? " (disabled)" : " (enabled)")
					+ (newModelsInfo != null ? " (new contexts: " + newModelsInfo : "");
		} else {
			return getKind() + ": " + wsModels.size() + " web socket models";
		}
	}

}
