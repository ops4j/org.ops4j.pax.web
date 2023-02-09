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

import org.ops4j.pax.web.service.spi.model.elements.LoginConfigModel;
import org.ops4j.pax.web.service.whiteboard.SecurityConfigurationMapping;
import org.ops4j.pax.web.service.whiteboard.SecurityConstraintMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DefaultSecurityConfigurationMapping extends AbstractContextRelated implements SecurityConfigurationMapping {

	private final LoginConfigModel loginConfig = new LoginConfigModel();
	private final List<SecurityConstraintMapping> securityConstraints = new ArrayList<>();
	private final Set<String> securityRoles = new LinkedHashSet<>();

	@Override
	public String getAuthMethod() {
		return loginConfig.getAuthMethod();
	}

	public void setAuthMethod(String method) {
		loginConfig.setAuthMethod(method);
	}

	@Override
	public String getRealmName() {
		return loginConfig.getRealmName();
	}

	public void setRealmName(String realmName) {
		loginConfig.setRealmName(realmName);
	}

	@Override
	public String getFormLoginPage() {
		return loginConfig.getFormLoginPage();
	}

	public void setFormLoginPage(String formLoginPage) {
		loginConfig.setFormLoginPage(formLoginPage);
	}

	@Override
	public String getFormErrorPage() {
		return loginConfig.getFormErrorPage();
	}

	public void setFormErrorPage(String formErrorPage) {
		loginConfig.setFormErrorPage(formErrorPage);
	}

	@Override
	public Collection<SecurityConstraintMapping> getSecurityConstraints() {
		return securityConstraints;
	}

	@Override
	public Collection<String> getSecurityRoles() {
		return securityRoles;
	}

	public LoginConfigModel getLoginConfig() {
		return loginConfig;
	}

}
