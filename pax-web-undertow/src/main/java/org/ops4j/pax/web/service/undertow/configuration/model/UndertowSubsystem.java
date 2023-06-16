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
package org.ops4j.pax.web.service.undertow.configuration.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import io.undertow.Handlers;
import io.undertow.attribute.ExchangeAttributes;
import io.undertow.predicate.Predicate;
import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.RedirectHandler;
import io.undertow.server.handlers.RequestLimitingHandler;
import io.undertow.server.handlers.SetAttributeHandler;
import io.undertow.server.handlers.SetHeaderHandler;
import io.undertow.server.handlers.builder.PredicatedHandler;
import io.undertow.server.handlers.builder.PredicatedHandlersParser;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import org.ops4j.pax.web.service.undertow.internal.configuration.ParserUtils;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

public class UndertowSubsystem {

	private BufferCache bufferCache;

	private Server server;

	private ServletContainer servletContainer;

	private final List<FileHandler> fileHandlers = new ArrayList<>();

	private Filters filters;

	public BufferCache getBufferCache() {
		return bufferCache;
	}

	public void setBufferCache(BufferCache bufferCache) {
		this.bufferCache = bufferCache;
	}

	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	public ServletContainer getServletContainer() {
		return servletContainer;
	}

	public void setServletContainer(ServletContainer servletContainer) {
		this.servletContainer = servletContainer;
	}

	public Filters getFilters() {
		return filters;
	}

	public void setFilters(Filters filters) {
		this.filters = filters;
	}

	public List<FileHandler> getFileHandlers() {
		return fileHandlers;
	}

	@Override
	public String toString() {
		return "{\n\t\tbuffer cache: " + bufferCache +
				"\n\t\tserver: " + server +
				"\n\t\tservlet container: " + servletContainer +
				"\n\t}";
	}

	public static class BufferCache {
		private static final QName ATT_NAME = new QName("name");
		private static final QName ATT_BUFFER_SIZE = new QName("buffer-size");
		private static final QName ATT_BUFFERS_PER_REGION = new QName("buffers-per-region");
		private static final QName ATT_MAX_REGIONS = new QName("max-regions");

		private String name;
		private int bufferSize = 1024;
		private int buffersPerRegion = 1024;
		private int maxRegions = 10;

		public static BufferCache create(Map<QName, String> attributes, Locator locator) throws SAXParseException {
			BufferCache cache = new BufferCache();
			cache.name = attributes.get(ATT_NAME);
			cache.bufferSize = ParserUtils.toInteger(attributes.get(ATT_BUFFER_SIZE), locator, 1024);
			cache.buffersPerRegion = ParserUtils.toInteger(attributes.get(ATT_BUFFERS_PER_REGION), locator, 1024);
			cache.maxRegions = ParserUtils.toInteger(attributes.get(ATT_MAX_REGIONS), locator, 10);

			return cache;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getBufferSize() {
			return bufferSize;
		}

		public void setBufferSize(int bufferSize) {
			this.bufferSize = bufferSize;
		}

		public int getBuffersPerRegion() {
			return buffersPerRegion;
		}

		public void setBuffersPerRegion(int buffersPerRegion) {
			this.buffersPerRegion = buffersPerRegion;
		}

		public int getMaxRegions() {
			return maxRegions;
		}

		public void setMaxRegions(int maxRegions) {
			this.maxRegions = maxRegions;
		}

		@Override
		public String toString() {
			return "{ name: " + name +
					", buffer size: " + bufferSize +
					", buffers per region: " + buffersPerRegion +
					", max regions: " + maxRegions +
					" }";
		}
	}

	public static class FileHandler {
		private static final QName ATT_NAME = new QName("name");
		private static final QName ATT_PATH = new QName("path");
		private static final QName ATT_CACHE_BUFFER_SIZE = new QName("cache-buffer-size");
		private static final QName ATT_CACHE_BUFFERS = new QName("cache-buffers");
		private static final QName ATT_DIRECTORY_LISTING = new QName("directory-listing");
		private static final QName ATT_FOLLOW_SYMLINKS = new QName("follow-symlink");
		private static final QName ATT_CASE_SENSITIVE = new QName("case-sensitive");
		private static final QName ATT_SAFE_SYMLINK_PATHS = new QName("safe-symlink-paths");

