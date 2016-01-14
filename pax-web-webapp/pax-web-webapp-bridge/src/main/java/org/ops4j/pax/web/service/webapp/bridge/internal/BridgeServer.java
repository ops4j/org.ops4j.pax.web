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
    private Map<String,ContextModel> contextModels = new TreeMap<String,ContextModel>();

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

    public Map<String, ContextModel> getContextModels() {
        return contextModels;
    }

    public ContextModel getContextModel(String contextModelName) {
        return contextModels.get(contextModelName);
    }

    public ContextModel removeContextModel(HttpContext httpContext) {
        for (ContextModel contextModel : contextModels.values()) {
            if (contextModel.getHttpContext().equals(httpContext)) {
                return contextModel;
            }
        }
        // wasn't found, return null;
        return null;
    }

    public ContextModel getOrCreateContextModel(ContextModel contextModel) {
        contextModels.put(contextModel.getContextName(), contextModel);
        return contextModel;
    }
}
