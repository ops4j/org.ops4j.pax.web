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

import java.util.Collection;

/**
 * <p>Single interface to register 3 security aspects of {@code web.xml} configuration:<ul>
 *     <li>{@code <login-config>}</li>
 *     <li>{@code <security-constraint>}</li>
 *     <li>{@code <security-role>}</li>
 * </ul></p>
 *
 * <p>This <em>mapping</em> was created as the last one for the purpose of
 * <a href="https://github.com/ops4j/org.ops4j.pax.web/issues/1823">Pax Web #1823 issue</a>.</p>
 *
 * <p>In Pax Web 7 we had two methods (which are back in Pax Web 8, but only for compatibility purpose):<ul>
 *     <li>org.ops4j.pax.web.service.WebContainer#registerLoginConfig()</li>
 *     <li>org.ops4j.pax.web.service.WebContainer#registerConstraintMapping()</li>
 * </ul>
 * However these were not atomic (or rather as atomic as it's rational), because both methods leads to restart
 * of the underlying context. When Whiteboard-registering a service with this interface, we can do everything
 * within single context restart.</p>
 *
 * @author Grzegorz Grzybek
 */
public interface SecurityConfigurationMapping extends ContextRelated {

	// --- the <login-config> part

	/**
	 * Returns {@code <login-config>/<auth-method>}
	 * @return
	 */
	String getAuthMethod();

	/**
	 * Returns {@code <login-config>/<realm-name>}
	 * @return
	 */
	String getRealmName();

	/**
	 * Returns {@code <login-config>/<form-login-config>/<form-login-page>}
	 * @return
	 */
	String getFormLoginPage();

	/**
	 * Returns {@code <login-config>/<form-login-config>/<form-error-page>}
	 * @return
	 */
	String getFormErrorPage();

	// --- the <security-constraint> part

	/**
	 * Returns a collection of {@code <security-constraint>} information.
	 * @return
	 */
	Collection<SecurityConstraintMapping> getSecurityConstraints();

	// --- the <security-role> part

	/**
	 * Returns declared security roles ({@code <security-role>/<role-name>})
	 * @return
	 */
	Collection<String> getSecurityRoles();

}
