/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.extender.samples.whiteboard.internal;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.osgi.service.http.whiteboard.Preprocessor;

public class WhiteboardPreprocessor implements Preprocessor {

	public static AtomicInteger counter;

	private final String name;
	private final List<String> events;
	private final int id;

	private FilterConfig config;

	public WhiteboardPreprocessor(String name, List<String> events) {
		this.name = name;
		this.events = events;
		this.id = counter.incrementAndGet();
	}

	@Override
	public void init(FilterConfig config) {
		this.config = config;
		events.add(String.format("init %s/%s(%d) in %s", name, config.getFilterName(), id, config.getServletContext().getContextPath()));
	}

	@Override
	public void destroy() {
		events.add(String.format("destroy %s/%s(%d) in %s", name, config.getFilterName(), id, config.getServletContext().getContextPath()));
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		chain.doFilter(request, response);
	}

	public List<String> getEvents() {
		return events;
	}

	public int getId() {
		return id;
	}

}
