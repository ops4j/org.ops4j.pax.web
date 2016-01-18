package org.ops4j.pax.web.service.webapp.bridge.internal;

import org.apache.xbean.finder.BundleAnnotationFinder;
import org.apache.xbean.finder.BundleAssignableClassFinder;
import org.ops4j.pax.swissbox.core.ContextClassLoaderUtils;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.LifeCycle;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.FilterModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.ops4j.pax.web.utils.ClassPathUtil;
import org.osgi.framework.*;
import org.osgi.service.http.HttpContext;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.Filter;
import javax.servlet.annotation.HandlesTypes;
import javax.servlet.descriptor.JspConfigDescriptor;
import java.io.*;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by loom on 14.01.16.
 */
public class BridgeServletContext implements ServletContext, LifeCycle {

    List<BridgeServletModel> bridgeServlets = new ArrayList<>();
    List<BridgeFilterModel> bridgeFilters = new ArrayList<>();
    List<BridgeServletModel> startedServlets = new ArrayList<>();

    private static final Logger LOG = LoggerFactory.getLogger(BridgeServletContext.class);

    private ContextModel contextModel;
    private ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();
    private ConcurrentLinkedDeque<? extends EventListener> eventListeners = new ConcurrentLinkedDeque<>();

    private boolean started = false;
    private ServiceTracker<PackageAdmin, PackageAdmin> packageAdminTracker;

    private BridgeServer bridgeServer;

