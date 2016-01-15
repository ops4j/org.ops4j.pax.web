package org.ops4j.pax.web.service.webapp.bridge.internal;

import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.osgi.service.http.HttpContext;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by loom on 14.01.16.
 */
public class BridgeServer {

    private ServerModel serverModel = null;
    private BridgeServerModel bridgeServerModel = new BridgeServerModel();
    private Map<String,BridgeServletContext> contextModels = new TreeMap<String,BridgeServletContext>();

    public BridgeServer() {
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

    public Map<String, BridgeServletContext> getContextModels() {
        return contextModels;
    }

    public BridgeServletContext getContextModel(String contextModelName) {
        return contextModels.get(contextModelName);
    }

    public ContextModel removeContextModel(HttpContext httpContext) {
        for (BridgeServletContext contextModel : contextModels.values()) {
            if (contextModel.getContextModel().getHttpContext().equals(httpContext)) {
                contextModels.remove(contextModel.getContextModel().getContextName());
                return contextModel.getContextModel();
            }
        }
        // wasn't found, return null;
        return null;
    }

    public BridgeServletContext getOrCreateContextModel(ContextModel contextModel) {
        BridgeServletContext bridgeServletContext = contextModels.get(contextModel.getContextName());
        if (bridgeServletContext == null) {
            bridgeServletContext = new BridgeServletContext(contextModel);
        }
        contextModels.put(contextModel.getContextName(), bridgeServletContext);
        return bridgeServletContext;
    }
}
