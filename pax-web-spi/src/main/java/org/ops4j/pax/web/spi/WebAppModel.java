package org.ops4j.pax.web.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ops4j.pax.web.descriptor.gen.ErrorPageType;
import org.ops4j.pax.web.descriptor.gen.FilterMappingType;
import org.ops4j.pax.web.descriptor.gen.FilterType;
import org.ops4j.pax.web.descriptor.gen.JspConfigType;
import org.ops4j.pax.web.descriptor.gen.ListenerType;
import org.ops4j.pax.web.descriptor.gen.LoginConfigType;
import org.ops4j.pax.web.descriptor.gen.MimeMappingType;
import org.ops4j.pax.web.descriptor.gen.ParamValueType;
import org.ops4j.pax.web.descriptor.gen.SecurityConstraintType;
import org.ops4j.pax.web.descriptor.gen.SecurityRoleType;
import org.ops4j.pax.web.descriptor.gen.ServletMappingType;
import org.ops4j.pax.web.descriptor.gen.ServletType;
import org.ops4j.pax.web.descriptor.gen.SessionConfigType;
import org.ops4j.pax.web.descriptor.gen.WebAppType;
import org.ops4j.pax.web.descriptor.gen.WelcomeFileListType;

public class WebAppModel {

    private WebAppType webApp;
    private List<ParamValueType> contextParams = new ArrayList<>();
    private List<FilterType> filters = new ArrayList<>();
    private List<FilterMappingType> filterMappings = new ArrayList<>();
    private List<ListenerType> listeners = new ArrayList<>();
    private List<ServletType> servlets = new ArrayList<>();
    private List<ServletMappingType> servletMappings = new ArrayList<>();
    private SessionConfigType sessionConfig;
    private List<MimeMappingType> mimeMappings = new ArrayList<>();
    private WelcomeFileListType welcomeFileList;
    private List<ErrorPageType> errorPages = new ArrayList<>();
    private JspConfigType jspConfig;
    private List<SecurityConstraintType> securityConstraints = new ArrayList<>();
    private LoginConfigType loginConfig;
    private List<SecurityRoleType> securityRoles = new ArrayList<>();
    
    private Map<String, FilterMappingType> filterMappingMap = new HashMap<>();
    
    
    public WebAppModel() {
    }
    
    public WebAppModel(WebAppType webApp) {
        this.webApp = webApp;
    }

    /**
     * @return the webApp
     */
    public WebAppType getWebApp() {
        return webApp;
    }

    /**
     * @param webApp
     *            the webApp to set
     */
    public void setWebApp(WebAppType webApp) {
        this.webApp = webApp;
    }

    /**
     * @return the contextParams
     */
    public List<ParamValueType> getContextParams() {
        return contextParams;
    }

    /**
     * @param contextParams
     *            the contextParams to set
     */
    public void setContextParams(List<ParamValueType> contextParams) {
        this.contextParams = contextParams;
    }

    /**
     * @return the filters
     */
    public List<FilterType> getFilters() {
        return filters;
    }

    /**
     * @param filters
     *            the filters to set
     */
    public void setFilters(List<FilterType> filters) {
        this.filters = filters;
    }

    /**
     * @return the filterMappings
     */
    public List<FilterMappingType> getFilterMappings() {
        return filterMappings;
    }

    /**
     * @param filterMappings
     *            the filterMappings to set
     */
    public void setFilterMappings(List<FilterMappingType> filterMappings) {
        this.filterMappings = filterMappings;
    }

    /**
     * @return the listeners
     */
    public List<ListenerType> getListeners() {
        return listeners;
    }

    /**
     * @param listeners
     *            the listeners to set
     */
    public void setListeners(List<ListenerType> listeners) {
        this.listeners = listeners;
    }

    /**
     * @return the servlets
     */
    public List<ServletType> getServlets() {
        return servlets;
    }

    /**
     * @param servlets
     *            the servlets to set
     */
    public void setServlets(List<ServletType> servlets) {
        this.servlets = servlets;
    }

