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

import org.ops4j.pax.web.service.spi.model.events.SecurityConfigurationEventData;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWebContainerView;
import org.ops4j.pax.web.service.whiteboard.SecurityConfigurationMapping;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>Single configuration class for 3 top-level elements of {@code web.xml}:<ul>
 *     <li>{@code <login-config>} (1 per context):<ul>
 *         <li>{@code org.eclipse.jetty.security.SecurityHandler#setRealmName()} + few others</li>
 *         <li>{@code org.apache.catalina.Context#setLoginConfig()}</li>
 *         <li>{@code io.undertow.servlet.api.DeploymentInfo#setLoginConfig()}</li>
 *     </ul></li>
 *     <li>{@code <security-constraint>} (N per context):<ul>
 *         <li>{@code org.eclipse.jetty.security.ConstraintAware#addConstraintMapping()}</li>
 *         <li>{@code org.apache.catalina.Context#addConstraint()}</li>
 *         <li>{@code io.undertow.servlet.api.DeploymentInfo#addSecurityConstraint()}</li>
 *     </ul></li>
 *     <li>{@code <security-role>} (N per context):<ul>
 *         <li>{@code org.eclipse.jetty.security.ConstraintSecurityHandler#setRoles()}</li>
 *         <li>{@code org.apache.catalina.Context#addSecurityRole()}</li>
 *         <li>{@code io.undertow.servlet.api.DeploymentInfo#addSecurityRole()}</li>
 *     </ul></li>
 * </ul></p>
 *
 * <p>Additionally, security declarations may be passed through {@link javax.servlet.ServletRegistration.Dynamic#setServletSecurity}
 * and for example, Tomcat passes the arguments to {@code org.apache.catalina.core.StandardContext#addServletSecurity()}.</p>
 */
public class SecurityConfigurationModel extends ElementModel<SecurityConfigurationMapping, SecurityConfigurationEventData> {

	private LoginConfigModel loginConfig = null;
	private final List<SecurityConstraintModel> securityConstraints = new ArrayList<>();
	private final Set<String> securityRoles = new LinkedHashSet<>();

	/**
	 * Returns a single, context-wide login configuration matching {@code <login-config>} element
	 * from {@code web.xml}.
	 * @return
	 */
	public LoginConfigModel getLoginConfig() {
		return loginConfig;
	}

	public void setLoginConfig(LoginConfigModel loginConfig) {
		this.loginConfig = loginConfig;
	}

	/**
	 * Returns a list of constraints matching the {@code <security-constraint>} elements from {@code web.xml}.
	 * @return
	 */
	public List<SecurityConstraintModel> getSecurityConstraints() {
		return securityConstraints;
	}

	/**
	 * Returns a list of roles matching the {@code <security-role>/<role-name>} elements from {@code web.xml}.
	 * @return
	 */
	public Set<String> getSecurityRoles() {
		return securityRoles;
	}

	@Override
	public void register(WhiteboardWebContainerView view) {
		view.registerSecurityConfiguration(this);
	}

	@Override
	public void unregister(WhiteboardWebContainerView view) {
		view.unregisterSecurityConfiguration(this);
	}

	@Override
	public String toString() {
		return "SecurityConfigurationModel{id=" + getId()
				+ ",authMethod='" + (loginConfig == null ? "<not set>" : loginConfig.getAuthMethod()) + "'"
				+ ",realmName='" + (loginConfig == null ? "<not set>" : loginConfig.getRealmName()) + "'"
				+ ",constraint count=" + securityConstraints.size()
				+ ",contexts=" + contextModels
				+ "}";
	}

	@Override
	public Boolean performValidation() throws Exception {
		return Boolean.TRUE;
	}

	@Override
	public SecurityConfigurationEventData asEventData() {
		return new SecurityConfigurationEventData(loginConfig.getAuthMethod(), loginConfig.getRealmName());
	}

}
