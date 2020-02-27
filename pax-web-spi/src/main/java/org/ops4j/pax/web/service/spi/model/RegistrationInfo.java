/*
 * Copyright 2020 OPS4J.
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
package org.ops4j.pax.web.service.spi.model;

import org.osgi.framework.Bundle;

/**
 * All registration details describing the registered element - whether it's servlet, filter or something else.
 */
public class RegistrationInfo {

	/**
	 * A bundle for which the registration was made. It's a bundle for which the
	 * {@link org.osgi.service.http.HttpService} is scoped or which context was used to register Whiteboard
	 * service.
	 */
	private Bundle registeringBundle;

	public Bundle getRegisteringBundle() {
		return registeringBundle;
	}

	public void setRegisteringBundle(Bundle registeringBundle) {
		this.registeringBundle = registeringBundle;
	}

}
