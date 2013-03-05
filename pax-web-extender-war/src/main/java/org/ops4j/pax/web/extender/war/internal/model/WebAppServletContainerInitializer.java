/*
 * Copyright 2011 Achim Nierbeck.
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
package org.ops4j.pax.web.extender.war.internal.model;

import javax.servlet.ServletContainerInitializer;

/**
 * Model for {@link ServletContainerInitializer}s.
 * 
 * @author achim
 * 
 */
public class WebAppServletContainerInitializer {
	private ServletContainerInitializer servletContainerInitializer;
	private Class<?>[] classes;

	/**
	 * @return the servletContainerInitializer
	 */
	public ServletContainerInitializer getServletContainerInitializer() {
		return servletContainerInitializer;
	}

	/**
	 * @param servletContainerInitializer
	 *            the servletContainerInitializer to set
	 */
	public void setServletContainerInitializer(
			ServletContainerInitializer servletContainerInitializer) {
		this.servletContainerInitializer = servletContainerInitializer;
	}

	/**
	 * @return the classes
	 */
	public Class<?>[] getClasses() {
		return classes;
	}

	/**
	 * @param classes
	 *            the classes to set
	 */
	public void setClasses(Class<?>[] classes) {
		this.classes = classes;
	}

}
