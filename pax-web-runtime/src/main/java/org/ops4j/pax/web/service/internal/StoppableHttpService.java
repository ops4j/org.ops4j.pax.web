/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.internal;

import org.ops4j.pax.web.service.WebContainer;

/**
 * Interface to be implemented by these {@link WebContainer} implementations that can be stopped.
 * This interface shuold not be part of {@code objectClass} property of OSGi service, because we don't want
 * users to stop the service.
 */
public interface StoppableHttpService {

	/**
	 * Stop the {@link org.osgi.service.http.HttpService}. It doesn't directly mean <em>stop the underlying
	 * HTTP server</em>, it's more like marking a {@link org.osgi.service.http.HttpService} as no longer open
	 * for registration of web elements.
	 */
	void stop();

}
