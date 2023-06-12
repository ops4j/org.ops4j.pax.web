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
package org.ops4j.pax.web.service.spi.model.elements;

import jakarta.websocket.Decoder;
import jakarta.websocket.Encoder;
import jakarta.websocket.server.ServerEndpoint;

import org.ops4j.pax.web.service.spi.model.events.WebSocketEventData;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWebContainerView;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Set of parameters describing everything that's required to register a WebSocket endpoint according to JSR-356.
 */
public class WebSocketModel extends ElementModel<Object, WebSocketEventData> {

    public static final Logger LOG = LoggerFactory.getLogger(WebSocketModel.class);

    private final Object webSocketEndpoint;
    private final Class<?> webSocketEndpointClass;

    private Class<?> webSocketEndpointClassResolved;

    // see jakarta.websocket.server.ServerEndpoint.value
    private String mappedPath;

    @SuppressWarnings("unchecked")
    private Class<? extends Decoder>[] decoderClasses = new Class[0];
    @SuppressWarnings("unchecked")
    private Class<? extends Encoder>[] encoderClasses = new Class[0];

    private String[] subprotocols = new String[0];

    public WebSocketModel() {
        this.webSocketEndpoint = null;
        this.webSocketEndpointClass = null;
    }

    public WebSocketModel(Object webSocketEndpoint, Class<?> webSocketEndpointClass) {
        this.webSocketEndpoint = webSocketEndpoint;
        this.webSocketEndpointClass = webSocketEndpointClass;
    }

    public Object getWebSocketEndpoint() {
        return webSocketEndpoint;
    }

    public Class<?> getWebSocketEndpointClass() {
        return webSocketEndpointClass;
    }

    public Class<?> getWebSocketEndpointClassResolved() {
        return webSocketEndpointClassResolved;
    }

    public String getMappedPath() {
        return mappedPath;
    }

    public Class<? extends Decoder>[] getDecoderClasses() {
        return decoderClasses;
    }

    public Class<? extends Encoder>[] getEncoderClasses() {
        return encoderClasses;
    }

    public String[] getSubprotocols() {
        return subprotocols;
    }

    @Override
    public void register(WhiteboardWebContainerView view) {
        view.registerWebSocket(this);
    }

    @Override
    public void unregister(WhiteboardWebContainerView view) {
        view.unregisterWebSocket(this);
    }

    @Override
    public WebSocketEventData asEventData() {
        WebSocketEventData data = new WebSocketEventData(webSocketEndpoint, webSocketEndpointClassResolved);
        setCommonEventProperties(data);
        return data;
    }

    @Override
    public Boolean performValidation() {
        int sources = 0;
        sources += (webSocketEndpoint != null ? 1 : 0);
        sources += (webSocketEndpointClass != null ? 1 : 0);
        sources += (getElementReference() != null ? 1 : 0);
        sources += (getElementSupplier() != null ? 1 : 0);
        if (sources == 0) {
            throw new IllegalArgumentException("WebSocket Model must specify one of: web socket instance, web socket class"
                    + " or service reference");
        }
        if (sources != 1) {
            throw new IllegalArgumentException("WebSocket Model should specify a web socket uniquely as instance, class"
                    + " or service reference");
        }

        Class<?> c = null;
        if (webSocketEndpoint != null) {
            c = webSocketEndpoint.getClass();
        } else if (webSocketEndpointClass != null) {
            c = webSocketEndpointClass;
        } else if (getElementSupplier() != null) {
            Object ws = getElementSupplier().get();
            if (ws != null) {
                c = ws.getClass();
            } else {
                throw new IllegalArgumentException("Can't determine the Web Socket endpoint path. Element supplier returned null.");
            }
        } else if (getElementReference() != null) {
            Object ws = null;
            BundleContext context = getRegisteringBundle().getBundleContext();
            try {
                ws = context == null ? null : context.getService(getElementReference());
                if (ws != null) {
                    c = ws.getClass();
                } else {
                    throw new IllegalArgumentException("Can't determine the Web Socket endpoint path. Service reference returned null.");
                }
            } finally {
                if (ws != null) {
                    context.ungetService(getElementReference());
                }
            }
        }

        if (c == null) {
            throw new IllegalArgumentException("Can't determine the Web Socket endpoint path.");
        }

        if (c.isAnnotationPresent(ServerEndpoint.class)) {
            ServerEndpoint endpoint = c.getAnnotation(ServerEndpoint.class);
            decoderClasses = endpoint.decoders();
            encoderClasses = endpoint.encoders();
            subprotocols = endpoint.subprotocols();

            if (endpoint.value() != null) {
                mappedPath = endpoint.value().trim();
                // set the class - we will need it during actual registration
                webSocketEndpointClassResolved = c;
                return Boolean.TRUE;
            }
        }

        LOG.warn("Can't determine the Web Socket endpoint path - is @ServerEndpoint annotation present?");

        return Boolean.FALSE;
    }

    @Override
    public String toString() {
        return "WebSocketModel{id=" + getId()
                + (webSocketEndpoint == null ? "" : ",endpoint=" + webSocketEndpoint)
                + (webSocketEndpointClassResolved == null ? "" : ",endpoint class=" + webSocketEndpointClassResolved)
                + ",contexts=" + getContextModelsInfo()
                + "}";
    }

}
