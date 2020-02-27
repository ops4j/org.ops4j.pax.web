/* Copyright 2012 Harald Wellmann
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
package org.ops4j.pax.web.service;

import javax.servlet.ServletContainerInitializer;

import org.ops4j.pax.web.annotations.Review;
import org.osgi.service.http.HttpService;

/**
 * Encapsulates dependencies for a web application. A service implementing this
 * interface is registered with a property {@code bundle.id} set to the web
 * application bundle ID when the dependencies for the given application are
 * satisfied.
 *
 * @author Harald Wellmann
 */
@Review("Should it be part of API?" +
		"Maybe together with AuthenticatorService is should be part of some \"extension\" package?")
public interface WebAppDependencyHolder {

	/**
	 * Returns the HTTP service to be used by the given application. Required.
	 *
	 * @return HTTP service, never null
	 */
	HttpService getHttpService();

	/**
	 * Returns a servlet container initializer to be added to the given
	 * application. Optional. Extensions may use this initializer to customize
	 * the servlet context.
	 *
	 * @return servlet container initializer, or null.
	 */
	ServletContainerInitializer getServletContainerInitializer();
}
