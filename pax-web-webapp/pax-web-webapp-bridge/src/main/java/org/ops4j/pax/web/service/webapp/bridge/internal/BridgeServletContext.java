package org.ops4j.pax.web.service.webapp.bridge.internal;

import org.ops4j.pax.web.service.spi.LifeCycle;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.descriptor.JspConfigDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by loom on 14.01.16.
 */
public class BridgeServletContext implements ServletContext, LifeCycle {

    List<BridgeServletModel> bridgeServlets = new ArrayList<>();
    List<BridgeServletModel> startedServlets = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(BridgeServletContext.class);

    private ContextModel contextModel;
    private ConcurrentMap<String,Object> attributes = new ConcurrentHashMap<String,Object>();
    private ConcurrentLinkedDeque<? extends EventListener> eventListeners = new ConcurrentLinkedDeque<>();

    private boolean started = false;

    public BridgeServletContext(ContextModel contextModel) {
        this.contextModel = contextModel;
    }

    public boolean isStarted() {
        return started;
    }

    public BridgeServletModel findServlet(ServletModel servletModel) {
        for (BridgeServletModel bridgeServletModel : bridgeServlets) {
            if (bridgeServletModel.getServletModel().equals(servletModel)) {
                return bridgeServletModel;
            }
        }
        return null;
    }

    public ContextModel getContextModel() {
        return contextModel;
    }

    @Override
    public String getContextPath() {
        return contextModel.getContextName();
    }

    @Override
    public ServletContext getContext(String uripath) {
        return null;
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return 0;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 0;
    }

    @Override
    public String getMimeType(String file) {
        return null;
    }

    @Override
    public Set<String> getResourcePaths(String path) {
        return null;
    }

    @Override
    public URL getResource(String path) throws MalformedURLException {
        return contextModel.getBundle().getResource(path);
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        try {
            URL resourceURL = getResource(path);
            if (resourceURL == null) {
                return null;
            }
            return resourceURL.openStream();
        } catch (IOException e) {
            logger.error("Couldn't open stream for resource at path " + path, e);
        }
        return null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        return null;
    }

    @Override
    public Servlet getServlet(String name) throws ServletException {
        return null;
    }

    @Override
    public Enumeration<Servlet> getServlets() {
        return null;
    }

    @Override
    public Enumeration<String> getServletNames() {
        return null;
    }

    @Override
    public void log(String msg) {
        logger.info(msg);
    }

    @Override
    public void log(Exception exception, String msg) {
        logger.error(msg, exception);
    }

    @Override
    public void log(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    @Override
    public String getRealPath(String path) {
        return null;
    }

    @Override
    public String getServerInfo() {
        return null;
    }

    @Override
    public String getInitParameter(String name) {
        return contextModel.getContextParams().get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return new Vector<String>(contextModel.getContextParams().keySet()).elements();
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        contextModel.getContextParams().put(name, value);
        return true;
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return new Vector<String>(attributes.keySet()).elements();
    }

    @Override
    public void setAttribute(String name, Object object) {
        attributes.put(name, object);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public String getServletContextName() {
        return contextModel.getContextName();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, String className) {
        return null;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        return null;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        return null;
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        return null;
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        return null;
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        return null;
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        return null;
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        return null;
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return null;
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return null;
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {

    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return null;
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return null;
    }

    @Override
    public void addListener(String className) {

    }

    @Override
    public <T extends EventListener> void addListener(T t) {
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        return null;
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return contextModel.getClassLoader();
    }

    @Override
    public void declareRoles(String... roleNames) {
    }

    @Override
    public String getVirtualServerName() {
        return null;
    }

    @Override
    public void start() throws Exception {
        if (started) {
            return;
        }
        Map<Integer,List<BridgeServletModel>> sortedServletsToStart = new TreeMap<>();
        for (BridgeServletModel bridgeServletModel : bridgeServlets) {
            if (bridgeServletModel.isInitialized()) {
                continue;
            }
            Integer loadOnStartup = bridgeServletModel.getServletModel().getLoadOnStartup();
            if (loadOnStartup != null) {
                List<BridgeServletModel> servlets = sortedServletsToStart.get(loadOnStartup);
                if (servlets == null) {
                    servlets = new ArrayList<>();
                }
                servlets.add(bridgeServletModel);
                sortedServletsToStart.put(loadOnStartup, servlets);
            }
        }
        for (Map.Entry<Integer,List<BridgeServletModel>> servletsForInteger : sortedServletsToStart.entrySet()) {
            List<BridgeServletModel> servlets = servletsForInteger.getValue();
            for (BridgeServletModel servlet : servlets) {
                servlet.init();
                startedServlets.add(servlet);
            }
        }
        if (contextModel.getContainerInitializers() != null && contextModel.getContainerInitializers().size() > 0) {
            for (Map.Entry<ServletContainerInitializer, Set<Class<?>>> containerInitializerEntry : contextModel.getContainerInitializers().entrySet()) {
                containerInitializerEntry.getKey().onStartup(containerInitializerEntry.getValue(), this);
            }
        }
        started = true;
    }

    @Override
    public void stop() throws Exception {
        if (!started) {
            return;
        }
        ListIterator<BridgeServletModel> startedServletsReverseIterator = startedServlets.listIterator();
        while (startedServletsReverseIterator.hasPrevious()) {
            BridgeServletModel bridgeServletModel = startedServletsReverseIterator.previous();
            bridgeServletModel.destroy();
        }
        started = false;
    }
}
