package org.ops4j.pax.web.extender.whiteboard.internal.tracker;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerEndpoint;

import org.ops4j.pax.web.extender.whiteboard.WebSocketMapping;
import org.ops4j.pax.web.extender.whiteboard.internal.ExtenderContext;
import org.ops4j.pax.web.extender.whiteboard.internal.element.WebSocketElement;
import org.ops4j.pax.web.extender.whiteboard.internal.util.ServicePropertiesUtils;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultWebSocketMapping;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketTracker extends AbstractTracker<Object, WebSocketElement> {

    private BundleContext bundleContext;

    WebSocketTracker(ExtenderContext extenderContext, BundleContext bundleContext) {
        super(extenderContext, bundleContext);
        this.bundleContext = bundleContext;
    }

    public static ServiceTracker<Object, WebSocketElement> createTracker(final ExtenderContext extenderContext,
            final BundleContext bundleContext) {
        return new WebSocketTracker(extenderContext, bundleContext).create(Object.class);
    }

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketTracker.class);

    @Override
    WebSocketElement createWebElement(ServiceReference<Object> serviceReference, Object published) {
        
        if (Endpoint.class.isAssignableFrom(published.getClass())) {
            LOG.warn("WebSockets created as instances of Endpoint isn't supported, because it requires also to register ServerApplicationConfig");
            return null;
        }

        ServerEndpoint serverEndpoint = published.getClass().getAnnotation(ServerEndpoint.class);
        if (serverEndpoint == null) {
            return null;
        }

        LOG.info("found websocket endpoint!!");
        
        WebSocketMapping mapping = new DefaultWebSocketMapping();
        mapping.setHttpContextId(ServicePropertiesUtils.extractHttpContextId(serviceReference));
        mapping.setWebSocket(published);
        return new WebSocketElement(mapping);
    }

}
