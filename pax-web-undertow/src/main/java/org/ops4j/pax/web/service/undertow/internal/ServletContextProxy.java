/*
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
package org.ops4j.pax.web.service.undertow.internal;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

class ServletContextProxy implements ServletContext {

    private AtomicReference<ServletContext> servletContextRef = new AtomicReference<>();
    private Context context;
    private final Object lock = new Object();


    ServletContextProxy(Context context){
        this.context = context;
    }


    private void initServletContext(){
        try {
            synchronized (lock) {
                if(servletContextRef.get() == null) {
                    context.getHandler(servletContext -> servletContextRef.set(servletContext));
                }
            }
        } catch (ServletException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getContextPath() {
        if(servletContextRef.get() == null){
             initServletContext();
        }
        return servletContextRef.get().getContextPath();
    }

    @Override
    public ServletContext getContext(String uripath) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getContext(uripath);
    }

    @Override
    public int getMajorVersion() {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getMinorVersion();
    }

    @Override
    public int getEffectiveMajorVersion() {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getEffectiveMajorVersion();
    }

    @Override
    public int getEffectiveMinorVersion() {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getEffectiveMinorVersion();
    }

    @Override
    public String getMimeType(String file) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getMimeType(file);
    }

    @Override
    public Set<String> getResourcePaths(String path) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getResourcePaths(path);
    }

    @Override
    public URL getResource(String path) throws MalformedURLException {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getResource(path);
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getResourceAsStream(path);
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getRequestDispatcher(path);
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getNamedDispatcher(name);
    }

    @Override
    public Servlet getServlet(String name) throws ServletException {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getServlet(name);
    }

    @Override
    public Enumeration<Servlet> getServlets() {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getServlets();
    }

    @Override
    public Enumeration<String> getServletNames() {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getServletNames();
    }

    @Override
    public void log(String msg) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        servletContextRef.get().log(msg);
    }

    @Override
    public void log(Exception exception, String msg) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        servletContextRef.get().log(exception, msg);
    }

    @Override
    public void log(String message, Throwable throwable) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        servletContextRef.get().log(message, throwable);
    }

    @Override
    public String getRealPath(String path) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getRealPath(path);
    }

    @Override
    public String getServerInfo() {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getServerInfo();
    }

    @Override
    public String getInitParameter(String name) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getInitParameter(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getInitParameterNames();
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().setInitParameter(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getAttributeNames();
    }

    @Override
    public void setAttribute(String name, Object object) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        servletContextRef.get().setAttribute(name, object);
    }

    @Override
    public void removeAttribute(String name) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        servletContextRef.get().removeAttribute(name);
    }

    @Override
    public String getServletContextName() {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getServletContextName();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, String className) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().addServlet(servletName, className);
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().addServlet(servletName, servlet);
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().addServlet(servletName, servletClass);
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().createServlet(clazz);
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getServletRegistration(servletName);
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getServletRegistrations();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().addFilter(filterName, className);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().addFilter(filterName, filter);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().addFilter(filterName, filterClass);
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().createFilter(clazz);
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getFilterRegistration(filterName);
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getFilterRegistrations();
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getSessionCookieConfig();
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        servletContextRef.get().setSessionTrackingModes(sessionTrackingModes);
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getDefaultSessionTrackingModes();
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getEffectiveSessionTrackingModes();
    }

    @Override
    public void addListener(String className) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        servletContextRef.get().addListener(className);
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        servletContextRef.get().addListener(t);
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        servletContextRef.get().addListener(listenerClass);
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().createListener(clazz);
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getJspConfigDescriptor();
    }

    @Override
    public ClassLoader getClassLoader() {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getClassLoader();
    }

    @Override
    public void declareRoles(String... roleNames) {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        servletContextRef.get().declareRoles(roleNames);
    }

    @Override
    public String getVirtualServerName() {
        if(servletContextRef.get() == null){
            initServletContext();
        }
        return servletContextRef.get().getVirtualServerName();
    }

    @Override
    public ServletRegistration.Dynamic addJspFile(String servletName, String jspFile) {
        if (servletContextRef.get() == null) {
            initServletContext();
        }
        return servletContextRef.get().addJspFile(servletName, jspFile);
    }

    @Override
    public int getSessionTimeout() {
        if (servletContextRef.get() == null) {
            initServletContext();
        }
        return servletContextRef.get().getSessionTimeout();
    }

    @Override
    public void setSessionTimeout(int sessionTimeout) {
        if (servletContextRef.get() == null) {
            initServletContext();
        }
        servletContextRef.get().setSessionTimeout(sessionTimeout);
    }

    @Override
    public String getRequestCharacterEncoding() {
        if (servletContextRef.get() == null) {
            initServletContext();
        }
        return servletContextRef.get().getRequestCharacterEncoding();
    }

    @Override
    public void setRequestCharacterEncoding(String encoding) {
        if (servletContextRef.get() == null) {
            initServletContext();
        }
        servletContextRef.get().setRequestCharacterEncoding(encoding);
    }

    @Override
    public String getResponseCharacterEncoding() {
        if (servletContextRef.get() == null) {
            initServletContext();
        }
        return servletContextRef.get().getResponseCharacterEncoding();
    }

    @Override
    public void setResponseCharacterEncoding(String encoding) {
        if (servletContextRef.get() == null) {
            initServletContext();
        }
        servletContextRef.get().setResponseCharacterEncoding(encoding);
    }

}
