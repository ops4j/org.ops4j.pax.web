/* Copyright 2010 Achim Nierbeck
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
import java.util.HashSet;
import javax.servlet.ServletContainerInitializer;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;

/**
 * @author achim
 */
public class ContainerInitializerModel extends ElementModel {

	private final ServletContainerInitializer containerInitializer;
	private final HashSet<Class<?>> classes;

	public ContainerInitializerModel(OsgiContextModel contextModel,
									 ServletContainerInitializer containerInitializer, Class<?>[] classes) {
//		super(contextModel);
		this.containerInitializer = containerInitializer;
		this.classes = new HashSet<>(Arrays.asList(classes));
	}

	/**
	 * @return the containerInitializer
	 */
	public ServletContainerInitializer getContainerInitializer() {
		return containerInitializer;
	}

	/**
	 * @return the classes
	 */
	public HashSet<Class<?>> getClasses() {
		return classes;
	}

	@Override
	public Boolean performValidation() {
		return Boolean.TRUE;
	}

}
