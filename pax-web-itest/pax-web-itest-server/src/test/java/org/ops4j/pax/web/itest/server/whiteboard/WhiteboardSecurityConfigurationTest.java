/*
 * Copyright 2023 OPS4J.
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
package org.ops4j.pax.web.itest.server.whiteboard;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultSecurityConfigurationMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultSecurityConstraintMapping;
import org.ops4j.pax.web.itest.server.MultiContainerTestSupport;
import org.ops4j.pax.web.itest.server.Runtime;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.internal.HttpServiceEnabled;
import org.ops4j.pax.web.service.internal.StoppableHttpService;
import org.ops4j.pax.web.service.spi.model.elements.SecurityConfigurationModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.whiteboard.SecurityConfigurationMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.servlet.context.ServletContextHelper;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;

import jakarta.servlet.Servlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.Collections;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@RunWith(Parameterized.class)
public class WhiteboardSecurityConfigurationTest extends MultiContainerTestSupport {

	@Test
	public void whiteboardSecurityConfiguration() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		ServletContextHelper helper1 = new ServletContextHelper() { };
		getServletContextHelperCustomizer().addingService(mockServletContextHelperReference(sample1, "c1",
				() -> helper1, 0L, 0, "/c1"));
		ServletContextHelper helper2 = new ServletContextHelper() { };
		getServletContextHelperCustomizer().addingService(mockServletContextHelperReference(sample1, "c2",
				() -> helper2, 0L, 0, "/c2"));

		// a servlet registered to two contexts
		ServiceReference<Servlet> servletRef = mockServletReference(sample1, "servlet1",
				() -> new TestServlet("1"), 0L, 0, "/s/*", "/t/*");
		when(servletRef.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT))
				.thenReturn("(|(osgi.http.whiteboard.context.name=c1)(osgi.http.whiteboard.context.name=c2))");
		ServletModel model = getServletCustomizer().addingService(servletRef);

		// no security, but user present
		String response = httpGET(port, "/c1/s", "Authorization: Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8)));
		assertTrue(response.contains("user principal: null"));
		assertTrue(response.contains("user principal class: null"));
		assertTrue(response.contains("user is admin: false"));
		response = httpGET(port, "/c2/s", "Authorization: Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8)));
		assertTrue(response.contains("user principal: null"));
		assertTrue(response.contains("user principal class: null"));
		assertTrue(response.contains("user is admin: false"));
		// no security, no user
		response = httpGET(port, "/c1/s");
		assertTrue(response.contains("user principal: null"));
		assertTrue(response.contains("user principal class: null"));
		assertTrue(response.contains("user is admin: false"));
		response = httpGET(port, "/c2/s");
		assertTrue(response.contains("user principal: null"));
		assertTrue(response.contains("user principal class: null"));
		assertTrue(response.contains("user is admin: false"));

		DefaultSecurityConfigurationMapping security = new DefaultSecurityConfigurationMapping();
		security.setContextSelectFilter("(osgi.http.whiteboard.context.name=c1)");
		security.setAuthMethod("BASIC");
		security.setRealmName("test");
		DefaultSecurityConstraintMapping constraint = new DefaultSecurityConstraintMapping();
		constraint.setName("secure-area");
		constraint.setAuthRolesSet(true);
		constraint.getAuthRoles().add("admin");
		DefaultSecurityConstraintMapping.DefaultWebResourceCollectionMapping wrc = new DefaultSecurityConstraintMapping.DefaultWebResourceCollectionMapping();
		wrc.setName("protected /s/*");
		wrc.getUrlPatterns().add("/s/*");
		constraint.getWebResourceCollections().add(wrc);
		security.getSecurityConstraints().add(constraint);
		security.getSecurityRoles().add("admin");

		ServiceReference<SecurityConfigurationMapping> secConfigRef = mockReference(sample1, SecurityConfigurationMapping.class,
				null, () -> security, 0L, 0);

		SecurityConfigurationModel securityConfigurationModel = getSecurityConfigurationCustomizer().addingService(secConfigRef);

		// basic auth security for protected resource in protected context
		response = httpGET(port, "/c1/s/test", "Authorization: Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8)));
		if (runtime == Runtime.JETTY) {
			assertTrue(response.contains("user principal: admin"));
			assertTrue(response.contains("user principal class: org.eclipse.jetty.security.UserPrincipal"));
			assertTrue(response.contains("user is admin: true"));
		} else if (runtime == Runtime.TOMCAT) {
			assertTrue(response.contains("user principal: admin"));
			assertTrue(response.contains("user principal class: org.apache.catalina.realm.GenericPrincipal"));
			assertTrue(response.contains("user is admin: true"));
		} else if (runtime == Runtime.UNDERTOW) {
			assertTrue(response.contains("user principal: admin"));
			assertTrue(response.contains("user principal class: org.ops4j.pax.web.service.undertow.internal.security.PropertiesIdentityManager$SimplePrincipal"));
			assertTrue(response.contains("user is admin: true"));
		}
		// no auth for protected resource in protected context
		response = httpGET(port, "/c1/s/test");
		assertTrue(response.startsWith("HTTP/1.1 401"));

		// basic auth security for unprotected resource in protected context
		response = httpGET(port, "/c1/t/test", "Authorization: Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8)));
		if (runtime == Runtime.JETTY) {
			assertTrue(response.contains("user principal: admin"));
			assertTrue(response.contains("user principal class: org.eclipse.jetty.security.UserPrincipal"));
			assertTrue(response.contains("user is admin: true"));
		} else if (runtime == Runtime.TOMCAT) {
			// Tomcat doesn't perform authentication if there are no constraints configured
			// see org.apache.catalina.authenticator.AuthenticatorBase.invoke()
			assertTrue(response.contains("user principal: null"));
			assertTrue(response.contains("user principal class: null"));
			assertTrue(response.contains("user is admin: false"));
		} else if (runtime == Runtime.UNDERTOW) {
			assertTrue(response.contains("user principal: admin"));
			assertTrue(response.contains("user principal class: org.ops4j.pax.web.service.undertow.internal.security.PropertiesIdentityManager$SimplePrincipal"));
			assertTrue(response.contains("user is admin: true"));
		}
		// no auth for unprotected resource in protected context
		response = httpGET(port, "/c1/t/test");
		assertTrue(response.contains("user principal: null"));
		assertTrue(response.contains("user principal class: null"));
		assertTrue(response.contains("user is admin: false"));

		// basic auth security for unprotected resource in unprotected context
		response = httpGET(port, "/c2/s/test", "Authorization: Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8)));
		assertTrue(response.contains("user principal: null"));
		assertTrue(response.contains("user principal class: null"));
		assertTrue(response.contains("user is admin: false"));
		// no auth for unprotected resource in unprotected context
		response = httpGET(port, "/c2/s/test");
		assertTrue(response.contains("user principal: null"));
		assertTrue(response.contains("user principal class: null"));
		assertTrue(response.contains("user is admin: false"));
		// basic auth security for second unprotected resource in unprotected context
		response = httpGET(port, "/c2/t/test", "Authorization: Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8)));
		assertTrue(response.contains("user principal: null"));
		assertTrue(response.contains("user principal class: null"));
		assertTrue(response.contains("user is admin: false"));
		// no auth for second unprotected resource in unprotected context
		response = httpGET(port, "/c2/t/test");
		assertTrue(response.contains("user principal: null"));
		assertTrue(response.contains("user principal class: null"));
		assertTrue(response.contains("user is admin: false"));

		getSecurityConfigurationCustomizer().removedService(secConfigRef, securityConfigurationModel);

		// no security, but user present
		response = httpGET(port, "/c1/s", "Authorization: Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8)));
		assertTrue(response.contains("user principal: null"));
		assertTrue(response.contains("user principal class: null"));
		assertTrue(response.contains("user is admin: false"));
		response = httpGET(port, "/c2/s", "Authorization: Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8)));
		assertTrue(response.contains("user principal: null"));
		assertTrue(response.contains("user principal class: null"));
		assertTrue(response.contains("user is admin: false"));
		// no security, no user
		response = httpGET(port, "/c1/s");
		assertTrue(response.contains("user principal: null"));
		assertTrue(response.contains("user principal class: null"));
		assertTrue(response.contains("user is admin: false"));
		response = httpGET(port, "/c2/s");
		assertTrue(response.contains("user principal: null"));
		assertTrue(response.contains("user principal class: null"));
		assertTrue(response.contains("user is admin: false"));
	}

	@Test
	public void loginConfigurationWithSecurityConstraints() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		WebContainer wc = new HttpServiceEnabled(sample1, controller, serverModel, null, config);

		wc.registerServlet("/test", new TestServlet("1"), null, null);

		// no security, but user present
		String response = httpGET(port, "/test", "Authorization: Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8)));
		assertTrue(response.contains("user principal: null"));
		assertTrue(response.contains("user principal class: null"));
		assertTrue(response.contains("user is admin: false"));
		// no security, no user
		response = httpGET(port, "/test");
		assertTrue(response.contains("user principal: null"));
		assertTrue(response.contains("user principal class: null"));
		assertTrue(response.contains("user is admin: false"));

		wc.registerLoginConfig("BASIC", "test", null, null, null);
		wc.registerConstraintMapping("security-1", null, "/test/*", null, true, Collections.singletonList("admin"), null);

		// without any security constraints, the realm will work, we'll get user principal from the request, but
		// there'll be no authorization (so no 401/403 without auth)

		// basic auth security
		response = httpGET(port, "/test", "Authorization: Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8)));
		if (runtime == Runtime.JETTY) {
			assertTrue(response.contains("user principal: admin"));
			assertTrue(response.contains("user principal class: org.eclipse.jetty.security.UserPrincipal"));
			assertTrue(response.contains("user is admin: true"));
		} else if (runtime == Runtime.TOMCAT) {
			assertTrue(response.contains("user principal: admin"));
			assertTrue(response.contains("user principal class: org.apache.catalina.realm.GenericPrincipal"));
			assertTrue(response.contains("user is admin: true"));
		} else if (runtime == Runtime.UNDERTOW) {
			assertTrue(response.contains("user principal: admin"));
			assertTrue(response.contains("user principal class: org.ops4j.pax.web.service.undertow.internal.security.PropertiesIdentityManager$SimplePrincipal"));
			assertTrue(response.contains("user is admin: true"));
		}
		// basic auth security, no user
		response = httpGET(port, "/test");
		assertTrue(response.startsWith("HTTP/1.1 401"));

		wc.unregisterConstraintMapping(null);
		wc.unregisterLoginConfig(null);

		// no security, but user present
		response = httpGET(port, "/test", "Authorization: Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8)));
		assertTrue(response.contains("user principal: null"));
		assertTrue(response.contains("user principal class: null"));
		assertTrue(response.contains("user is admin: false"));
		// no security, no user
		response = httpGET(port, "/test");
		assertTrue(response.contains("user principal: null"));
		assertTrue(response.contains("user principal class: null"));
		assertTrue(response.contains("user is admin: false"));

		((StoppableHttpService) wc).stop();

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(wc);

		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

	private static class TestServlet extends Utils.MyIdServlet {

		TestServlet(String id) {
			super(id);
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
			Principal up = req.getUserPrincipal();
			resp.getWriter().print(String.format("user principal: %s%n", up == null ? "null" : up.getName()));
			resp.getWriter().print(String.format("user principal class: %s%n", up == null ? "null" : up.getClass().getName()));
			resp.getWriter().print(String.format("user is admin: %b%n", req.isUserInRole("admin")));
		}
	}

}