    /**
     * @return the servletMappings
     */
    public List<ServletMappingType> getServletMappings() {
        return servletMappings;
    }

    /**
     * @param servletMappings
     *            the servletMappings to set
     */
    public void setServletMappings(List<ServletMappingType> servletMappings) {
        this.servletMappings = servletMappings;
    }

    /**
     * @return the sessionConfig
     */
    public SessionConfigType getSessionConfig() {
        return sessionConfig;
    }

    /**
     * @param sessionConfig
     *            the sessionConfig to set
     */
    public void setSessionConfig(SessionConfigType sessionConfig) {
        this.sessionConfig = sessionConfig;
    }

    /**
     * @return the mimeMappings
     */
    public List<MimeMappingType> getMimeMappings() {
        return mimeMappings;
    }

    /**
     * @param mimeMappings
     *            the mimeMappings to set
     */
    public void setMimeMappings(List<MimeMappingType> mimeMappings) {
        this.mimeMappings = mimeMappings;
    }

    /**
     * @return the welcomeFileList
     */
    public WelcomeFileListType getWelcomeFileList() {
        return welcomeFileList;
    }

    /**
     * @param welcomeFileList
     *            the welcomeFileList to set
     */
    public void setWelcomeFileList(WelcomeFileListType welcomeFileList) {
        this.welcomeFileList = welcomeFileList;
    }

    /**
     * @return the errorPages
     */
    public List<ErrorPageType> getErrorPages() {
        return errorPages;
    }

    /**
     * @param errorPages
     *            the errorPages to set
     */
    public void setErrorPages(List<ErrorPageType> errorPages) {
        this.errorPages = errorPages;
    }

    /**
     * @return the jspConfig
     */
    public JspConfigType getJspConfig() {
        return jspConfig;
    }

    /**
     * @param jspConfig
     *            the jspConfig to set
     */
    public void setJspConfig(JspConfigType jspConfig) {
        this.jspConfig = jspConfig;
    }

    /**
     * @return the securityConstraints
     */
    public List<SecurityConstraintType> getSecurityConstraints() {
        return securityConstraints;
    }

    /**
     * @param securityConstraints
     *            the securityConstraints to set
     */
    public void setSecurityConstraints(List<SecurityConstraintType> securityConstraints) {
        this.securityConstraints = securityConstraints;
    }

    /**
     * @return the loginConfig
     */
    public LoginConfigType getLoginConfig() {
        return loginConfig;
    }

    /**
     * @param loginConfig
     *            the loginConfig to set
     */
    public void setLoginConfig(LoginConfigType loginConfig) {
        this.loginConfig = loginConfig;
    }

    /**
     * @return the securityRoles
     */
    public List<SecurityRoleType> getSecurityRoles() {
        return securityRoles;
    }

    /**
     * @param securityRoles
     *            the securityRoles to set
     */
    public void setSecurityRoles(List<SecurityRoleType> securityRoles) {
        this.securityRoles = securityRoles;
    }

    
    public void putFilterMapping(String filterName, FilterMappingType filterMapping) {
        filterMappingMap.put(filterName, filterMapping);
    }
    
    public FilterMappingType getFilterMapping(String filterName) {
        return filterMappingMap.get(filterName);
    }

    public ServletType findServlet(String servletName) {
        for (ServletType servlet : servlets) {
            if (servlet.getServletName().getValue().equals(servletName)) {
                return servlet;
            }
        }
        return null;
    }

    public boolean hasServletMapping(String servletName) {
        for (ServletMappingType servletMapping : servletMappings) {
            if (servletMapping.getServletName().getValue().equals(servletName)) {
                return true;
            }
        }
        return false;
    }
    
    public FilterType findFilter(String filterName) {
        for (FilterType filter : filters) {
            if (filter.getFilterName().getValue().endsWith(filterName)) {
                return filter;
            }
        }
        return null;
    }
    
    public boolean hasFilterMapping(String filterName) {
        for (FilterMappingType filterMapping : filterMappings) {
            if (filterMapping.getFilterName().getValue().equals(filterName)) {
                return true;
            }
        }
        return false;
    }
}
