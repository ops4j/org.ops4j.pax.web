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
package org.ops4j.pax.web.service.whiteboard;

import jakarta.servlet.annotation.ServletSecurity;
import java.util.Collection;

/**
 * <p>Single constraint mapping to be used by {@link SecurityConfigurationMapping}. It represents single
 * {@code <security-constraint>} element from {@code web.xml}.</p>
 *
 * @author Grzegorz Grzybek
 */
public interface SecurityConstraintMapping extends ContextRelated {

	/**
	 * Returns {@code <security-constraint>/<display-name>}
	 * @return
	 */
	String getName();

	/**
	 * Returns a collection of {@code <security-constraint>/<web-resource-collection>} information.
	 * @return
	 */
	Collection<WebResourceCollectionMapping> getWebResourceCollections();

	/**
	 * Returns a collection of {@code <security-constraint>/<auth-constraint>/<role-name>}
	 * @return
	 */
	Collection<String> getAuthRoles();

	/**
	 * When returning {@code true}, it means that empty roles collection mean "deny all authentication attempts".
	 * {@code false} means that authentication is successful despite the associated user roles. Effectively this flag
	 * disctinguishes between empty roles set ({@code true}) and no roles set at all ({@code false}).
	 * @return
	 */
	default boolean isAuthRolesSet() {
		return true;
	}

	/**
	 * Returns {@code <security-constraint>/<user-data-constraint>/<transport-guarantee>}.
	 * @return
	 */
	default ServletSecurity.TransportGuarantee getTransportGuarantee() {
		return ServletSecurity.TransportGuarantee.NONE;
	}

	/**
	 * A representation of single {@code <security-constraint>/<web-resource-collection>}
	 */
	interface WebResourceCollectionMapping {

		/**
		 * Returns {@code <security-constraint>/<web-resource-collection>/<web-resource-name>}.
		 * @return
		 */
		String getName();

		/**
		 * Returns {@code <security-constraint>/<web-resource-collection>/<url-pattern>} collection.
		 * @return
		 */
		Collection<String> getUrlPatterns();

		/**
		 * Returns {@code <security-constraint>/<web-resource-collection>/<http-method>} collection.
		 * @return
		 */
		Collection<String> getHttpMethods();

		/**
		 * Returns {@code <security-constraint>/<web-resource-collection>/<http-method-omission>} collection.
		 * @return
		 */
		Collection<String> getHttpMethodOmissions();

	}

}
