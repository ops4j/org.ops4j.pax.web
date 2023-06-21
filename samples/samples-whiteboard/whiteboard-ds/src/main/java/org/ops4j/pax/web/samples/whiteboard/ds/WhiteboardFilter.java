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
package org.ops4j.pax.web.samples.whiteboard.ds;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;

@Component(
		service = { Filter.class, WhiteboardFilter.class }, // WhiteboardFilter only for testing
		scope = ServiceScope.PROTOTYPE,
		property = {
				HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN + "=/simple-servlet",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME + "=SimpleFilter"
		}
)
public class WhiteboardFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		response.getWriter().println("Request changed by SimpleFilter");
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
	}

}
