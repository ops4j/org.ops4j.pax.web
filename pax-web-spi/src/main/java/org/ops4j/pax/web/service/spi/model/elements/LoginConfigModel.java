/*
 * Copyright 2010 Achim Nierbeck
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

import java.util.List;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWebContainerView;

/**
 * <p>Set of parameters for login configuration parameters, referenced from {@link OsgiContextModel}.</p>
 *
 * <p>This model reflects {@code <login-config>} element from {@code web.xml}.</p>
 */
public class LoginConfigModel extends ElementModel {

	/** {@code <login-config>/<auth-method>} */
	private String authMethod;

	/** {@code <login-config>/<realm-name>} */
	private String realmName;

	/** {@code <login-config>/<form-login-config>/<form-login-page>} */
	private String formLoginPage;

	/** {@code <login-config>/<form-login-config>/<form-error-page>} */
	private String formErrorPage;

	LoginConfigModel(List<OsgiContextModel> contextModels) {
//		super(contextModels);
	}

	@Override
	public void register(WhiteboardWebContainerView view) {
	}

	@Override
	public void unregister(WhiteboardWebContainerView view) {
	}

	public String getAuthMethod() {
		return authMethod;
	}

	public void setAuthMethod(String authMethod) {
		this.authMethod = authMethod;
	}

	public String getRealmName() {
		return realmName;
	}

	public void setRealmName(String realmName) {
		this.realmName = realmName;
	}

	public String getFormLoginPage() {
		return formLoginPage;
	}

	public void setFormLoginPage(String formLoginPage) {
		this.formLoginPage = formLoginPage;
	}

	public String getFormErrorPage() {
		return formErrorPage;
	}

	public void setFormErrorPage(String formErrorPage) {
		this.formErrorPage = formErrorPage;
	}
	@Override
	public Boolean performValidation() {
		return Boolean.TRUE;
	}

}
