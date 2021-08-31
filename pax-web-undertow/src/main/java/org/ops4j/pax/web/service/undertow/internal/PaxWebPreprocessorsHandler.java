/*
 * Copyright 2020 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.undertow.internal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiFilterChain;
import org.ops4j.pax.web.service.spi.servlet.PreprocessorFilterConfig;
import org.osgi.service.http.whiteboard.Preprocessor;

/**
 * This {@link HandlerWrapper} ensures that {@link org.osgi.service.http.whiteboard.Preprocessor preprocessors},
 * filters and target servlet use proper request wrapper that returns proper {@link ServletContext}.
 */
public class PaxWebPreprocessorsHandler implements HandlerWrapper {

	/**
	 * {@link Preprocessor} are registered as filters, but without particular target
	 * {@link org.ops4j.pax.web.service.spi.servlet.OsgiServletContext}, so they're effectively registered in
	 * all available physical servlet contexts.
	 */
	private final List<PreprocessorFilterConfig> preprocessors = new LinkedList<>();

	@Override
	@SuppressWarnings("Convert2Lambda")
	public HttpHandler wrap(HttpHandler handler) {
		return new HttpHandler() {
			@Override
			public void handleRequest(HttpServerExchange exchange) throws Exception {
				ServletRequestContext context = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
				HttpServletRequest incomingRequest = (HttpServletRequest) context.getServletRequest();
				HttpServletResponse outgoingRequest = (HttpServletResponse) context.getServletResponse();

				List<Preprocessor> preprocessorInstances = preprocessors.stream().map(PreprocessorFilterConfig::getInstance).collect(Collectors.toList());

				final Exception[] ex = new Exception[] { null };
				FilterChain chain = new OsgiFilterChain(new ArrayList<>(preprocessorInstances), null, null, new FilterChain() {
					@Override
					public void doFilter(ServletRequest request, ServletResponse response) {
						// just proceed
						try {
							handler.handleRequest(exchange);
						} catch (Exception e) {
							ex[0] = e;
						}
					}
				}, null);

				chain.doFilter(incomingRequest, outgoingRequest);
				if (ex[0] != null) {
					throw ex[0];
				}
			}
		};
	}

	public List<PreprocessorFilterConfig> getPreprocessors() {
		return preprocessors;
	}

}
