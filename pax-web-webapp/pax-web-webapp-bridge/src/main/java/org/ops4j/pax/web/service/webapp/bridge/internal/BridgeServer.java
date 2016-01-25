package org.ops4j.pax.web.service.webapp.bridge.internal;

import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

import javax.servlet.ServletContext;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by loom on 14.01.16.
 */
public class BridgeServer {

    private ServerModel serverModel = null;
    private BridgeServerModel bridgeServerModel = new BridgeServerModel();
    private Map<String,BridgeServletContext> bridgeServletContexts = new TreeMap<String,BridgeServletContext>();
    private Bundle bridgeBundle;
    private ServletContext proxyServletContext;

    public BridgeServer() {
    }

    public Bundle getBridgeBundle() {
        return bridgeBundle;
    }

    public void setBridgeBundle(Bundle bridgeBundle) {
        this.bridgeBundle = bridgeBundle;
    }

    public ServerModel getServerModel() {
        return serverModel;
    }

    public void setServerModel(ServerModel serverModel) {
        this.serverModel = serverModel;
    }

    public BridgeServerModel getBridgeServerModel() {
        return bridgeServerModel;
    }

    public void setBridgeServerModel(BridgeServerModel bridgeServerModel) {
        this.bridgeServerModel = bridgeServerModel;
    }

    public ServletContext getProxyServletContext() {
        return proxyServletContext;
    }

    public void setProxyServletContext(ServletContext proxyServletContext) {
        this.proxyServletContext = proxyServletContext;
    }

    public Map<String, BridgeServletContext> getBridgeServletContexts() {
        return bridgeServletContexts;
    }

    public BridgeServletContext getContextModel(String contextModelName) {
        return bridgeServletContexts.get(contextModelName);
    }

    public ContextModel removeContextModel(HttpContext httpContext) {
        for (BridgeServletContext contextModel : bridgeServletContexts.values()) {
            if (contextModel.getContextModel().getHttpContext().equals(httpContext)) {
                bridgeServletContexts.remove(contextModel.getContextModel().getContextName());
                return contextModel.getContextModel();
            }
        }
        // wasn't found, return null;
        return null;
    }

    public BridgeServletContext getOrCreateContextModel(ContextModel contextModel) {
        BridgeServletContext bridgeServletContext = bridgeServletContexts.get(contextModel.getContextName());
        if (bridgeServletContext == null) {
            bridgeServletContext = new BridgeServletContext(contextModel, this);
        }
        bridgeServletContexts.put(contextModel.getContextName(), bridgeServletContext);
        return bridgeServletContext;
    }
}