		private String name;
		private String path;
		private Integer cacheBufferSize = 1024;
		private Integer cacheBuffers = 1024;
		private Boolean directoryListing = false;
		private Boolean followSymlink = false;
		private Boolean caseSensitive = true;
		private final List<String> safeSymlinkPaths = new ArrayList<>();

		public static FileHandler create(Map<QName, String> attributes, Locator locator) throws SAXParseException {
			FileHandler handler = new FileHandler();
			handler.name = attributes.get(ATT_NAME);
			handler.path = attributes.get(ATT_PATH);
			handler.cacheBufferSize = ParserUtils.toInteger(attributes.get(ATT_CACHE_BUFFER_SIZE), locator, 1024);
			handler.cacheBuffers = ParserUtils.toInteger(attributes.get(ATT_CACHE_BUFFERS), locator, 1024);
			handler.directoryListing = ParserUtils.toBoolean(attributes.get(ATT_DIRECTORY_LISTING), locator, false);
			handler.followSymlink = ParserUtils.toBoolean(attributes.get(ATT_FOLLOW_SYMLINKS), locator, false);
			handler.caseSensitive = ParserUtils.toBoolean(attributes.get(ATT_CASE_SENSITIVE), locator, true);
			handler.safeSymlinkPaths.addAll(ParserUtils.toStringList(attributes.get(ATT_SAFE_SYMLINK_PATHS), locator));

			return handler;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public Integer getCacheBufferSize() {
			return cacheBufferSize;
		}

		public void setCacheBufferSize(Integer cacheBufferSize) {
			this.cacheBufferSize = cacheBufferSize;
		}

		public Integer getCacheBuffers() {
			return cacheBuffers;
		}

		public void setCacheBuffers(Integer cacheBuffers) {
			this.cacheBuffers = cacheBuffers;
		}

		public Boolean getDirectoryListing() {
			return directoryListing;
		}

		public void setDirectoryListing(Boolean directoryListing) {
			this.directoryListing = directoryListing;
		}

		public Boolean getFollowSymlink() {
			return followSymlink;
		}

		public void setFollowSymlink(Boolean followSymlink) {
			this.followSymlink = followSymlink;
		}

		public Boolean getCaseSensitive() {
			return caseSensitive;
		}

		public void setCaseSensitive(Boolean caseSensitive) {
			this.caseSensitive = caseSensitive;
		}

		public List<String> getSafeSymlinkPaths() {
			return safeSymlinkPaths;
		}
	}

	public static class Filters {
		private final List<ResponseHeaderFilter> responseHeaders = new ArrayList<>();
		private final List<ErrorPageFilter> errorPages = new ArrayList<>();
		private final List<CustomFilter> customFilters = new ArrayList<>();
		private final List<ExpressionFilter> expressionFilters = new ArrayList<>();
		private final List<GzipFilter> gzipFilters = new ArrayList<>();
		private final List<RequestLimitFilter> requestLimitFilters = new ArrayList<>();
		private final List<RewriteFilter> rewriteFilters = new ArrayList<>();

		public List<ResponseHeaderFilter> getResponseHeaders() {
			return responseHeaders;
		}

		public List<ErrorPageFilter> getErrorPages() {
			return errorPages;
		}

		public List<CustomFilter> getCustomFilters() {
			return customFilters;
		}

		public List<ExpressionFilter> getExpressionFilters() {
			return expressionFilters;
		}

		public List<GzipFilter> getGzipFilters() {
			return gzipFilters;
		}

		public List<RequestLimitFilter> getRequestLimitFilters() {
			return requestLimitFilters;
		}

		public List<RewriteFilter> getRewriteFilters() {
			return rewriteFilters;
		}
	}

