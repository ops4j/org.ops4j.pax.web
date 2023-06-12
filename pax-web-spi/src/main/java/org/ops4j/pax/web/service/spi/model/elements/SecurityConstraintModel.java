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
package org.ops4j.pax.web.service.spi.model.elements;

import jakarta.servlet.ServletSecurityElement;
import jakarta.servlet.annotation.ServletSecurity;
import java.util.ArrayList;
import java.util.List;

/**
 * Passive web element representing {@code <security-constraint>} element of {@code web.xml}. See equivalent
 * {@code org.apache.tomcat.util.descriptor.web.SecurityConstraint} and
 * {@code org.apache.tomcat.util.descriptor.web.SecurityCollection} classes from Tomcat.
 */
public class SecurityConstraintModel {

	/** {@code <display-name>} - even if XSD allows more such elements */
	private String name;

	/** {@code <web-resource-collection>} - should be at least one */
	private final List<WebResourceCollection> webResourceCollections = new ArrayList<>();

	/** {@code <auth-constraint>/<role-name>} elements */
	private final List<String> authRoles = new ArrayList<>();

	/** Flag distinguishing no auth roles ({$code true} - deny all) and auth roles not set at all ({$code false} - allow all) */
	private boolean authRolesSet = true;

	/** {@code <transport-guarantee><user-data-constraint>}. INTEGRAL and CONFIDENTIAL are in practice equivalent */
	private ServletSecurity.TransportGuarantee transportGuarantee = ServletSecurity.TransportGuarantee.NONE;

	/**
	 * If the security constraint model was created from Servlet-specific configuration like:<ul>
	 *     <li>{@link jakarta.servlet.ServletRegistration.Dynamic#setServletSecurity(ServletSecurityElement)}, or</li>
	 *     <li>{@link ServletSecurity} annotation</li>
	 * </ul>we remember the associated {@link ServletModel}, so dynamic security constraints are properly applied.
	 */
	private ServletModel servletModel;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<WebResourceCollection> getWebResourceCollections() {
		return webResourceCollections;
	}

	public List<String> getAuthRoles() {
		return authRoles;
	}

	public boolean isAuthRolesSet() {
		return authRolesSet;
	}

	public void setAuthRolesSet(boolean authRolesSet) {
		this.authRolesSet = authRolesSet;
	}

	public ServletSecurity.TransportGuarantee getTransportGuarantee() {
		return transportGuarantee;
	}

	public void setTransportGuarantee(ServletSecurity.TransportGuarantee transportGuarantee) {
		this.transportGuarantee = transportGuarantee;
	}

	public void setServletModel(ServletModel servletModel) {
		this.servletModel = servletModel;
	}

	public ServletModel getServletModel() {
		return servletModel;
	}

	/**
	 * A collection of constrained collection of URIs - {@code <web-resource-collection>} elements from {@code web.xml}
	 */
	public static class WebResourceCollection {

		/** {@code <web-resource-name>} */
		private String name;

		/** {@code <url-pattern>} */
		private final List<String> patterns = new ArrayList<>();

		/** {@code <http-method>} */
		private final List<String> methods = new ArrayList<>();

		/** {@code <http-method-omission>} */
		private final List<String> omittedMethods = new ArrayList<>();

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<String> getPatterns() {
			return patterns;
		}

		public List<String> getMethods() {
			return methods;
		}

		public List<String> getOmittedMethods() {
			return omittedMethods;
		}
	}

}
