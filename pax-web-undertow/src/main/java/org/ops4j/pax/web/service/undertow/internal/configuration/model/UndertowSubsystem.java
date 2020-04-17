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
package org.ops4j.pax.web.service.undertow.internal.configuration.model;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import io.undertow.Handlers;
import io.undertow.predicate.Predicate;
import io.undertow.predicate.Predicates;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.SetHeaderHandler;
import io.undertow.server.handlers.builder.HandlerParser;

import static org.ops4j.pax.web.service.undertow.internal.configuration.model.ObjectFactory.NS_UNDERTOW;

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
	private List<FileHandler> fileHandlers = new ArrayList<>();

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
		final StringBuilder sb = new StringBuilder("{");
		sb.append("\n\t\tbuffer cache: " + bufferCache);
		sb.append("\n\t\tserver: " + server);
		sb.append("\n\t\tservlet container: " + servletContainer);
		sb.append("\n\t}");
		return sb.toString();
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
			final StringBuilder sb = new StringBuilder("{ ");
			sb.append("name: ").append(name);
			sb.append(", buffer size: ").append(bufferSize);
			sb.append(", buffers per region: ").append(buffersPerRegion);
			sb.append(", max regions: ").append(maxRegions);
			sb.append(" }");
			return sb.toString();
		}
	}

	@XmlType(name = "file-handlerType", namespace = NS_UNDERTOW)
	public static class FileHandler {
		@XmlAttribute
		private String name;
		@XmlAttribute
		private String path;

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
	}

	@XmlType(name = "filterType", namespace = NS_UNDERTOW, propOrder = {
			"responseHeaders",
			"errorPages",
			"customFilters",
			"expressionFilters"
	})
	public static class Filters {
		@XmlElement(name = "response-header")
		private List<ResponseHeaderFilter> responseHeaders = new ArrayList<>();
		@XmlElement(name = "error-page")
		private List<ErrorPageFilter> errorPages = new ArrayList<>();
		@XmlElement(name = "filter")
		private List<CustomFilter> customFilters = new ArrayList<>();
		@XmlElement(name = "expression-filter")
		private List<ExpressionFilter> expressionFilters = new ArrayList<>();

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
		public abstract HttpHandler configure(HttpHandler handler, String predicate);
	}

	@XmlType(name = "response-headerType", namespace = NS_UNDERTOW)
	public static class ResponseHeaderFilter extends AbstractFilter {
		@XmlAttribute(name = "header-name")
		private String header;
		@XmlAttribute(name = "header-value")
		private String value;

		@Override
		public HttpHandler configure(HttpHandler handler, String predicate) {
			SetHeaderHandler setHeaderHandler = new SetHeaderHandler(handler, header, value);
			if (predicate == null) {
				return setHeaderHandler;
			}
			Predicate p = Predicates.parse(predicate, HttpHandler.class.getClassLoader());
			// predicate means "apply SetHeaderHandler if predicate matches, otherwise forward to passed handler"
			return Handlers.predicate(p, setHeaderHandler, handler);
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
		public HttpHandler configure(HttpHandler handler, String predicate) {
			// TODO: not sure what to do here
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
		public HttpHandler configure(HttpHandler handler, String predicate) {
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
		public HttpHandler configure(HttpHandler handler, String predicate) {
			HandlerWrapper wrapper = HandlerParser.parse(expression, HttpHandler.class.getClassLoader());
			if (predicate == null) {
				return wrapper.wrap(handler);
			}
			Predicate p = Predicates.parse(predicate, HttpHandler.class.getClassLoader());
			// predicate means "apply expression if predicate matches, otherwise forward to passed handler withour processing"
			return Handlers.predicate(p, wrapper.wrap(handler), handler);
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

}
