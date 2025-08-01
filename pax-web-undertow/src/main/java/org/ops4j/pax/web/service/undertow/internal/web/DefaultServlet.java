/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.ops4j.pax.web.service.undertow.internal.web;

import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.DefaultResourceSupplier;
import io.undertow.server.handlers.resource.DirectoryUtils;
import io.undertow.server.handlers.resource.PreCompressedResourceSupplier;
import io.undertow.server.handlers.resource.RangeAwareResource;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceSupplier;
import io.undertow.servlet.UndertowServletLogger;
import io.undertow.servlet.api.DefaultServletConfig;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.ServletContextImpl;
import io.undertow.util.ByteRange;
import io.undertow.util.CanonicalPathUtils;
import io.undertow.util.DateUtils;
import io.undertow.util.ETag;
import io.undertow.util.ETagUtils;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// CHECKSTYLE:OFF
/**
 * Default servlet responsible for serving up resources. This is both a handler and a servlet. If no filters
 * match the current path then the resources will be served up asynchronously using the
 * {@link io.undertow.server.HttpHandler#handleRequest(io.undertow.server.HttpServerExchange)} method,
 * otherwise the request is handled as a normal servlet request.
 * <p>
 * By default we only allow a restricted set of extensions.
 * </p>
 * todo: this thing needs a lot more work. In particular:
 * - caching for blocking requests
 * - correct mime type
 * - range/last-modified and other headers to be handled properly
 * - head requests
 * - and probably heaps of other things
 *
 * <p>Pax Web 8: I really tried simply extending this servlet, but there are too many private methods/fields.</p>
 *
 * @author Stuart Douglas
 */
public class DefaultServlet extends HttpServlet {

    public static final Logger LOG = LoggerFactory.getLogger(DefaultServlet.class);

    public static final String DIRECTORY_LISTING = "directory-listing";
    public static final String DEFAULT_ALLOWED = "default-allowed";
    public static final String ALLOWED_EXTENSIONS = "allowed-extensions";
    public static final String DISALLOWED_EXTENSIONS = "disallowed-extensions";
    public static final String RESOLVE_AGAINST_CONTEXT_ROOT = "resolve-against-context-root";
    public static final String ALLOW_POST = "allow-post";

    private static final Set<String> DEFAULT_ALLOWED_EXTENSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("js", "css", "png", "jpg", "gif", "html", "htm", "txt", "pdf", "jpeg", "xml")));


    private Deployment deployment;
    protected ResourceSupplier resourceSupplier;
    private boolean directoryListingEnabled = false;

    private boolean defaultAllowed = true;
    private Set<String> allowed = DEFAULT_ALLOWED_EXTENSIONS;
    private Set<String> disallowed = Collections.emptySet();
    private boolean resolveAgainstContextRoot;
    private boolean allowPost = false;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContextImpl sc = (ServletContextImpl) config.getServletContext();
        this.deployment = sc.getDeployment();
        DefaultServletConfig defaultServletConfig = deployment.getDeploymentInfo().getDefaultServletConfig();
        if (defaultServletConfig != null) {
            defaultAllowed = defaultServletConfig.isDefaultAllowed();
            allowed = new HashSet<>();
            if (defaultServletConfig.getAllowed() != null) {
                allowed.addAll(defaultServletConfig.getAllowed());
            }
            disallowed = new HashSet<>();
            if (defaultServletConfig.getDisallowed() != null) {
                disallowed.addAll(defaultServletConfig.getDisallowed());
            }
        }
        if (config.getInitParameter(DEFAULT_ALLOWED) != null) {
            defaultAllowed = Boolean.parseBoolean(config.getInitParameter(DEFAULT_ALLOWED));
        }
        if (config.getInitParameter(ALLOWED_EXTENSIONS) != null) {
            String extensions = config.getInitParameter(ALLOWED_EXTENSIONS);
            allowed = new HashSet<>(Arrays.asList(extensions.split(",")));
        }
        if (config.getInitParameter(DISALLOWED_EXTENSIONS) != null) {
            String extensions = config.getInitParameter(DISALLOWED_EXTENSIONS);
            disallowed = new HashSet<>(Arrays.asList(extensions.split(",")));
        }
        if (config.getInitParameter(RESOLVE_AGAINST_CONTEXT_ROOT) != null) {
            resolveAgainstContextRoot = Boolean.parseBoolean(config.getInitParameter(RESOLVE_AGAINST_CONTEXT_ROOT));
        }
        if (config.getInitParameter(ALLOW_POST) != null) {
            allowPost = Boolean.parseBoolean(config.getInitParameter(ALLOW_POST));
        }
        if(deployment.getDeploymentInfo().getPreCompressedResources().isEmpty()) {
            this.resourceSupplier = new DefaultResourceSupplier(deployment.getDeploymentInfo().getResourceManager());
        } else {
            PreCompressedResourceSupplier preCompressedResourceSupplier = new PreCompressedResourceSupplier(deployment.getDeploymentInfo().getResourceManager());
            for(Map.Entry<String, String> entry : deployment.getDeploymentInfo().getPreCompressedResources().entrySet()) {
                preCompressedResourceSupplier.addEncoding(entry.getKey(), entry.getValue());
            }
            this.resourceSupplier = preCompressedResourceSupplier;
        }
        String listings = config.getInitParameter(DIRECTORY_LISTING);
        if (Boolean.valueOf(listings)) {
            this.directoryListingEnabled = true;
        }
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        String path = getPath(req);
        if (!isAllowed(path, req.getDispatcherType())) {
            resp.sendError(StatusCodes.NOT_FOUND);
            return;
        }
        if(File.separatorChar != '/') {
            //if the separator char is not / we want to replace it with a / and canonicalise
            path = CanonicalPathUtils.canonicalize(path.replace(File.separatorChar, '/'));
        }

        // io.undertow.servlet.handlers.SecurityActions.requireCurrentServletRequestContext() has package access...
