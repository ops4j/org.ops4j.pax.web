/*
 * Copyright 2022 OPS4J.
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
package org.ops4j.pax.web.service.spi.task;

import java.util.LinkedList;
import java.util.List;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.LoginConfigModel;
import org.ops4j.pax.web.service.spi.model.elements.SecurityConstraintModel;

/**
 * Change of login config and security constraints.
 */
public class SecurityConfigChange extends Change {

	private final OsgiContextModel osgiContextModel;
	private final LoginConfigModel loginConfigModel;
	private final List<SecurityConstraintModel> securityConstraints = new LinkedList<>();
	private final List<String> securityRoles = new LinkedList<>();

	public SecurityConfigChange(OpCode op, OsgiContextModel osgiContextModel, LoginConfigModel loginConfigModel,
			List<SecurityConstraintModel> securityConstraints, List<String> securityRoles) {
		super(op);
		this.osgiContextModel = osgiContextModel;
		this.loginConfigModel = loginConfigModel;
		this.securityConstraints.addAll(securityConstraints);
		this.securityRoles.addAll(securityRoles);
	}

	@Override
	public void accept(BatchVisitor visitor) {
		visitor.visitSecurityConfigChange(this);
	}

	public OsgiContextModel getOsgiContextModel() {
		return osgiContextModel;
	}

	public LoginConfigModel getLoginConfigModel() {
		return loginConfigModel;
	}

	public List<SecurityConstraintModel> getSecurityConstraints() {
		return securityConstraints;
	}

	public List<String> getSecurityRoles() {
		return securityRoles;
	}

}
