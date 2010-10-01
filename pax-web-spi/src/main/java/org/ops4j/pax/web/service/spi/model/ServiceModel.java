/* Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.spi.model;

import java.util.Collection;
import java.util.EventListener;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import org.osgi.service.http.HttpContext;
import org.ops4j.lang.NullArgumentException;

public class ServiceModel {

	private final Map<String, ServletModel> m_aliasMapping;
	private final Map<Servlet, ServletModel> m_servletModels;
	private final Map<Filter, FilterModel> m_filterModels;
	private final Map<EventListener, EventListenerModel> m_eventListenerModels;
	private final Map<String, LoginConfigModel> m_loginConfigModels;
	/**
	 * Mapping between the error and error page model.
	 */
	private final Map<String, ErrorPageModel> m_errorPageModels;
	private final Map<HttpContext, ContextModel> m_contextModels;
	private final Map<String, SecurityConstraintMappingModel> m_securityConstraintMappingModels;

	public ServiceModel() {
		m_aliasMapping = new HashMap<String, ServletModel>();
		m_servletModels = new HashMap<Servlet, ServletModel>();
		m_filterModels = new LinkedHashMap<Filter, FilterModel>();
		m_eventListenerModels = new HashMap<EventListener, EventListenerModel>();
		m_errorPageModels = new HashMap<String, ErrorPageModel>();
		m_contextModels = new HashMap<HttpContext, ContextModel>();
		m_loginConfigModels = new HashMap<String, LoginConfigModel>(); //PAXWEB-210 -- added these her too.
		m_securityConstraintMappingModels = new HashMap<String, SecurityConstraintMappingModel>();
	}

	public synchronized ServletModel getServletModelWithAlias(final String alias) {
		NullArgumentException.validateNotEmpty(alias, "Alias");
		return m_aliasMapping.get(alias);
	}

	public synchronized void addServletModel(final ServletModel model) {
		if (model.getAlias() != null) {
			m_aliasMapping.put(model.getAlias(), model);
		}
		m_servletModels.put(model.getServlet(), model);
		addContextModel(model.getContextModel());
	}

	public synchronized void removeServletModel(final ServletModel model) {
		if (model.getAlias() != null) {
			m_aliasMapping.remove(model.getAlias());
		}
		m_servletModels.remove(model.getServlet());
	}

	public ServletModel removeServlet(final Servlet servlet) {
		final ServletModel model;
		synchronized (m_servletModels) {
			model = m_servletModels.get(servlet);
			if (model == null) {
				throw new IllegalArgumentException("Servlet [" + servlet
						+ " is not currently registered in any context");
			}
			m_servletModels.remove(servlet);
			return model;
		}
	}

	public void addEventListenerModel(final EventListenerModel model) {
		synchronized (m_eventListenerModels) {
			if (m_eventListenerModels.containsKey(model.getEventListener())) {
				throw new IllegalArgumentException("Listener ["
						+ model.getEventListener() + "] already registered.");
			}
			m_eventListenerModels.put(model.getEventListener(), model);
			addContextModel(model.getContextModel());
		}
	}

	public EventListenerModel removeEventListener(final EventListener listener) {
		final EventListenerModel model;
		synchronized (m_eventListenerModels) {
			model = m_eventListenerModels.get(listener);
			if (model == null) {
				throw new IllegalArgumentException("Listener [" + listener
						+ " is not currently registered in any context");
			}
			m_eventListenerModels.remove(listener);
			return model;
		}
	}

	public void addFilterModel(final FilterModel model) {
		synchronized (m_filterModels) {
			if (m_filterModels.containsKey(model.getFilter())) {
				throw new IllegalArgumentException("Filter ["
						+ model.getFilter() + "] is already registered.");
			}
			m_filterModels.put(model.getFilter(), model);
			addContextModel(model.getContextModel());
		}
	}

	public FilterModel removeFilter(final Filter filter) {
		final FilterModel model;
		synchronized (m_filterModels) {
			model = m_filterModels.get(filter);
			if (model == null) {
				throw new IllegalArgumentException("Filter [" + filter
						+ " is not currently registered in any context");
			}
			m_filterModels.remove(filter);
			return model;
		}
	}

	public ServletModel[] getServletModels() {
		final Collection<ServletModel> models = m_servletModels.values();
		return models.toArray(new ServletModel[models.size()]);
	}

	public EventListenerModel[] getEventListenerModels() {
		final Collection<EventListenerModel> models = m_eventListenerModels
				.values();
		return models.toArray(new EventListenerModel[models.size()]);
	}

	public FilterModel[] getFilterModels() {
		final Collection<FilterModel> models = m_filterModels.values();
		return models.toArray(new FilterModel[models.size()]);
	}

	public ErrorPageModel[] getErrorPageModels() {
		final Collection<ErrorPageModel> models = m_errorPageModels.values();
		return models.toArray(new ErrorPageModel[models.size()]);
	}

	public void addContextModel(final ContextModel contextModel) {
		if (!m_contextModels.containsKey(contextModel.getHttpContext())) {
			m_contextModels.put(contextModel.getHttpContext(), contextModel);
		}
	}

	public ContextModel[] getContextModels() {
		final Collection<ContextModel> contextModels = m_contextModels.values();
		if (contextModels == null || contextModels.size() == 0) {
			return new ContextModel[0];
		}
		return contextModels.toArray(new ContextModel[contextModels.size()]);
	}

	public ContextModel getContextModel(final HttpContext httpContext) {
		return m_contextModels.get(httpContext);
	}

	public void addErrorPageModel(final ErrorPageModel model) {
		synchronized (m_errorPageModels) {
			final String key = model.getError() + "|"
					+ model.getContextModel().getId();
			if (m_errorPageModels.containsKey(key)) {
				throw new IllegalArgumentException("Error page for ["
						+ model.getError() + "] already registered.");
			}
			m_errorPageModels.put(key, model);
			addContextModel(model.getContextModel());
		}
	}

	public ErrorPageModel removeErrorPage(final String error,
			final ContextModel contextModel) {
		final ErrorPageModel model;
		synchronized (m_errorPageModels) {
			final String key = error + "|" + contextModel.getId();
			model = m_errorPageModels.get(key);
			if (model == null) {
				throw new IllegalArgumentException("Error page for [" + error
						+ "] cannot be found in the provided http context");
			}
			m_errorPageModels.remove(key);
			return model;
		}
	}

	public void addLoginModel(LoginConfigModel model) {
		synchronized (m_loginConfigModels) {
			if (m_loginConfigModels.containsKey(model.getRealmName())) {
				throw new IllegalArgumentException("Login Config ["
						+ model.getRealmName() + "] is already registered.");
			}
			m_loginConfigModels.put(model.getRealmName(), model);
			addContextModel(model.getContextModel());
		}
	}
	
	public LoginConfigModel[] getLoginModels() {
		Collection<LoginConfigModel> loginModels = m_loginConfigModels.values();
		return loginModels.toArray(new LoginConfigModel[loginModels.size()]);
	}

	public void addSecurityConstraintMappingModel(SecurityConstraintMappingModel model) {
		synchronized (m_securityConstraintMappingModels) {
			if (m_securityConstraintMappingModels.containsKey(model.getConstraintName())) {
				throw new IllegalArgumentException("Security Mapping ["
						+ model.getConstraintName() + "] is already registered.");
			}
			m_securityConstraintMappingModels.put(model.getConstraintName(), model);
			addContextModel(model.getContextModel());
		}
	}
	
	public SecurityConstraintMappingModel[] getSecurityConstraintMappings() {
		Collection<SecurityConstraintMappingModel> collection = m_securityConstraintMappingModels.values();
		return collection.toArray(new SecurityConstraintMappingModel[collection.size()]);
	}

	/**
	 * Returns true if the sevice can still be configured. This is possible
	 * before any web components (servlets / filters / listeners / error pages)
	 * are registered. TODO verify what happen once the web elements are
	 * registered and then unregistered. Can still be configured?
	 * 
	 * @return true, if service can be configured false otherwise
	 */
	public boolean canBeConfigured() {
		return m_servletModels.size() == 0 && m_filterModels.size() == 0
				&& m_eventListenerModels.size() == 0
				&& m_errorPageModels.size() == 0
				&& m_loginConfigModels.size() == 0;
	}

}