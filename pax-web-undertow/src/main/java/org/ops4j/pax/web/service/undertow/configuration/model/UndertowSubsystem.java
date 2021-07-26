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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

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

import static org.ops4j.pax.web.service.undertow.configuration.model.ObjectFactory.NS_UNDERTOW;

@XmlType(name = "undertow-subsystemType", namespace = NS_UNDERTOW, propOrder = {
		"bufferCache",
		"server",
		"servletContainer",
		"fileHandlers",
		"filters"
})
public class UndertowSubsystem {

	@XmlElement(name = "buffer-cache")
	private BufferCache bufferCache;

	@XmlElement
	private Server server;

	@XmlElement(name = "servlet-container")
	private ServletContainer servletContainer;

	@XmlElementWrapper(name = "handlers")
	@XmlElement(name = "file")
	private final List<FileHandler> fileHandlers = new ArrayList<>();

	@XmlElement
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

	@XmlType(name = "buffer-cacheType", namespace = NS_UNDERTOW)
	public static class BufferCache {
		@XmlAttribute
		private String name;
		@XmlAttribute(name = "buffer-size")
		private int bufferSize = 1024;
		@XmlAttribute(name = "buffers-per-region")
		private int buffersPerRegion = 1024;
		@XmlAttribute(name = "max-regions")
		private int maxRegions = 10;

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

	@XmlType(name = "file-handlerType", namespace = NS_UNDERTOW)
	public static class FileHandler {
		@XmlAttribute
		private String name;
		@XmlAttribute
		private String path;
		@XmlAttribute(name = "cache-buffer-size")
		private Integer cacheBufferSize = 1024;
		@XmlAttribute(name = "cache-buffers")
		private Integer cacheBuffers = 1024;
		@XmlAttribute(name = "directory-listing")
		private Boolean directoryListing = false;
		@XmlAttribute(name = "follow-symlink")
		private Boolean followSymlink = false;
		@XmlAttribute(name = "case-sensitive")
		private Boolean caseSensitive = true;
		@XmlAttribute(name = "safe-symlink-paths")
		private final List<String> safeSymlinkPaths = new ArrayList<>();

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

	@XmlType(name = "filterType", namespace = NS_UNDERTOW, propOrder = {
			"responseHeaders",
			"errorPages",
			"customFilters",
			"expressionFilters",
			"gzipFilters",
			"requestLimitFilters",
			"rewriteFilters"
	})
	public static class Filters {
		@XmlElement(name = "response-header")
		private final List<ResponseHeaderFilter> responseHeaders = new ArrayList<>();
		@XmlElement(name = "error-page")
		private final List<ErrorPageFilter> errorPages = new ArrayList<>();
		@XmlElement(name = "filter")
		private final List<CustomFilter> customFilters = new ArrayList<>();
		@XmlElement(name = "expression-filter")
		private final List<ExpressionFilter> expressionFilters = new ArrayList<>();
		@XmlElement(name = "gzip")
		private final List<GzipFilter> gzipFilters = new ArrayList<>();
		@XmlElement(name = "request-limit")
		private final List<RequestLimitFilter> requestLimitFilters = new ArrayList<>();
		@XmlElement(name = "rewrite")
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

	@XmlType(name = "abstractFilterType", namespace = NS_UNDERTOW)
	public abstract static class AbstractFilter {
		@XmlAttribute
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

	@XmlType(name = "response-headerType", namespace = NS_UNDERTOW)
	public static class ResponseHeaderFilter extends AbstractFilter {
		@XmlAttribute(name = "header-name")
		private String header;
		@XmlAttribute(name = "header-value")
		private String value;

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

	@XmlType(name = "errorPageType", namespace = NS_UNDERTOW)
	public static class ErrorPageFilter extends AbstractFilter {
		@XmlAttribute
		private String code;
		@XmlAttribute
		private String path;

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

	@XmlType(name = "customFilterType", namespace = NS_UNDERTOW)
	public static class CustomFilter extends AbstractFilter {
		@XmlAttribute(name = "class-name")
		private String className;
		@XmlAttribute
		private String module;

		@Override
		public HttpHandler configure(HttpHandler handler, Predicate predicate) {
			// TODO: use javax.servlet filters or just generic io.undertow.server.HttpHandler?
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

	@XmlType(name = "expressionFilterType", namespace = NS_UNDERTOW)
	public static class ExpressionFilter extends AbstractFilter {
		@XmlAttribute(name = "expression")
		private String expression;
		@XmlAttribute
		private String module;

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

	@XmlType(name = "gzipType", namespace = NS_UNDERTOW)
	public static class GzipFilter extends AbstractFilter {
		@Override
		public HttpHandler configure(HttpHandler handler, Predicate predicate) {
			if (predicate == null) {
				predicate = Predicates.truePredicate();
			}
			return new EncodingHandler(handler, new ContentEncodingRepository()
					.addEncodingHandler("gzip", new GzipEncodingProvider(), 50, predicate));
		}
	}

	@XmlType(name = "request-limitType", namespace = NS_UNDERTOW)
	public static class RequestLimitFilter extends AbstractFilter {
		@XmlAttribute(name = "max-concurrent-requests")
		private Integer maxConcurrentRequests = 100;
		@XmlAttribute(name = "queue-size")
		private Integer queueSize = 0;

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

	@XmlType(name = "rewriteFilterType", namespace = NS_UNDERTOW)
	public static class RewriteFilter extends AbstractFilter {
		@XmlAttribute
		private String target;
		@XmlAttribute
		private String redirect;

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
