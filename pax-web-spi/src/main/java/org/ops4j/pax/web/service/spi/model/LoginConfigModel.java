/* Copyright 2010 Achim Nierbeck
 *
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
package org.ops4j.pax.web.service.spi.model;

import org.ops4j.lang.NullArgumentException;

public class LoginConfigModel extends Model {

	private final String realmName;
	private final String authMethod;

	public LoginConfigModel(ContextModel contextModel, String authMethod,
			String realmName) {
		super(contextModel);
		NullArgumentException.validateNotEmpty(authMethod, "authMethod");
		NullArgumentException.validateNotEmpty(realmName, "realmName");
		this.authMethod = authMethod;
		this.realmName = realmName;
	}

	/**
	 * @return the realmName
	 */
	public String getRealmName() {
		return realmName;
	}

	/**
	 * @return the authMethod
	 */
	public String getAuthMethod() {
		return authMethod;
	}

}
