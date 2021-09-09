/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.service.spi.servlet;

import java.util.Collections;
import java.util.Enumeration;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.whiteboard.Preprocessor;

public class PreprocessorFilterConfig implements FilterConfig {

	private final FilterModel model;
	private final OsgiServletContext context;
	private boolean initCalled = false;

	private Preprocessor instance;
	private ServiceObjects<Filter> serviceObjects;

	public PreprocessorFilterConfig(FilterModel model, OsgiServletContext context) {
		this.model = model;
		this.context = context;
	}

	/**
	 * {@link FilterConfig} for particular {@link Preprocessor} is responsible for creation and destruction
	 * of the instance. It is especially important when {@link Preprocessor} is registered as prorotype OSGi service.
	 * @return
	 */
	public Preprocessor getInstance() {
		if (instance != null) {
			return instance;
		}

		// obtain Filter using reference
		ServiceReference<Filter> ref = model.getElementReference();
		if (ref != null) {
			if (!model.isPrototype()) {
				instance = (Preprocessor) model.getRegisteringBundle().getBundleContext().getService(ref);
			} else {
				serviceObjects = model.getRegisteringBundle().getBundleContext().getServiceObjects(ref);
				instance = (Preprocessor) serviceObjects.getService();
			}
		}
		if (instance == null && model.getFilterClass() != null) {
			try {
				instance = (Preprocessor) model.getFilterClass().newInstance();
			} catch (Exception e) {
				throw new IllegalStateException("Can't instantiate Preprocessor with class " + model.getFilterClass(), e);
			}
		}
		if (instance == null && model.getElementSupplier() != null) {
			instance = (Preprocessor) model.getElementSupplier().get();
		}

		if (instance == null) {
			model.setDtoFailureCode(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE);
		}

		return instance;
	}

	public void destroy() {
		if (instance != null) {
			instance.destroy();
			instance = null;
			if (model.getElementReference() != null) {
				if (!model.isPrototype()) {
					model.getRegisteringBundle().getBundleContext().ungetService(model.getElementReference());
				} else {
					serviceObjects.ungetService(getInstance());
					serviceObjects = null;
				}
			}
		}
	}

	public FilterModel getModel() {
		return model;
	}

	@Override
	public String getFilterName() {
		return model.getName();
	}

	@Override
	public ServletContext getServletContext() {
		return context;
	}

	@Override
	public String getInitParameter(String name) {
		return model.getInitParams().get(name);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return Collections.enumeration(model.getInitParams().keySet());
	}

	public void setInitCalled(boolean initCalled) {
		this.initCalled = initCalled;
	}

	public boolean isInitCalled() {
		return initCalled;
	}

	public void copyFrom(PreprocessorFilterConfig pfc) {
		instance = pfc.instance;
		serviceObjects = pfc.serviceObjects;
	}

}
