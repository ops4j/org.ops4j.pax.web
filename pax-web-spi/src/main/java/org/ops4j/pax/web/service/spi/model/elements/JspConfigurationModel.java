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
package org.ops4j.pax.web.service.spi.model.elements;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.descriptor.JspPropertyGroupDescriptor;
import javax.servlet.descriptor.TaglibDescriptor;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;

/**
 * <p>Set of parameters to configure JSP parameters, referenced from {@link OsgiContextModel}. Eventually
 * these parameters are used to configure server context's JSP servlet.</p>
 *
 * <p>This model reflects {@code <jsp-config>} element from {@code web.xml}.</p>
 */
public class JspConfigurationModel extends ElementModel implements JspConfigDescriptor {

	/** {@code <jsp-config>/<taglib>} */
	private final List<TaglibDescriptor> taglibDescriptors = new ArrayList<>();

	/** {@code <jsp-config>/<jsp-property-group>} */
	private final List<JspPropertyGroupDescriptor> jspPropertyGroupDescriptors = new ArrayList<>();

	JspConfigurationModel(List<OsgiContextModel> contextModels) {
//		super(contextModels);
	}

	@Override
	public Collection<TaglibDescriptor> getTaglibs() {
		return taglibDescriptors;
	}

	@Override
	public Collection<JspPropertyGroupDescriptor> getJspPropertyGroups() {
		return jspPropertyGroupDescriptors;
	}

}
