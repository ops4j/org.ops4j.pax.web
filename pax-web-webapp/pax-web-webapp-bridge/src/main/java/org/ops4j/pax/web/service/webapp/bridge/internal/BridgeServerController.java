package org.ops4j.pax.web.service.webapp.bridge.internal;

import org.ops4j.pax.web.service.spi.*;
import org.ops4j.pax.web.service.spi.model.*;
import org.osgi.service.http.HttpContext;

import javax.servlet.Servlet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by loom on 13.01.16.
 */
public class BridgeServerController implements ServerController {

    private static final Logger logger = LoggerFactory.getLogger(BridgeServerController.class);

    private boolean started = false;
    private Configuration configuration = null;
    private final Set<ServerListener> serverListeners = newThreadSafeSet();
    private BridgeServer bridgeServer;

    private Set<ServerListener> newThreadSafeSet() {
        return new CopyOnWriteArraySet<ServerListener>();
    }

    public BridgeServerController(BridgeServer bridgeServer) {
        this.bridgeServer = bridgeServer;
    }

    @Override
    public void start() {
        started = true;
        logger.debug("Bridge server started");
        fireStateChange(ServerEvent.STARTED);
    }

    @Override
    public void stop() {
        started = false;
        logger.debug("Bridge server stopped");
        fireStateChange(ServerEvent.STOPPED);
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public boolean isConfigured() {
        return configuration != null;
    }

    @Override
    public void configure(Configuration configuration) {
        this.configuration = configuration;
        fireStateChange(ServerEvent.CONFIGURED);
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void addListener(ServerListener listener) {
        serverListeners.add(listener);
    }

    @Override
    public void removeListener(ServerListener listener) {
        serverListeners.remove(listener);
    }

    @Override
    public void addServlet(ServletModel model) {
    }

    @Override
    public void removeServlet(ServletModel model) {

    }

    @Override
    public void addEventListener(EventListenerModel eventListenerModel) {

    }

    @Override
    public void removeEventListener(EventListenerModel eventListenerModel) {

    }

    @Override
    public void addFilter(FilterModel filterModel) {

    }

    @Override
    public void removeFilter(FilterModel filterModel) {

    }

    @Override
    public void addErrorPage(ErrorPageModel model) {

    }

    @Override
    public void removeErrorPage(ErrorPageModel model) {

    }

    @Override
    public void addWelcomFiles(WelcomeFileModel model) {

    }

    @Override
    public void removeWelcomeFiles(WelcomeFileModel model) {

    }

    @Override
    public LifeCycle getContext(ContextModel model) {
        final ContextModel contextModel = model;
        return new LifeCycle() {
            @Override
            public void start() throws Exception {
                logger.debug("Starting context {}", contextModel.getContextName());
            }

            @Override
            public void stop() throws Exception {
                logger.debug("Stopping context {}", contextModel.getContextName());
            }
        };
    }

    @Override
    public Integer getHttpPort() {
        return null;
    }

    @Override
    public Integer getHttpSecurePort() {
        return null;
    }

    @Override
    public Servlet createResourceServlet(ContextModel contextModel, String alias, String name) {
        return new BridgeResourceServlet(contextModel.getHttpContext(), contextModel.getContextName(), alias, name);
    }

    @Override
    public void addSecurityConstraintMapping(SecurityConstraintMappingModel secMapModel) {

    }

    @Override
    public void addContainerInitializerModel(ContainerInitializerModel model) {

    }

    @Override
    public void removeContext(HttpContext httpContext) {

    }

    private void fireStateChange(ServerEvent event) {
        for (ServerListener serverListener : serverListeners) {
            serverListener.stateChanged(event);
        }
    }

}
