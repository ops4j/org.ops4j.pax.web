package org.ops4j.pax.web.service.webapp.bridge.internal;

import org.ops4j.pax.web.service.spi.*;
import org.ops4j.pax.web.service.spi.model.*;
import org.osgi.service.http.HttpContext;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.osgi.service.http.NamespaceException;
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
        BridgeServletContext bridgeServletContext = bridgeServer.getOrCreateContextModel(model.getContextModel());
        BridgeServletModel bridgeServletModel = new BridgeServletModel(model, bridgeServletContext);
        bridgeServletContext.bridgeServlets.add(bridgeServletModel);
        try {
            if (bridgeServletContext.isStarted() &&
                    !bridgeServletModel.isInitialized() &&
                    (bridgeServletModel.getServletModel().getLoadOnStartup() != null)) {
                bridgeServletModel.init();
            }
            bridgeServer.getBridgeServerModel().addServletModel(model);
        } catch (NamespaceException e) {
            logger.error("Error registering servlet " + model , e);
        } catch (ServletException e) {
            logger.error("Error registering servlet " + model , e);
        }
    }

    @Override
    public void removeServlet(ServletModel model) {
        BridgeServletContext bridgeServletContext = bridgeServer.getOrCreateContextModel(model.getContextModel());
        bridgeServer.getBridgeServerModel().removeServletModel(model);
        BridgeServletModel bridgeServletModel = bridgeServletContext.findServlet(model);
        if (bridgeServletModel.isInitialized()) {
            bridgeServletModel.destroy();
        }
        bridgeServletContext.bridgeServlets.remove(bridgeServletModel);
    }

    @Override
    public void addEventListener(EventListenerModel eventListenerModel) {
        BridgeServletContext bridgeServletContext = bridgeServer.getOrCreateContextModel(eventListenerModel.getContextModel());
        if (bridgeServletContext != null) {
            bridgeServletContext.addListener(eventListenerModel.getEventListener());
        }
        bridgeServer.getBridgeServerModel().addEventListener(eventListenerModel);
    }

    @Override
    public void removeEventListener(EventListenerModel eventListenerModel) {
        BridgeServletContext bridgeServletContext = bridgeServer.getOrCreateContextModel(eventListenerModel.getContextModel());
        if (bridgeServletContext != null) {
            bridgeServletContext.removeListener(eventListenerModel.getEventListener());
        }
        bridgeServer.getBridgeServerModel().removeEventListener(eventListenerModel);
    }

    @Override
    public void addFilter(FilterModel filterModel) {
        bridgeServer.getOrCreateContextModel(filterModel.getContextModel());
        bridgeServer.getBridgeServerModel().addFilterModel(filterModel);
    }

    @Override
    public void removeFilter(FilterModel filterModel) {
        bridgeServer.getOrCreateContextModel(filterModel.getContextModel());
        bridgeServer.getBridgeServerModel().removeFilterModel(filterModel);
    }

    @Override
    public void addErrorPage(ErrorPageModel model) {
        bridgeServer.getOrCreateContextModel(model.getContextModel());
    }

    @Override
    public void removeErrorPage(ErrorPageModel model) {
        bridgeServer.getOrCreateContextModel(model.getContextModel());
    }

    @Override
    public void addWelcomFiles(WelcomeFileModel model) {
        bridgeServer.getOrCreateContextModel(model.getContextModel());
    }

    @Override
    public void removeWelcomeFiles(WelcomeFileModel model) {
        bridgeServer.getOrCreateContextModel(model.getContextModel());
    }

    @Override
    public LifeCycle getContext(ContextModel model) {
        return bridgeServer.getOrCreateContextModel(model);
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
        bridgeServer.getOrCreateContextModel(contextModel);
        return new BridgeResourceServlet(contextModel.getHttpContext(), contextModel.getContextName(), alias, name);
    }

    @Override
    public void addSecurityConstraintMapping(SecurityConstraintMappingModel secMapModel) {
        bridgeServer.getOrCreateContextModel(secMapModel.getContextModel());
    }

    @Override
    public void addContainerInitializerModel(ContainerInitializerModel model) {
        BridgeServletContext bridgeServletContext = bridgeServer.getOrCreateContextModel(model.getContextModel());
        if (bridgeServletContext == null) {
            return;
        }
        bridgeServletContext.addServletContainerInitializer(model);
    }

    @Override
    public void removeContext(HttpContext httpContext) {
        bridgeServer.removeContextModel(httpContext);

    }

    private void fireStateChange(ServerEvent event) {
        for (ServerListener serverListener : serverListeners) {
            serverListener.stateChanged(event);
        }
    }

}
