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
package org.ops4j.pax.web.service.undertow.internal;

import java.util.Dictionary;
import javax.servlet.Servlet;

import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.cache.LRUCache;
import org.ops4j.pax.web.annotations.Review;
import org.osgi.service.http.HttpContext;

/**
 * <p>A {@link PathHandler} that can (try to) resolve conflicts when registering multiple contexts under
 * the same path.</p>
 * <p>The problem is that when using {@code etc/undertow.xml} with {@code <location>} handlers <strong>and</strong>
 * when using normal WARs and {@link org.osgi.service.http.HttpService#registerServlet(String, Servlet, Dictionary, HttpContext)} we may
 * end up with multiple {@link io.undertow.server.HttpHandler handlers} handling {@code "/"} path. We should always prefer
 * the path from {@code etc/undertow.xml} configuration - assuming it's configured and user know what (s)he's
 * doing.</p>
 * <p>It's a direct fork of original {@link PathHandler} but with different {@link io.undertow.util.PathMatcher}</p>
 */
@Review("Is it needed?")
public class ContextAwarePathHandler extends PathHandler {

	private final PathMatcher<HttpHandler> pathMatcher = new PathMatcher<>();

	private final LRUCache<String, PathMatcher.PathMatch<HttpHandler>> cache;

	public ContextAwarePathHandler(final HttpHandler defaultHandler) {
		this(0);
		pathMatcher.addPrefixPath("/", defaultHandler);
	}

	public ContextAwarePathHandler(final HttpHandler defaultHandler, int cacheSize) {
		this(cacheSize);
		pathMatcher.addPrefixPath("/", defaultHandler);
	}

	public ContextAwarePathHandler() {
		this(0);
	}

	public ContextAwarePathHandler(int cacheSize) {
		if (cacheSize > 0) {
			cache = new LRUCache<>(cacheSize, -1, true);
		} else {
			cache = null;
		}
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		PathMatcher.PathMatch<HttpHandler> match = null;
		boolean hit = false;
		if (cache != null) {
			match = cache.get(exchange.getRelativePath());
			hit = true;
		}
		if (match == null) {
			match = pathMatcher.match(exchange.getRelativePath());
		}
		if (match.getValue() == null) {
			ResponseCodeHandler.HANDLE_404.handleRequest(exchange);
			return;
		}
		if (hit) {
			cache.add(exchange.getRelativePath(), match);
		}
		exchange.setRelativePath(match.getRemaining());
		if (exchange.getResolvedPath().isEmpty()) {
			//first path handler, we can just use the matched part
			exchange.setResolvedPath(match.getMatched());
		} else {
			//already something in the resolved path
			StringBuilder sb = new StringBuilder(exchange.getResolvedPath().length() + match.getMatched().length());
			sb.append(exchange.getResolvedPath());
			sb.append(match.getMatched());
			exchange.setResolvedPath(sb.toString());
		}
		match.getValue().handleRequest(exchange);
	}

	/**
	 * Adds a path prefix and a handler for that path. If the path does not start
	 * with a / then one will be prepended.
	 * <p>
	 * The match is done on a prefix bases, so registering /foo will also match /bar. Exact
	 * path matches are taken into account first.
	 * <p>
	 * If / is specified as the path then it will replace the default handler.
	 *
	 * @param path    The path
	 * @param handler The handler
	 * @see #addPrefixPath(String, io.undertow.server.HttpHandler)
	 * @deprecated Superseded by {@link #addPrefixPath(String, io.undertow.server.HttpHandler)}.
	 */
	@Deprecated
	public synchronized PathHandler addPath(final String path, final HttpHandler handler) {
		return addPrefixPath(path, handler);
	}

	/**
	 * Adds a path prefix and a handler for that path. If the path does not start
	 * with a / then one will be prepended.
	 * <p>
	 * The match is done on a prefix bases, so registering /foo will also match /foo/bar.
	 * Though exact path matches are taken into account before prefix path matches. So
	 * if an exact path match exists it's  handler will be triggered.
	 * <p>
	 * If / is specified as the path then it will replace the default handler.
	 *
	 * @param path    If the request contains this prefix, run handler.
	 * @param handler The handler which is activated upon match.
	 * @return The resulting PathHandler after this path has been added to it.
	 */
	public synchronized PathHandler addPrefixPath(final String path, final HttpHandler handler) {
		Handlers.handlerNotNull(handler);
		pathMatcher.addPrefixPath(path, handler);
		return this;
	}

	/**
	 * If the request path is exactly equal to the given path, run the handler.
	 * <p>
	 * Exact paths are prioritized higher than prefix paths.
	 *
	 * @param path If the request path is exactly this, run handler.
	 * @param handler Handler run upon exact path match.
	 * @return The resulting PathHandler after this path has been added to it.
	 */
	public synchronized PathHandler addExactPath(final String path, final HttpHandler handler) {
		Handlers.handlerNotNull(handler);
		pathMatcher.addExactPath(path, handler);
		return this;
	}

	@Deprecated
	public synchronized PathHandler removePath(final String path) {
		return removePrefixPath(path);
	}

	public synchronized PathHandler removePrefixPath(final String path) {
		pathMatcher.removePrefixPath(path);
		return this;
	}

	public synchronized PathHandler removeExactPath(final String path) {
		pathMatcher.removeExactPath(path);
		return this;
	}

	public synchronized PathHandler clearPaths() {
		pathMatcher.clearPaths();
		return this;
	}

	public HttpHandler getDefaultHandler() {
		return pathMatcher.getDefaultHandler();
	}

}
