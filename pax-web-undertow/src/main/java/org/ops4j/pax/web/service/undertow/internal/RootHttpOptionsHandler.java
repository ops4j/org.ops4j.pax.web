/*
 * Copyright 2022 OPS4J.
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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;

public class RootHttpOptionsHandler implements HttpHandler {

	private final String allowedMethods;
	private final HttpHandler next;

	public RootHttpOptionsHandler(HttpHandler next, List<String> disallowedMethods) {
		Set<String> methods = new LinkedHashSet<>(Arrays.asList("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS", "TRACE"));
		for (String m : disallowedMethods) {
			methods.remove(m);
		}
		this.allowedMethods = String.join(", ", methods);
		this.next = next;
	}

	@Override
	public void handleRequest(final HttpServerExchange exchange) throws Exception {
		if (Methods.OPTIONS.equals(exchange.getRequestMethod()) && "*".equals(exchange.getRequestPath())) {
			exchange.setStatusCode(StatusCodes.OK);
			exchange.getResponseHeaders().put(Headers.ALLOW, allowedMethods);
			exchange.endExchange();
		} else {
			next.handleRequest(exchange);
		}
	}

}
