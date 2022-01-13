/* Copyright 2007 Alin Dreghiciu.
 *
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
package org.ops4j.pax.web.service.jetty.internal;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jetty Handler collection that calls only the handler (=context) that matches
 * the request path after performing the substring based matching of requests
 * path to registered aliases.
 *
 * @author Alin Dreghiciu
 * @since 0.2.3, December 22, 2007
 */
class JettyServerHandlerCollection extends HandlerCollection {

	private static final Logger LOG = LoggerFactory
			.getLogger(JettyServerHandlerCollection.class);

	private final ServerModel serverModel;

	JettyServerHandlerCollection(final ServerModel serverModel) {
		super(true);
		NullArgumentException.validateNotNull(serverModel, "Server model");
		this.serverModel = serverModel;
	}

	@Override
	public void handle(final String target, final Request baseRequest,
					   final HttpServletRequest request, final HttpServletResponse response)
			throws IOException, ServletException {
		if (!isStarted()) {
			return;
		}

		if ("*".equals(target) && "OPTIONS".equals(request.getMethod())) {
			// https://github.com/ops4j/org.ops4j.pax.web/issues/1667
			// see org.apache.catalina.connector.CoyoteAdapter.postParseRequest()
			response.setHeader("Allow", "GET, HEAD, POST, PUT, DELETE, OPTIONS");
			baseRequest.setHandled(true);
			return;
		}

		final ContextModel matched = serverModel.matchPathToContext(target);
		if (matched != null) {
			// check for nulls and start complaining
			NullArgumentException.validateNotNull(matched.getHttpContext(),
					"The http Context of " + matched.getContextName()
							+ " is null");
			NullArgumentException.validateNotNull(getServer(),
					"The server is null!");

			final ContextHandler context = ((JettyServerWrapper) getServer())
					.getContext(matched.getHttpContext());

			try {
				if (context == null) {
					response.setStatus(HttpServletResponse.SC_NOT_FOUND);
					return;
				}
				context.handle(target, baseRequest, request, response);

				//CHECKSTYLE:OFF
			} catch (EofException e) {
				throw e;
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new ServletException(e);
			}
			//CHECKSTYLE:ON

		}
		// now handle all other handlers
		// PAXWEB-981 - let's ensure that only one org.eclipse.jetty.server.handler.ContextHandler can handle a request
		// Servlets 3.1, 12.1 "Use of URL Paths":
		//  - Upon receipt of a client request, the Web container determines the Web application to which to forward it.
		//  - The Web container next must locate the servlet to process the request using the path mapping procedure [...]
		//  - The first successful match is used with no further matches attempted
		Handler[] handlers = getHandlers();
		if (handlers != null) {
			for (Handler handler : handlers) {
				if (matched != null
						&& (handler instanceof ContextHandler || matchedContextEqualsHandler(matched, handler))) {
					continue;
				}
				handler.handle(target, baseRequest, request, response);
			}
		}
	}

	private boolean matchedContextEqualsHandler(ContextModel matched,
												Handler handler) {
		return handler == ((JettyServerWrapper) getServer())
				.getContext(matched.getHttpContext());
	}

	@Override
	public boolean addBean(Object o) {
		LOG.debug("Adding bean: {}", o);

		if (!(o instanceof HttpServiceContext)) {
			LOG.debug("calling supper add bean ...");
			return super.addBean(o);
		}

		return addBean(o, false);
	}

}
