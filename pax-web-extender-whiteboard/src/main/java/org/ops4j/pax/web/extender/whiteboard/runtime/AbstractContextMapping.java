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
package org.ops4j.pax.web.extender.whiteboard.runtime;

import java.util.HashMap;
import java.util.Map;

import org.ops4j.pax.web.service.whiteboard.ContextMapping;

public abstract class AbstractContextMapping implements ContextMapping {

	private String contextId;
	private String contextPath;
	private Map<String, String> initParameters = new HashMap<>();
	private String[] virtualHosts = new String[0];
	private String[] connectors = new String[0];

	@Override
	public String getContextId() {
		return contextId;
	}

	@Override
	public String getContextPath() {
		return contextPath;
	}

	@Override
	public Map<String, String> getInitParameters() {
		return initParameters;
	}

	@Override
	public String[] getVirtualHosts() {
		return virtualHosts;
	}

	@Override
	public String[] getConnectors() {
		return connectors;
	}

	public void setContextId(String contextId) {
		this.contextId = contextId;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public void setInitParameters(Map<String, String> initParameters) {
		this.initParameters = initParameters;
	}

	public void setVirtualHosts(String[] virtualHosts) {
		this.virtualHosts = virtualHosts;
	}

	public void setConnectors(String[] connectors) {
		this.connectors = connectors;
	}

}
