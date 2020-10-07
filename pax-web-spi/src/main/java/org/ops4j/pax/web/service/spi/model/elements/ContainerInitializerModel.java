/*
 * Copyright 2010 Achim Nierbeck
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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.servlet.ServletContainerInitializer;

import org.ops4j.pax.web.service.spi.model.events.ContainerInitializerEventData;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWebContainerView;

/**
 * Model for {@link ServletContainerInitializer}. It'll never (at least for now) be registered through Whiteboard,
 * so we don't have to pay attention to OSGi service references. And we will never (for now) be able to register
 * single SCI into multiple {@link org.ops4j.pax.web.service.spi.servlet.OsgiServletContext} instances. Only one -
 * the one associated with a bundle to which an instance of {@link org.ops4j.pax.web.service.WebContainer} is scoped.
 */
public class ContainerInitializerModel extends ElementModel<ServletContainerInitializer, ContainerInitializerEventData> {

	private final ServletContainerInitializer containerInitializer;
	private final Set<Class<?>> classes = new LinkedHashSet<>();

	// if SCI was created because ServletModel for JSP was added, we need to remember this
	private ServletModel relatedModel;

	public ContainerInitializerModel(ServletContainerInitializer containerInitializer, Class<?>[] classes) {
		this.containerInitializer = containerInitializer;
		if (classes != null) {
			this.classes.addAll(Arrays.asList(classes));
		}
	}

	@Override
	public void register(WhiteboardWebContainerView view) {
		// for now it's NOOP, because there's no Whiteboard support for SCI
		// but we may rething this method (and unregister) in pax-web-extender-war
	}

	@Override
	public void unregister(WhiteboardWebContainerView view) {
	}

	@Override
	public ContainerInitializerEventData asEventData() {
		ContainerInitializerEventData data = new ContainerInitializerEventData(containerInitializer);
		setCommonEventProperties(data);
		return data;
	}

	public ServletContainerInitializer getContainerInitializer() {
		return containerInitializer;
	}

	/**
	 * @return the classes
	 */
	public Set<Class<?>> getClasses() {
		return classes;
	}

	@Override
	public Boolean performValidation() {
		if (containerInitializer == null) {
			throw new IllegalArgumentException("No ServletContainerInitializer is specified");
		}

		return Boolean.TRUE;
	}

	public void setRelatedModel(ServletModel model) {
		this.relatedModel = model;
	}

	public ServletModel getRelatedModel() {
		return relatedModel;
	}

}
