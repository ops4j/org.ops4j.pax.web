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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;

import org.ops4j.lang.NullArgumentException;
import org.osgi.service.http.HttpContext;

public class ServiceModel {

    private final Map<String, ServletModel> m_aliasMapping;
    private final Set<ServletModel> m_servletModels;
    private final Map<Filter, FilterModel> m_filterModels;
    private final Map<EventListener, EventListenerModel> m_eventListenerModels;
    private final Map<String, LoginConfigModel> m_loginConfigModels;
    /**
     * Mapping between the error and error page model.
     */
    private final Map<String, ErrorPageModel> m_errorPageModels;
    private final Map<HttpContext, ContextModel> m_contextModels;
    private final Map<String, SecurityConstraintMappingModel> m_securityConstraintMappingModels;
    private final Map<ServletContainerInitializer, ContainerInitializerModel> containerInitializers;

    public ServiceModel() {
        m_aliasMapping = new HashMap<String, ServletModel>();
        m_servletModels = new HashSet<ServletModel>();
        m_filterModels = new LinkedHashMap<Filter, FilterModel>();
        m_eventListenerModels = new HashMap<EventListener, EventListenerModel>();
        m_errorPageModels = new HashMap<String, ErrorPageModel>();
        m_contextModels = new HashMap<HttpContext, ContextModel>();
        m_loginConfigModels = new HashMap<String, LoginConfigModel>(); // PAXWEB-210
        // --
        // added
        // these
        // her
        // too.
        m_securityConstraintMappingModels = new HashMap<String, SecurityConstraintMappingModel>();
        containerInitializers = new HashMap<ServletContainerInitializer, ContainerInitializerModel>();
    }

    public synchronized ServletModel getServletModelWithAlias(final String alias) {
        NullArgumentException.validateNotEmpty(alias, "Alias");
        return m_aliasMapping.get(alias);
    }

    public synchronized void addServletModel(final ServletModel model) {
        if (model.getAlias() != null) {
            m_aliasMapping.put(model.getAlias(), model);
        }
        m_servletModels.add(model);
        addContextModel(model.getContextModel());
    }

    public synchronized void removeServletModel(final ServletModel model) {
        if (model.getAlias() != null) {
            m_aliasMapping.remove(model.getAlias());
        }
        m_servletModels.remove(model);
    }

    public synchronized ServletModel removeServlet(final Servlet servlet) {
        final ServletModel model = findServletModel(servlet);
        if (model == null) {
            throw new IllegalArgumentException("Servlet [" + servlet
                    + " is not currently registered in any context");
        }
        m_servletModels.remove(servlet);
        return model;
    }

    private synchronized ServletModel findServletModel(Servlet servlet) {
        for (ServletModel servletModel : m_servletModels) {
            if (servletModel.getServlet() != null && servletModel.getServlet().equals(servlet)) {
                return servletModel;
            }
        }
        return null;
    }

    public synchronized Set<ServletModel> removeServletClass(final Class<? extends Servlet> servletClass) {
        final Set<ServletModel> models = findServletModels(servletClass);
        if (models == null) {
            throw new IllegalArgumentException("Servlet class [" + servletClass
                    + " is not currently registered in any context");
        }
        m_servletModels.removeAll(models);
        return models;
    }

    private synchronized Set<ServletModel> findServletModels(final Class<? extends Servlet> servletClass) {
        Set<ServletModel> servletModels = null;
        for (ServletModel servletModel : m_servletModels) {
            if (servletModel.getServletClass() != null && servletModel.getServletClass().equals(servletClass)) {
                if (servletModels == null) {
                    servletModels = new HashSet<ServletModel>();
                }
                servletModels.add(servletModel);
            }
        }
        return servletModels;
    }

    public synchronized void addEventListenerModel(final EventListenerModel model) {
        if (m_eventListenerModels.containsKey(model.getEventListener())) {
            throw new IllegalArgumentException("Listener ["
                    + model.getEventListener() + "] already registered.");
        }
        m_eventListenerModels.put(model.getEventListener(), model);
        addContextModel(model.getContextModel());
    }

    public synchronized EventListenerModel removeEventListener(final EventListener listener) {
        final EventListenerModel model;
        model = m_eventListenerModels.get(listener);
        if (model == null) {
            throw new IllegalArgumentException("Listener [" + listener
                    + " is not currently registered in any context");
        }
        m_eventListenerModels.remove(listener);
        return model;
    }

    public synchronized void addFilterModel(final FilterModel model) {
        if (m_filterModels.containsKey(model.getFilter())) {
            throw new IllegalArgumentException("Filter ["
                    + model.getFilter() + "] is already registered.");
        }
        m_filterModels.put(model.getFilter(), model);
        addContextModel(model.getContextModel());
    }

    public synchronized FilterModel removeFilter(final Filter filter) {
        final FilterModel model;
        model = m_filterModels.get(filter);
        if (model == null) {
            throw new IllegalArgumentException("Filter [" + filter
                    + " is not currently registered in any context");
        }
        m_filterModels.remove(filter);
        return model;
    }

