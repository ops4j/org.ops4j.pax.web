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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import jakarta.servlet.descriptor.JspConfigDescriptor;
import jakarta.servlet.descriptor.JspPropertyGroupDescriptor;
import jakarta.servlet.descriptor.TaglibDescriptor;

import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;

/**
 * <p>Set of parameters to configure JSP parameters, referenced from {@link OsgiContextModel}. Eventually
 * these parameters are used to configure server context's JSP servlet.</p>
 *
 * <p>This model reflects {@code <jsp-config>} element from {@code web.xml}.</p>
 *
 * <p>Differently than other <em>models</em> in this package, this class isn't an {@link ElementModel} - for now
 * it can't be registered via whiteboard and is used only internally by {@link OsgiContextModel}.</p>
 *
 * <p>The JSP configuration that can be found in {@code web.xml} is just tiny fraction of entire configuration that
 * can be passed ton Jasper engine and servlet. The remaining JSP configuration can be specified globally via
 * {@link Configuration#jsp()}.</p>
 */
public class JspConfigurationModel implements JspConfigDescriptor {

	/** {@code <jsp-config>/<taglib>} */
	private final List<TaglibDescriptor> taglibs = new LinkedList<>();

	/** {@code <jsp-config>/<jsp-property-group>} */
	private final List<JspPropertyGroupDescriptor> propertyGroups = new LinkedList<>();

	@Override
	public Collection<TaglibDescriptor> getTaglibs() {
		return taglibs;
	}

	@Override
	public Collection<JspPropertyGroupDescriptor> getJspPropertyGroups() {
		return propertyGroups;
	}

}
