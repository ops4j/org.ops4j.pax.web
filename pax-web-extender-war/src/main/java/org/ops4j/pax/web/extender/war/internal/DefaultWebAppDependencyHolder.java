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
package org.ops4j.pax.web.extender.war.internal;

import org.osgi.service.http.HttpService;

/**
 * A {@link WebAppDependencyHolder} for web applications which only depend on an
 * HTTP service and not on external customizers.
 *
 * @author Harald Wellmann
 */
public class DefaultWebAppDependencyHolder {

	private HttpService httpService;

	public DefaultWebAppDependencyHolder(HttpService httpService) {
		this.httpService = httpService;
	}

//	@Override
//	public HttpService getHttpService() {
//		return httpService;
//	}
//
//	@Override
//	public ServletContainerInitializer getServletContainerInitializer() {
//		return null;
//	}
}