//        HttpServerExchange exchange = SecurityActions.requireCurrentServletRequestContext().getOriginalRequest().getExchange();
        HttpServerExchange exchange = requireCurrentServletRequestContext().getOriginalRequest().getExchange();
        final Resource resource;
        //we want to disallow windows characters in the path
        if(File.separatorChar == '/' || !path.contains(File.separator)) {
            resource = resourceSupplier.getResource(exchange, path);
        } else {
            resource = null;
        }

        if (resource == null) {
            if (req.getDispatcherType() == DispatcherType.INCLUDE) {
                //servlet 9.3
                UndertowServletLogger.REQUEST_LOGGER.requestedResourceDoesNotExistForIncludeMethod(path);
                throw new FileNotFoundException(path);
            } else {
                resp.sendError(StatusCodes.NOT_FOUND);
            }
            return;
        } else if (resource.isDirectory()) {
            if (directoryListingEnabled) {
                if ("css".equals(req.getQueryString())) {
                    resp.setContentType("text/css");
                    resp.getWriter().write(DirectoryUtils.Blobs.FILE_CSS);
                    return;
                } else if ("js".equals(req.getQueryString())) {
                    resp.setContentType("application/javascript");
                    resp.getWriter().write(DirectoryUtils.Blobs.FILE_JS);
                    return;
                }
                resp.setContentType("text/html");
                StringBuilder output = DirectoryUtils.renderDirectoryListing(exchange, req.getRequestURI(), resource);
                resp.getWriter().write(output.toString());
            } else {
                // Pax Web 8: directories without slash are redirected to make behavior consistent with
                // Jetty and Tomcat, as Undertow doesn't have similar:
                //  - org.eclipse.jetty.server.handler.ContextHandler.setAllowNullPathInfo()
                //  - org.apache.catalina.core.StandardContext.setMapperContextRootRedirectEnabled()
                boolean isContextRoot = "/".equals(path) && req.getRequestURI().equals(req.getContextPath());
                if (isContextRoot || !path.endsWith("/")) {
                    if (req.getDispatcherType() == DispatcherType.INCLUDE) {
                        LOG.warn("Can't redirect to welcome page for INCLUDE dispatch");
                        return;
                    }

                    StringBuilder location = new StringBuilder(req.getRequestURI());
                    location.append('/');
                    if (req.getQueryString() != null) {
                        location.append('?');
                        location.append(req.getQueryString());
                    }
                    // Avoid protocol relative redirects
                    while (location.length() > 1 && location.charAt(1) == '/') {
                        location.deleteCharAt(0);
                    }
                    resp.sendRedirect(resp.encodeRedirectURL(location.toString()));
                } else {
                    resp.sendError(StatusCodes.FORBIDDEN);
                }
            }
        } else {
            if(path.endsWith("/")) {
                //UNDERTOW-432
                resp.sendError(StatusCodes.NOT_FOUND);
                return;
            }
            serveFileBlocking(req, resp, resource, exchange);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(allowPost) {
            doGet(req, resp);
        } else {
            /*
             * Where a servlet has received a POST request we still require the capability to include static content.
             */
            switch (req.getDispatcherType()) {
                case INCLUDE:
                case FORWARD:
                case ERROR:
                    doGet(req, resp);
                    break;
                default:
                    super.doPost(req, resp);
            }
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        switch (req.getDispatcherType()) {
            case INCLUDE:
            case FORWARD:
            case ERROR:
                doGet(req, resp);
                break;
            default:
                super.doPut(req, resp);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        switch (req.getDispatcherType()) {
            case INCLUDE:
            case FORWARD:
            case ERROR:
                doGet(req, resp);
                break;
            default:
                super.doDelete(req, resp);
        }
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        switch (req.getDispatcherType()) {
            case INCLUDE:
            case FORWARD:
            case ERROR:
                doGet(req, resp);
                break;
            default:
                super.doOptions(req, resp);
        }
    }

    @Override
    protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        switch (req.getDispatcherType()) {
            case INCLUDE:
            case FORWARD:
            case ERROR:
                doGet(req, resp);
                break;
            default:
                super.doTrace(req, resp);
        }
    }

    private void serveFileBlocking(final HttpServletRequest req, final HttpServletResponse resp, final Resource resource, HttpServerExchange exchange) throws IOException {
        final ETag etag = resource.getETag();
        final Date lastModified = resource.getLastModified();
        if(req.getDispatcherType() != DispatcherType.INCLUDE) {
            if (!ETagUtils.handleIfMatch(req.getHeader(Headers.IF_MATCH_STRING), etag, false) ||
                    !DateUtils.handleIfUnmodifiedSince(req.getHeader(Headers.IF_UNMODIFIED_SINCE_STRING), lastModified)) {
                resp.setStatus(StatusCodes.PRECONDITION_FAILED);
                return;
            }
            if (!ETagUtils.handleIfNoneMatch(req.getHeader(Headers.IF_NONE_MATCH_STRING), etag, true) ||
                    !DateUtils.handleIfModifiedSince(req.getHeader(Headers.IF_MODIFIED_SINCE_STRING), lastModified)) {
                if(req.getMethod().equals(Methods.GET_STRING) || req.getMethod().equals(Methods.HEAD_STRING)) {
                    resp.setStatus(StatusCodes.NOT_MODIFIED);
                } else {
                    resp.setStatus(StatusCodes.PRECONDITION_FAILED);
                }
                return;
            }
        }

        //we are going to proceed. Set the appropriate headers
        if(resp.getContentType() == null) {
            if(!resource.isDirectory()) {
                final String contentType = deployment.getServletContext().getMimeType(resource.getName());
                if (contentType != null) {
                    resp.setContentType(contentType);
                } else {
                    resp.setContentType("application/octet-stream");
                }
            }
        }
        if (lastModified != null) {
            resp.setHeader(Headers.LAST_MODIFIED_STRING, resource.getLastModifiedString());
        }
        if (etag != null) {
            resp.setHeader(Headers.ETAG_STRING, etag.toString());
        }
        ByteRange.RangeResponseResult rangeResponse = null;
        long start = -1, end = -1;
        try {
            //only set the content length if we are using a stream
            //if we are using a writer who knows what the length will end up being
            //todo: if someone installs a filter this can cause problems
            //not sure how best to deal with this
            //we also can't deal with range requests if a writer is in use
            Long contentLength = resource.getContentLength();
            if (contentLength != null) {
                resp.getOutputStream();
                if(contentLength > Integer.MAX_VALUE) {
                    resp.setContentLengthLong(contentLength);
                } else {
                    resp.setContentLength(contentLength.intValue());
                }
                if(resource instanceof RangeAwareResource && ((RangeAwareResource)resource).isRangeSupported() && resource.getContentLength() != null) {
                    resp.setHeader(Headers.ACCEPT_RANGES_STRING, "bytes");
                    //TODO: figure out what to do with the content encoded resource manager
                    final ByteRange range = ByteRange.parse(req.getHeader(Headers.RANGE_STRING));
                    if(range != null) {
                        rangeResponse = range.getResponseResult(resource.getContentLength(), req.getHeader(Headers.IF_RANGE_STRING), resource.getLastModified(), resource.getETag() == null ? null : resource.getETag().getTag());
                        if(rangeResponse != null){
                            start = rangeResponse.getStart();
                            end = rangeResponse.getEnd();
                            resp.setStatus(rangeResponse.getStatusCode());
                            resp.setHeader(Headers.CONTENT_RANGE_STRING, rangeResponse.getContentRange());
                            long length = rangeResponse.getContentLength();
                            if(length > Integer.MAX_VALUE) {
                                resp.setContentLengthLong(length);
                            } else {
                                resp.setContentLength((int) length);
                            }
                            if(rangeResponse.getStatusCode() == StatusCodes.REQUEST_RANGE_NOT_SATISFIABLE) {
                                return;
                            }
                        }
                    }
                }
            }
        } catch (IllegalStateException e) {

        }
        final boolean include = req.getDispatcherType() == DispatcherType.INCLUDE;
        if (!req.getMethod().equals(Methods.HEAD_STRING)) {
            if(rangeResponse == null) {
                resource.serve(exchange.getResponseSender(), exchange, completionCallback(include));
            } else {
                ((RangeAwareResource)resource).serveRange(exchange.getResponseSender(), exchange, start, end, completionCallback(include));
            }
        }
    }

    private IoCallback completionCallback(final boolean include) {
        return new IoCallback() {

            @Override
            public void onComplete(final HttpServerExchange exchange, final Sender sender) {
                if (!include) {
                    sender.close();
                }
            }

            @Override
            public void onException(final HttpServerExchange exchange, final Sender sender, final IOException exception) {
                //not much we can do here, the connection is broken
                sender.close();
            }
        };
    }

    protected String getPath(final HttpServletRequest request) {
        String servletPath;
        String pathInfo;

        if (request.getDispatcherType() == DispatcherType.INCLUDE && request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null) {
            pathInfo = (String) request.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);
            servletPath = (String) request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
        } else {
            pathInfo = request.getPathInfo();
            servletPath = request.getServletPath();
        }
        String result = pathInfo;
        if (result == null) {
            result = CanonicalPathUtils.canonicalize(servletPath);
        } else if(resolveAgainstContextRoot) {
            result = servletPath + CanonicalPathUtils.canonicalize(pathInfo);
        } else {
            result = CanonicalPathUtils.canonicalize(result);
        }
        if ((result == null) || (result.isEmpty())) {
            result = "/";
        }
        return result;

    }

    private boolean isAllowed(String path, DispatcherType dispatcherType) {
        if (!path.isEmpty()) {
            if(dispatcherType == DispatcherType.REQUEST) {
                //WFLY-3543 allow the dispatcher to access stuff in web-inf and meta inf
                // io.undertow.servlet.handlers.Paths.isForbidden() has package access...
//                if (Paths.isForbidden(path)) {
                if (path.startsWith("/META-INF") ||
                        path.startsWith("META-INF") ||
                        path.startsWith("/WEB-INF") ||
                        path.startsWith("WEB-INF")) {
                    return false;
                }
            }
        }
        if(defaultAllowed && disallowed.isEmpty()) {
            return true;
        }
        int pos = path.lastIndexOf('/');
        final String lastSegment;
        if (pos == -1) {
            lastSegment = path;
        } else {
            lastSegment = path.substring(pos + 1);
        }
        if (lastSegment.isEmpty()) {
            return true;
        }
        int ext = lastSegment.lastIndexOf('.');
        if (ext == -1) {
            //no extension
            return true;
        }
        final String extension = lastSegment.substring(ext + 1, lastSegment.length());
        if (defaultAllowed) {
            return !disallowed.contains(extension);
        } else {
            return allowed.contains(extension);
        }
    }

    protected static ServletRequestContext requireCurrentServletRequestContext() {
        return ServletRequestContext.requireCurrent();
    }

}
// CHECKSTYLE:ON