    public BridgeServletContext(ContextModel contextModel, BridgeServer bridgeServer) {
        this.contextModel = contextModel;
        this.bridgeServer = bridgeServer;
        if (bridgeServer.getBridgeBundle() != null) {
            org.osgi.framework.Filter filterPackage = null;
            try {
                filterPackage = bridgeServer.getBridgeBundle()
                        .getBundleContext()
                        .createFilter(
                                "(objectClass=org.osgi.service.packageadmin.PackageAdmin)");
            } catch (InvalidSyntaxException e) {
                LOG.error(
                        "InvalidSyntaxException while waiting for PackageAdmin Service",
                        e);
            }
            packageAdminTracker = new ServiceTracker<PackageAdmin, PackageAdmin>(
                    bridgeServer.getBridgeBundle().getBundleContext(), filterPackage, null);
            packageAdminTracker.open();
        }
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

    public BridgeFilterModel findFilter(FilterModel filterModel) {
        for (BridgeFilterModel bridgeFilterModel : bridgeFilters) {
            if (bridgeFilterModel.getFilterModel().equals(filterModel)) {
                return bridgeFilterModel;
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
        return 3;
    }

    @Override
    public int getMinorVersion() {
        return 1;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return 3;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 1;
    }

    @Override
    public String getMimeType(String file) {
        if (file == null) {
            return null;
        }
        int dotIndex = file.lastIndexOf(".");
        if (dotIndex < 0) {
            return null;
        }
        String extension = file.substring(dotIndex + 1);
        if (extension.length() < 1) {
            return null;
        }
        return null;
    }

    @Override
    public Set<String> getResourcePaths(final String path) {
        final HttpContext httpContext = contextModel.getHttpContext();
        if (httpContext instanceof WebContainerContext) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("getting resource paths for : [" + path + "]");
            }
            try {
                final Set<String> paths = AccessController.doPrivileged(
                        new PrivilegedExceptionAction<Set<String>>() {
                            @Override
                            public Set<String> run() throws Exception {
                                return ((WebContainerContext) httpContext)
                                        .getResourcePaths(path);
                            }
                        }, contextModel.getAccessControllerContext());
                if (paths == null) {
                    return null;
                }
                // Servlet specs mandates that the paths must start with an
                // slash "/"
                final Set<String> slashedPaths = new HashSet<String>();
                for (String foundPath : paths) {
                    if (foundPath != null) {
                        if (foundPath.trim().startsWith("/")) {
                            slashedPaths.add(foundPath.trim());
                        } else {
                            slashedPaths.add("/" + foundPath.trim());
                        }
                    }
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("found resource paths: " + paths);
                }
                return slashedPaths;
            } catch (PrivilegedActionException e) {
                LOG.warn("Unauthorized access: " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    @Override
    public URL getResource(String path) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("getting resource: [" + path + "]");
        }
        URL resource = null;

        // IMPROVEMENT start PAXWEB-314
        try {
            resource = new URL(path);
            LOG.debug("resource: [" + path
                    + "] is already a URL, returning");
            return resource;
        } catch (MalformedURLException e) {
            // do nothing, simply log
            LOG.debug("not a URL or invalid URL: [" + path
                    + "], treating as a file path");
        }
        // IMPROVEMENT end PAXWEB-314

        // FIX start PAXWEB-233
        final String p;
        if (path != null && path.endsWith("/") && path.length() > 1) {
            p = path.substring(0, path.length() - 1);
        } else {
            p = path;
        }
        // FIX end

        try {
            resource = AccessController.doPrivileged(
                    new PrivilegedExceptionAction<URL>() {
                        @Override
                        public URL run() throws Exception {
                            return contextModel.getHttpContext().getResource(p);
                        }
                    }, contextModel.getAccessControllerContext());
            if (LOG.isDebugEnabled()) {
                LOG.debug("found resource: " + resource);
            }
        } catch (PrivilegedActionException e) {
            LOG.warn("Unauthorized access: " + e.getMessage());
        }
        return resource;
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        final URL url = getResource(path);
        if (url != null) {
            try {
                return AccessController.doPrivileged(
                        new PrivilegedExceptionAction<InputStream>() {
                            @Override
                            public InputStream run() throws Exception {
                                try {
                                    return url.openStream();
                                } catch (IOException e) {
                                    LOG.warn("URL canot be accessed: "
                                            + e.getMessage());
                                }
                                return null;
                            }

                        }, contextModel.getAccessControllerContext());
            } catch (PrivilegedActionException e) {
                LOG.warn("Unauthorized access: " + e.getMessage());
            }

        }
        return null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return new BridgePathRequestDispatcher(path, bridgeServer);
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        String path = null;
        // @todo implement this
        return new BridgeNamedRequestDispatcher(name, bridgeServer);
    }

    @Override
    public Servlet getServlet(String name) throws ServletException {
        if (name == null) {
            return null;
        }
        for (BridgeServletModel bridgeServletModel : bridgeServlets) {
            if (name.equals(bridgeServletModel.getServletModel().getName())) {
                return bridgeServletModel.getServletModel().getServlet();
            }
        }
        return null;
    }

    @Override
    public Enumeration<Servlet> getServlets() {
        List<Servlet> servlets = new ArrayList<>();
        for (BridgeServletModel bridgeServletModel : bridgeServlets) {
            servlets.add(bridgeServletModel.getServletModel().getServlet());
        }
        return new Vector<Servlet>(servlets).elements();
    }

    @Override
    public Enumeration<String> getServletNames() {
        List<String> servletNames = new ArrayList<>();
        for (BridgeServletModel bridgeServletModel : bridgeServlets) {
            servletNames.add(bridgeServletModel.getServletModel().getName());
        }
        return new Vector<String>(servletNames).elements();
    }

    @Override
    public void log(String msg) {
        LOG.info(msg);
    }

    @Override
    public void log(Exception exception, String msg) {
        LOG.error(msg, exception);
    }

    @Override
    public void log(String message, Throwable throwable) {
        LOG.error(message, throwable);
    }

    @Override
    public String getRealPath(String path) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("getting real path: [{}]", path);
        }

        URL resource = getResource(path);
        if (resource != null) {
            String protocol = resource.getProtocol();
            if (protocol.equals("file")) {
                String fileName = resource.getFile();
                if (fileName != null) {
                    File file = new File(fileName);
                    if (file.exists()) {
                        String realPath = file.getAbsolutePath();
                        LOG.debug("found real path: [{}]", realPath);
                        return realPath;
                    }
                }
            }
        }
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
        Map<Integer, List<BridgeServletModel>> sortedServletsToStart = new TreeMap<>();
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
        for (Map.Entry<Integer, List<BridgeServletModel>> servletsForInteger : sortedServletsToStart.entrySet()) {
            List<BridgeServletModel> servlets = servletsForInteger.getValue();
            for (BridgeServletModel servlet : servlets) {
                servlet.init();
                startedServlets.add(servlet);
            }
        }

        // scan for ServletContainerInitializers
        Bundle bundle = contextModel.getBundle();
        Set<Bundle> bundlesInClassSpace = ClassPathUtil.getBundlesInClassSpace(
                bundle, new HashSet<Bundle>());

        if (bridgeServer.getBridgeBundle() != null) {
            ClassPathUtil.getBundlesInClassSpace(bridgeServer.getBridgeBundle(),
                    bundlesInClassSpace);
        }

        for (URL u : ClassPathUtil.findResources(bundlesInClassSpace,
                "/META-INF/services",
                "javax.servlet.ServletContainerInitializer", true)) {
            try {
                InputStream is = u.openStream();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(is));
                // only the first line is read, it contains the name of the
                // class.
                String className = reader.readLine();
                LOG.info("will add {} to ServletContainerInitializers",
                        className);

                if (className.endsWith("JasperInitializer")) {
                    LOG.info(
                            "Skipt {}, because specialized handler will be present",
                            className);
                    continue;
                }

                Class<?> initializerClass;

                try {
                    initializerClass = bundle.loadClass(className);
                } catch (ClassNotFoundException ignore) {
                    initializerClass = bridgeServer.getBridgeBundle().loadClass(className);
                }

                // add those to the model contained ones
                Map<ServletContainerInitializer, Set<Class<?>>> containerInitializers = contextModel
                        .getContainerInitializers();

                ServletContainerInitializer initializer = (ServletContainerInitializer) initializerClass
                        .newInstance();

                if (containerInitializers == null) {
                    containerInitializers = new HashMap<ServletContainerInitializer, Set<Class<?>>>();
                    contextModel.setContainerInitializers(containerInitializers);
                }

                Set<Class<?>> setOfClasses = new HashSet<Class<?>>();
                // scan for @HandlesTypes
                HandlesTypes handlesTypes = initializerClass
                        .getAnnotation(HandlesTypes.class);
                if (handlesTypes != null) {
                    Class<?>[] classes = handlesTypes.value();

                    for (Class<?> klass : classes) {
                        boolean isAnnotation = klass.isAnnotation();
                        boolean isInterface = klass.isInterface();

                        if (isAnnotation) {
                            try {
                                BundleAnnotationFinder baf = new BundleAnnotationFinder(
                                        packageAdminTracker.getService(),
                                        bundle);
                                List<Class<?>> annotatedClasses = baf
                                        .findAnnotatedClasses((Class<? extends Annotation>) klass);
                                setOfClasses.addAll(annotatedClasses);
                            } catch (Exception e) {
                                LOG.warn(
                                        "Failed to find annotated classes for ServletContainerInitializer",
                                        e);
                            }
                        } else if (isInterface) {
                            BundleAssignableClassFinder basf = new BundleAssignableClassFinder(
                                    packageAdminTracker.getService(),
                                    new Class[]{klass}, bundle);
                            Set<String> interfaces = basf.find();
                            for (String interfaceName : interfaces) {
                                setOfClasses.add(bundle
                                        .loadClass(interfaceName));
                            }
                        } else {
                            // class
                            BundleAssignableClassFinder basf = new BundleAssignableClassFinder(
                                    packageAdminTracker.getService(),
                                    new Class[]{klass}, bundle);
                            Set<String> classNames = basf.find();
                            for (String klassName : classNames) {
                                setOfClasses.add(bundle
                                        .loadClass(klassName));
                            }
                        }
                    }
                }
                contextModel.addContainerInitializer(initializer, setOfClasses);
                LOG.info("added ServletContainerInitializer: {}", className);
            } catch (ClassNotFoundException | InstantiationException
                    | IllegalAccessException | IOException e) {
                LOG.warn("failed to parse and instantiate of javax.servlet.ServletContainerInitializer in classpath");
            }
        }

        if (isJspAvailable()) { // use JasperClassloader
            LOG.info("registering JasperInitializer");
            @SuppressWarnings("unchecked")
            Class<ServletContainerInitializer> loadClass = (Class<ServletContainerInitializer>) loadClass("org.ops4j.pax.web.jsp.JasperInitializer");
            contextModel.addContainerInitializer(loadClass.newInstance(),
                    Collections.<Class<?>>emptySet());
        }

        if (contextModel.getContainerInitializers() != null && contextModel.getContainerInitializers().size() > 0) {
            for (final Map.Entry<ServletContainerInitializer, Set<Class<?>>> containerInitializerEntry : contextModel.getContainerInitializers().entrySet()) {
                final ServletContext servletContext = this;
                try {
                    ContextClassLoaderUtils.doWithClassLoader(getClassLoader(),
                            new Callable<Void>() {
                                @Override
                                public Void call() throws IOException,
                                        ServletException {
                                    containerInitializerEntry.getKey().onStartup(containerInitializerEntry.getValue(), servletContext);
                                    return null;
                                }

                            });
                } catch (Exception e) {
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    }
                    LOG.error("Ignored exception during listener registration",
                            e);
                }
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

    public synchronized Class<?> loadClass(String className) throws ClassNotFoundException {
        return className == null ? null : (this.contextModel.getClassLoader() == null ? bridgeServer.getBridgeBundle().getClass().getClassLoader().loadClass(className) : this.contextModel.getClassLoader().loadClass(className));
    }

    private boolean isJspAvailable() {
        try {
            return (org.ops4j.pax.web.jsp.JspServletWrapper.class != null);
        } catch (NoClassDefFoundError ignore) {
            return false;
        }
    }

}
