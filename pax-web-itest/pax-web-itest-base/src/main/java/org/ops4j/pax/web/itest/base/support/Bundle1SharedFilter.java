/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ops4j.pax.web.itest.base.support;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class Bundle1SharedFilter implements Filter {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(Bundle1SharedFilter.class);

	public void init(FilterConfig filterConfig) throws ServletException {
		LOGGER.info("init({})", filterConfig);
	}

	public void doFilter(ServletRequest request, ServletResponse response,
						 FilterChain chain) throws IOException, ServletException {
		LOGGER.info("doFilter({}, {}, {})", new Object[]{
				request, response, chain});
		response.getWriter().println("Before Bundle1SharedFilter");
		chain.doFilter(request, response);
		response.getWriter().println("After Bundle1SharedFilter");
	}

	public void destroy() {
		LOGGER.info("destroy()");
	}

}
