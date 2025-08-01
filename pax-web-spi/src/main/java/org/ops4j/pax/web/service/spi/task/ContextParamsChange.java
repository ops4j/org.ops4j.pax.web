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

import java.util.LinkedHashMap;
import java.util.Map;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;

/**
 * Change of context params ({@link jakarta.servlet.ServletContext#getInitParameter(String)}).
 */
public class ContextParamsChange extends Change {

	private final OsgiContextModel osgiContextModel;
	private final Map<String, String> params = new LinkedHashMap<>();

	public ContextParamsChange(OpCode op, OsgiContextModel osgiContextModel, Map<String, String> params) {
		super(op);
		this.osgiContextModel = osgiContextModel;
		this.params.putAll(params);
	}

	@Override
	public void accept(BatchVisitor visitor) {
		visitor.visitContextParamsChange(this);
	}

	public OsgiContextModel getOsgiContextModel() {
		return osgiContextModel;
	}

	public Map<String, String> getParams() {
		return params;
	}

}
