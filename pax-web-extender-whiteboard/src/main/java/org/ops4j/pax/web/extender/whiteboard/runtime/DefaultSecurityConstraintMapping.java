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
package org.ops4j.pax.web.extender.whiteboard.runtime;

import org.ops4j.pax.web.service.spi.model.elements.SecurityConstraintModel;
import org.ops4j.pax.web.service.whiteboard.SecurityConstraintMapping;

import jakarta.servlet.annotation.ServletSecurity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DefaultSecurityConstraintMapping extends AbstractContextRelated implements SecurityConstraintMapping {

	private final SecurityConstraintModel securityConstraint = new SecurityConstraintModel();
	private final List<WebResourceCollectionMapping> webResourceCollections = new ArrayList<>();

	@Override
	public String getName() {
		return securityConstraint.getName();
	}

	public void setName(String name) {
		securityConstraint.setName(name);
	}

	@Override
	public Collection<WebResourceCollectionMapping> getWebResourceCollections() {
		return webResourceCollections;
	}

	@Override
	public Collection<String> getAuthRoles() {
		return securityConstraint.getAuthRoles();
	}

	@Override
	public boolean isAuthRolesSet() {
		return securityConstraint.isAuthRolesSet();
	}

	public void setAuthRolesSet(boolean rolesSet) {
		securityConstraint.setAuthRolesSet(rolesSet);
	}

	@Override
	public ServletSecurity.TransportGuarantee getTransportGuarantee() {
		return securityConstraint.getTransportGuarantee();
	}

	public void setTransportGuarantee(ServletSecurity.TransportGuarantee transportGuarantee) {
		securityConstraint.setTransportGuarantee(transportGuarantee);
	}

	public SecurityConstraintModel getSecurityConstraint() {
		return securityConstraint;
	}

	public static class DefaultWebResourceCollectionMapping implements WebResourceCollectionMapping {

		private final SecurityConstraintModel.WebResourceCollection webResourceCollection = new SecurityConstraintModel.WebResourceCollection();

		@Override
		public String getName() {
			return webResourceCollection.getName();
		}

		public void setName(String name) {
			webResourceCollection.setName(name);
		}

		@Override
		public Collection<String> getUrlPatterns() {
			return webResourceCollection.getPatterns();
		}

		@Override
		public Collection<String> getHttpMethods() {
			return webResourceCollection.getMethods();
		}

		@Override
		public Collection<String> getHttpMethodOmissions() {
			return webResourceCollection.getOmittedMethods();
		}

		public SecurityConstraintModel.WebResourceCollection getWebResourceCollection() {
			return webResourceCollection;
		}

	}

}
