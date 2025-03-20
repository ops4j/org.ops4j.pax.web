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
package org.ops4j.pax.web.service.jetty.internal;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.Callback;

/**
 * <p>This {@link ContextHandlerCollection} keeps three sets of {@link org.eclipse.jetty.server.Handler handlers}:<ul>
 * <li>OSGi-registered {@link org.eclipse.jetty.server.Handler handlers} with priority higher than 0</li>
 * <li><em>Main</em> {@link ContextHandlerCollection} to keep actual {@link org.eclipse.jetty.server.handler.ContextHandler}
 * instances</li>
 * <li>OSGi-registered {@link org.eclipse.jetty.server.Handler handlers} with priority lower or equal 0</li>
 * </ul></p>
 */
public class PrioritizedHandlerCollection extends ContextHandlerCollection {

	protected final ContextHandlerCollection handlerCollectionBefore = new ContextHandlerCollection();
	protected final ContextHandlerCollection handlerCollectionAfter = new ContextHandlerCollection();

	private final Set<PriorityValue<Handler>> handlersBefore = new TreeSet<>(JettyServerControllerFactory.priorityComparator);
	private final Set<PriorityValue<Handler>> handlersAfter = new TreeSet<>(JettyServerControllerFactory.priorityComparator);

	/**
	 * Dedicated method to add a handler retrieved from OSGi registry. If the priority is higher than 0, this
	 * {@link Handler} should be called before calling actual context handlers. if the priority is lower or equal
	 * 0, such {@link Handler} should be called after any context handler.
	 *
	 * @param handlers
	 */
	public void setPriorityHandlers(Set<PriorityValue<Handler>> handlers) {
		handlers.forEach(pv -> {
			if (pv.getPriority() > 0) {
				handlersBefore.add(pv);
			} else {
				handlersAfter.add(pv);
			}
		});
		Handler[] handlersA = handlersBefore.stream().map(PriorityValue::getValue).toArray(Handler[]::new);
		if (handlersA.length > 0) {
			handlerCollectionBefore.setHandlers(handlersA);
		}
		Handler[] handlersZ = handlersAfter.stream().map(PriorityValue::getValue).toArray(Handler[]::new);
		if (handlersZ.length > 0) {
			handlerCollectionAfter.setHandlers(handlersZ);
		}
	}

	@Override
	protected void doStart() throws Exception {
		super.doStart();
		handlerCollectionBefore.setServer(getServer());
		handlerCollectionAfter.setServer(getServer());
		handlerCollectionBefore.start();
		handlerCollectionAfter.start();
	}

	@Override
	protected void doStop() throws Exception {
		super.doStop();
		handlerCollectionBefore.stop();
		handlerCollectionAfter.stop();
	}

	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {
		if (isStarted()) {
			try {
				boolean handled = handlerCollectionBefore.handle(request, response, callback);
				if (!handled) {
					String target = Request.getPathInContext(request);
					// https://github.com/ops4j/org.ops4j.pax.web/issues/1664
					if ("OPTIONS".equals(request.getMethod()) && "*".equals(target)) {
						response.setStatus(HttpServletResponse.SC_OK);
						response.getHeaders().add("Allow", "GET, HEAD, POST, PUT, DELETE, OPTIONS");
						callback.succeeded();
						return true;
					}
					// User should know what (s)he's doing - if a handler marks the request as handled, there's
					// no need to call real context handlers.
					handled = super.handle(request, response, callback);
				}
				// however, let's allow the "after" handlers to run - whatever they are
				handled |= handlerCollectionAfter.handle(request, response, callback);
				return handled;
			} catch (IOException | RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new ServletException(e);
			}
		}
		return true;
	}

}