	public abstract static class AbstractFilter {
		protected static final QName ATT_NAME = new QName("name");

		protected String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		/**
		 * Configures given filter using <code>handler</code> as <em>next</em> {@link HttpHandler}
		 * @param handler
		 * @param predicate
		 * @return
		 */
		public abstract HttpHandler configure(HttpHandler handler, Predicate predicate);
	}

	public static class ResponseHeaderFilter extends AbstractFilter {
		private static final QName ATT_HEADER_NAME = new QName("header-name");
		private static final QName ATT_HEADER_VALUE = new QName("header-value");

		private String header;
		private String value;

		public static ResponseHeaderFilter create(Map<QName, String> attributes, Locator locator) {
			ResponseHeaderFilter filter = new ResponseHeaderFilter();
			filter.name = attributes.get(ATT_NAME);
			filter.header = attributes.get(ATT_HEADER_NAME);
			filter.value = attributes.get(ATT_HEADER_VALUE);

			return filter;
		}

		@Override
		public HttpHandler configure(HttpHandler handler, Predicate predicate) {
			SetHeaderHandler setHeaderHandler = new SetHeaderHandler(handler, header, value);
			if (predicate == null) {
				return setHeaderHandler;
			}
			return Handlers.predicate(predicate, setHeaderHandler, handler);
		}

		public String getHeader() {
			return header;
		}

