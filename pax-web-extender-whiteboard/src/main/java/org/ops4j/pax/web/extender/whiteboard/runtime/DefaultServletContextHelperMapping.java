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

import java.util.Arrays;

import org.ops4j.pax.web.service.whiteboard.ServletContextHelperMapping;
import org.osgi.service.servlet.context.ServletContextHelper;

public class DefaultServletContextHelperMapping extends AbstractContextMapping implements ServletContextHelperMapping {

	private ServletContextHelper servletContextHelper;

	@Override
	public ServletContextHelper getServletContextHelper() {
		return servletContextHelper;
	}

	public void setServletContextHelper(ServletContextHelper servletContextHelper) {
		this.servletContextHelper = servletContextHelper;
	}

	@Override
	public String toString() {
		return "DefaultHttpContextMapping{"
				+ "servletContextHelper=" + servletContextHelper
				+ ", contextId='" + getContextId() + '\''
				+ ", contextPath='" + getContextPath() + '\''
				+ ", initParameters=" + getInitParameters()
				+ ", virtualHosts=" + Arrays.toString(getVirtualHosts())
				+ '}';
	}

}
