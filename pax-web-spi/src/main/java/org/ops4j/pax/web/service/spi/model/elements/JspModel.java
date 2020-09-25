/*
 * Copyright 2008 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.spi.model.elements;

import java.util.Arrays;
import java.util.Map;

import org.ops4j.pax.web.service.spi.model.events.JspEventData;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWebContainerView;
import org.ops4j.pax.web.service.whiteboard.JspMapping;

public class JspModel extends ElementModel<JspMapping, JspEventData> {

	private final String[] mappings;
	private final String jspFile;

	private Map<String, String> initParams;

	public JspModel(String[] mappings, String jspFile) {
		this.mappings = mappings;
		this.jspFile = jspFile;
	}

	@Override
	public void register(WhiteboardWebContainerView view) {
		view.registerJsp(this);
	}

	@Override
	public void unregister(WhiteboardWebContainerView view) {
		view.unregisterJsp(this);
	}

	@Override
	public JspEventData asEventData() {
		JspEventData data = new JspEventData(mappings, jspFile);
		setCommonEventProperties(data);
		return data;
	}

	public String[] getMappings() {
		return mappings;
	}

	public String getJspFile() {
		return jspFile;
	}

	public Map<String, String> getInitParams() {
		return initParams;
	}

	public void setInitParams(Map<String, String> initParams) {
		this.initParams = initParams;
	}

	@Override
	public String toString() {
		return "JspModel{id=" + getId()
				+ ",mappings=" + Arrays.asList(mappings)
				+ ",jspFile=" + jspFile
				+ "}";
	}

	@Override
	public Boolean performValidation() {

		return Boolean.TRUE;
	}

}