		public void setHeader(String header) {
			this.header = header;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	public static class ErrorPageFilter extends AbstractFilter {
		private static final QName ATT_CODE = new QName("code");
		private static final QName ATT_PATH = new QName("path");

		private String code;
		private String path;

		public static ErrorPageFilter create(Map<QName, String> attributes, Locator locator) {
			ErrorPageFilter filter = new ErrorPageFilter();
			filter.name = attributes.get(ATT_NAME);
			filter.code = attributes.get(ATT_CODE);
			filter.path = attributes.get(ATT_PATH);

			return filter;
		}

		@Override
		public HttpHandler configure(HttpHandler handler, Predicate predicate) {
			// not handled by Pax Web - error pages are configured using OSGi means (Whiteboard, WAB)
			return handler;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}
	}

	public static class CustomFilter extends AbstractFilter {
		private static final QName ATT_CLASS_NAME = new QName("class-name");
		private static final QName ATT_MODULE = new QName("module");

		private String className;
		private String module;

		public static CustomFilter create(Map<QName, String> attributes, Locator locator) {
			CustomFilter filter = new CustomFilter();
			filter.name = attributes.get(ATT_NAME);
			filter.className = attributes.get(ATT_CLASS_NAME);
			filter.module = attributes.get(ATT_MODULE);

			return filter;
		}

		@Override
		public HttpHandler configure(HttpHandler handler, Predicate predicate) {
			// TODO: use jakarta.servlet filters or just generic io.undertow.server.HttpHandler?
			return handler;
		}

		public String getClassName() {
			return className;
		}

		public void setClassName(String className) {
			this.className = className;
		}

		public String getModule() {
			return module;
		}

		public void setModule(String module) {
			this.module = module;
		}
	}

	public static class ExpressionFilter extends AbstractFilter {
		private static final QName ATT_EXPRESSION = new QName("expression");
		private static final QName ATT_MODULE = new QName("module");

		private String expression;
		private String module;

		public static ExpressionFilter create(Map<QName, String> attributes, Locator locator) {
			ExpressionFilter filter = new ExpressionFilter();
			filter.name = attributes.get(ATT_NAME);
			filter.expression = attributes.get(ATT_EXPRESSION);
			filter.module = attributes.get(ATT_MODULE);

			return filter;
		}

		@Override
		public HttpHandler configure(HttpHandler handler, Predicate predicate) {
			List<PredicatedHandler> handlers = PredicatedHandlersParser.parse(expression, HttpHandler.class.getClassLoader());
			// predicate means "apply expression if predicate matches, otherwise forward to passed handler withour processing"
			if (predicate == null) {
				return Handlers.predicates(handlers, handler);
			}
			return Handlers.predicate(predicate, Handlers.predicates(handlers, handler), handler);
		}

		public String getExpression() {
			return expression;
		}

		public void setExpression(String expression) {
			this.expression = expression;
		}

		public String getModule() {
			return module;
		}

		public void setModule(String module) {
			this.module = module;
		}
	}

	public static class GzipFilter extends AbstractFilter {

		public static GzipFilter create(Map<QName, String> attributes, Locator locator) {
			GzipFilter filter = new GzipFilter();
			filter.name = attributes.get(ATT_NAME);

			return filter;
		}

		@Override
		public HttpHandler configure(HttpHandler handler, Predicate predicate) {
			if (predicate == null) {
				predicate = Predicates.truePredicate();
			}
			return new EncodingHandler(handler, new ContentEncodingRepository()
					.addEncodingHandler("gzip", new GzipEncodingProvider(), 50, predicate));
		}
	}

	public static class RequestLimitFilter extends AbstractFilter {
		private static final QName ATT_MAX_CONCURRENT_REQUESTS = new QName("max-concurrent-requests");
		private static final QName ATT_QUEUE_SIZE = new QName("queue-size");

		private Integer maxConcurrentRequests = 100;
		private Integer queueSize = 0;

		public static RequestLimitFilter create(Map<QName, String> attributes, Locator locator) throws SAXParseException {
			RequestLimitFilter filter = new RequestLimitFilter();
			filter.name = attributes.get(ATT_NAME);
			filter.maxConcurrentRequests = ParserUtils.toInteger(attributes.get(ATT_MAX_CONCURRENT_REQUESTS), locator, 100);
			filter.queueSize = ParserUtils.toInteger(attributes.get(ATT_QUEUE_SIZE), locator, 0);

			return filter;
		}

		public Integer getMaxConcurrentRequests() {
			return maxConcurrentRequests;
		}

		public void setMaxConcurrentRequests(Integer maxConcurrentRequests) {
			this.maxConcurrentRequests = maxConcurrentRequests;
		}

		public Integer getQueueSize() {
			return queueSize;
		}

		public void setQueueSize(Integer queueSize) {
			this.queueSize = queueSize;
		}

		@Override
		public HttpHandler configure(HttpHandler handler, Predicate predicate) {
			HttpHandler requestLimitingHandler = new RequestLimitingHandler(maxConcurrentRequests, queueSize, handler);
			if (predicate == null) {
				return requestLimitingHandler;
			}
			return Handlers.predicate(predicate, requestLimitingHandler, handler);
		}
	}

	public static class RewriteFilter extends AbstractFilter {
		private static final QName ATT_TARGET = new QName("target");
		private static final QName ATT_REDIRECT = new QName("redirect");

		private String target;
		private String redirect;

		public static RewriteFilter create(Map<QName, String> attributes, Locator locator) {
			RewriteFilter filter = new RewriteFilter();
			filter.name = attributes.get(ATT_NAME);
			filter.target = attributes.get(ATT_TARGET);
			filter.redirect = attributes.get(ATT_REDIRECT);

			return filter;
		}

		public String getTarget() {
			return target;
		}

		public void setTarget(String target) {
			this.target = target;
		}

		public String getRedirect() {
			return redirect;
		}

		public void setRedirect(String redirect) {
			this.redirect = redirect;
		}

		@Override
		public HttpHandler configure(HttpHandler handler, Predicate predicate) {
			HttpHandler redirectHandler;
			if ("true".equalsIgnoreCase(redirect)) {
				redirectHandler = new RedirectHandler(target);
			} else {
				redirectHandler = new SetAttributeHandler(handler, ExchangeAttributes.relativePath(),
						ExchangeAttributes.parser(HttpHandler.class.getClassLoader()).parse(target));
			}
			if (predicate == null) {
				return redirectHandler;
			}
			return Handlers.predicate(predicate, redirectHandler, handler);
		}
	}

}