    public synchronized ServletModel[] getServletModels() {
        return m_servletModels.toArray(new ServletModel[m_servletModels.size()]);
    }

    public synchronized EventListenerModel[] getEventListenerModels() {
        final Collection<EventListenerModel> models = m_eventListenerModels.values();
        return models.toArray(new EventListenerModel[models.size()]);
    }

    public synchronized FilterModel[] getFilterModels() {
        final Collection<FilterModel> models = m_filterModels.values();
        return models.toArray(new FilterModel[models.size()]);
    }

    public synchronized ErrorPageModel[] getErrorPageModels() {
        final Collection<ErrorPageModel> models = m_errorPageModels.values();
        return models.toArray(new ErrorPageModel[models.size()]);
    }

    public synchronized void addContextModel(final ContextModel contextModel) {
        if (!m_contextModels.containsKey(contextModel.getHttpContext())) {
            m_contextModels.put(contextModel.getHttpContext(), contextModel);
        }
    }

    public synchronized ContextModel[] getContextModels() {
        final Collection<ContextModel> contextModels = m_contextModels.values();
        if (contextModels == null || contextModels.size() == 0) {
            return new ContextModel[0];
        }
        return contextModels.toArray(new ContextModel[contextModels.size()]);
    }

    public synchronized ContextModel getContextModel(final HttpContext httpContext) {
        return m_contextModels.get(httpContext);
    }

    public synchronized void addErrorPageModel(final ErrorPageModel model) {
        final String key = model.getError() + "|"
                + model.getContextModel().getId();
        if (m_errorPageModels.containsKey(key)) {
            throw new IllegalArgumentException("Error page for ["
                    + model.getError() + "] already registered.");
        }
        m_errorPageModels.put(key, model);
        addContextModel(model.getContextModel());
    }

    public synchronized ErrorPageModel removeErrorPage(final String error,
            final ContextModel contextModel) {
        final ErrorPageModel model;
        final String key = error + "|" + contextModel.getId();
        model = m_errorPageModels.get(key);
        if (model == null) {
            throw new IllegalArgumentException("Error page for [" + error
                    + "] cannot be found in the provided http context");
        }
        m_errorPageModels.remove(key);
        return model;
    }

    public synchronized void addLoginModel(LoginConfigModel model) {
        if (m_loginConfigModels.containsKey(model.getRealmName())) {
            throw new IllegalArgumentException("Login Config ["
                    + model.getRealmName() + "] is already registered.");
        }
        m_loginConfigModels.put(model.getRealmName(), model);
        addContextModel(model.getContextModel());
    }

    public synchronized LoginConfigModel[] getLoginModels() {
        Collection<LoginConfigModel> loginModels = m_loginConfigModels.values();
        return loginModels.toArray(new LoginConfigModel[loginModels.size()]);
    }

    public synchronized void addSecurityConstraintMappingModel(
            SecurityConstraintMappingModel model) {
        if (m_securityConstraintMappingModels.containsKey(model
                .getConstraintName())) {
            throw new IllegalArgumentException("Security Mapping ["
                    + model.getConstraintName()
                    + "] is already registered.");
        }
        m_securityConstraintMappingModels.put(model.getConstraintName(),
                model);
        addContextModel(model.getContextModel());
    }

    public synchronized SecurityConstraintMappingModel[] getSecurityConstraintMappings() {
        Collection<SecurityConstraintMappingModel> collection = m_securityConstraintMappingModels
                .values();
        return collection.toArray(new SecurityConstraintMappingModel[collection
                                                                     .size()]);
    }

    public synchronized void addContainerInitializerModel(ContainerInitializerModel model) {
        if (containerInitializers.containsKey(model.getContainerInitializer())) {
            throw new IllegalArgumentException("ServletContainerInitializer "
                    + model.getContainerInitializer() + " already registered");
        }
        containerInitializers.put(model.getContainerInitializer(), model);
    }

    public synchronized void removeContainerInitializerModel(ContainerInitializerModel model) {
        // TODO Auto-generated method stub

    }

    /**
     * Returns true if the context can still be configured. This is possible
     * before any web components (servlets / filters / listeners / error pages)
     * are registered. TODO verify what happen once the web elements are
     * registered and then unregistered. Can still be configured?
     * 
     * @param httpContext created by the service of this model
     * @return true, if context can be configured false otherwise
     */
    public synchronized boolean canBeConfigured(HttpContext httpContext) {
        return canBeConfigured(httpContext, m_servletModels)
                && canBeConfigured(httpContext, m_filterModels.values())
                && canBeConfigured(httpContext, m_eventListenerModels.values())
                && canBeConfigured(httpContext, m_errorPageModels.values())
                && canBeConfigured(httpContext, m_loginConfigModels.values());
    }

    private boolean canBeConfigured(HttpContext httpContext, Collection<? extends Model> models) {
        for (Model model : models) {
            ContextModel contextModel = model.getContextModel();
            HttpContext candidateHttpContext = contextModel.getHttpContext();
            if (candidateHttpContext.equals(httpContext)) {
                return false;
            }
        }
        return true;
    }
}