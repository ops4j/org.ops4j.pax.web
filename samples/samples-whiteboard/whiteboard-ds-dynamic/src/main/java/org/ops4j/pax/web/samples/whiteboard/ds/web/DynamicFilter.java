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
package org.ops4j.pax.web.samples.whiteboard.ds.web;

import org.ops4j.pax.web.samples.whiteboard.ds.AdminService;
import org.ops4j.pax.web.samples.whiteboard.ds.ManagementService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;

@Component(
		service = Filter.class, scope = ServiceScope.PROTOTYPE,
		property = HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN + "=/*"
)
public class DynamicFilter implements Filter {

	public static final Logger LOG = LoggerFactory.getLogger(DynamicFilter.class);

	@Reference(policyOption = ReferencePolicyOption.GREEDY)
	private AdminService adminService;

	@Reference(policyOption = ReferencePolicyOption.GREEDY)
	private ManagementService managementService;

	@Activate
	public void init() {
		LOG.info("DynamicFilter activated: {}, as={}, ms={}", System.identityHashCode(this),
				adminService.getAdminId(), managementService.getManagementId());
	}

	@Deactivate
	public void destroy() {
		LOG.info("DynamicFilter deactivated: {}, as={}, ms={}", System.identityHashCode(this),
				adminService.getAdminId(), managementService.getManagementId());
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		response.getWriter().print("<" + adminService.getAdminId() + ">");
		chain.doFilter(request, response);
		response.getWriter().print("<" + managementService.getManagementId() + ">");
	}

}
