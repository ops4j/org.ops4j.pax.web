package org.ops4j.pax.web.service.undertow.internal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.HttpServletRequestImpl;

/**
 * @author Guillaume Nodet
 */
public class ResourceServlet extends HttpServlet implements ResourceManager {

    private final Context context;
    private final HttpHandler handler;
    private final String alias;
    private final String name;

    public ResourceServlet(final Context context, String alias, String name) {
        this.context = context;
        this.alias = alias;
        if ("/".equals(name)) {
            this.name = "";
        } else {
            this.name = name;
        }
        this.handler = new ResourceHandler(this, new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                ServletRequestContext src = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
                src.getOriginalResponse().sendError(404);
            }
        });
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
        if (!(request instanceof HttpServletRequestImpl)) {
            throw new IllegalStateException("Request is not an instance of " + HttpServletRequestImpl.class.getName());
        }
        HttpServerExchange exchange = ((HttpServletRequestImpl) request).getExchange();
        try {
            handler.handleRequest(exchange);
        } catch (IOException | ServletException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    public Resource getResource(String path) throws IOException {
        String contextName = context.getContextModel().getContextName();
        if (contextName.isEmpty()) {
            contextName = "/";
        }
        String mapping;
        if (contextName.equals(alias)) {
            // special handling since resouceServlet has default name
            // attached to it
            if (!"default".equalsIgnoreCase(name)) {
                mapping = name + path;
            } else {
                mapping = path;
            }
        } else {
            mapping = path.replaceFirst(contextName, "/");
            if (!"default".equalsIgnoreCase(name)) {
                mapping = mapping.replaceFirst(alias,
                        Matcher.quoteReplacement(name));
            }
        }
        return context.getResource(mapping);
    }

    @Override
    public boolean isResourceChangeListenerSupported() {
        return false;
    }

    @Override
    public void registerResourceChangeListener(ResourceChangeListener listener) {

    }

    @Override
    public void removeResourceChangeListener(ResourceChangeListener listener) {

    }

    @Override
    public void close() throws IOException {

    }
}
