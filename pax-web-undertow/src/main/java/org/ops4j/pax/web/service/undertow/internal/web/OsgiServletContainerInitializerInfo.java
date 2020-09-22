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
package org.ops4j.pax.web.service.undertow.internal.web;

import javax.servlet.ServletContainerInitializer;

import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.servlet.api.ServletContainerInitializerInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import org.ops4j.pax.web.service.spi.model.elements.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.servlet.OsgiDynamicServletContext;
import org.ops4j.pax.web.service.spi.servlet.RegisteringContainerInitializer;
import org.ops4j.pax.web.service.spi.servlet.SCIWrapper;

/**
 * This {@link ServletContainerInitializerInfo} allows to clear underlying {@link ContainerInitializerModel}.
 */
public class OsgiServletContainerInitializerInfo extends ServletContainerInitializerInfo {

	private final ContainerInitializerModel model;

	public OsgiServletContainerInitializerInfo(ContainerInitializerModel model, OsgiDynamicServletContext context) {
		super(model.getContainerInitializer().getClass(), new SCIInstanceFactory(context, model), model.getClasses());
		this.model = model;
	}

	public OsgiServletContainerInitializerInfo(RegisteringContainerInitializer initializer) {
		super(initializer.getClass(), new ImmediateInstanceFactory<>(initializer), null);
		this.model = null;
	}

	public ContainerInitializerModel getModel() {
		return model;
	}

	private static final class SCIInstanceFactory implements InstanceFactory<ServletContainerInitializer> {
		private final OsgiDynamicServletContext context;
		// this will get cleared after calling onStartup() and we also don't want to keep the reference
		// after user calls org.ops4j.pax.web.service.WebContainer.unregisterServletContainerInitializer()
		private ContainerInitializerModel model;

		SCIInstanceFactory(OsgiDynamicServletContext context, ContainerInitializerModel model) {
			this.context = context;
			this.model = model;
		}

		@Override
		public InstanceHandle<ServletContainerInitializer> createInstance() throws InstantiationException {
			return new InstanceHandle<ServletContainerInitializer>() {
				@Override
				public ServletContainerInitializer getInstance() {
					if (SCIInstanceFactory.this.model != null) {
						// return a wrapper that changes the ServletContext used
						return new SCIWrapper(SCIInstanceFactory.this.context, SCIInstanceFactory.this.model);
					} else {
						// return something that doesn't do anything in onStartup()
						return (c, ctx) -> { };
					}
				}

				@Override
				public void release() {
					SCIInstanceFactory.this.model = null;
				}
			};
		}
	}

}
