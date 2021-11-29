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
package org.ops4j.pax.web.service.spi.model.views;

import java.util.Set;

import org.ops4j.pax.web.service.spi.model.info.ServletInfo;
import org.ops4j.pax.web.service.spi.model.info.WebApplicationInfo;

/**
 * <p>An interfaces that should be implemented by bundles that want to create/alter/enhance the information returned by
 * {@link ReportWebContainerView}.</p>
 *
 * <p>This interface was created to let pax-web-extender-war provide more information about failed WAB installation.</p>
 */
public interface ReportViewPlugin {

	/**
	 * This method is called to fill a list of {@link WebApplicationInfo web application models} for reporting
	 * purpose. It is the responsibility of the implementation to check (by some kind of key) if the
	 * {@link WebApplicationInfo} is already in the passed set - in this case, the existing model can be updated
	 * instead.
	 *
	 * @param webapps
	 */
	void collectWebApplications(Set<WebApplicationInfo> webapps);

	/**
	 * Returns information about single WAB (by its context path) - only successully deployed WABs should be returned,
	 * because there may be more failed contexts too. For information about failed WABs, use the method with bundle ID.
	 *
	 * @param contextPath
	 * @return
	 */
	WebApplicationInfo getWebApplication(String contextPath);

	/**
	 * Returns information about single WAB (by its bundle id).
	 * @param bundleId
	 * @return
	 */
	WebApplicationInfo getWebApplication(long bundleId);

	/**
	 * Fill information about available servlets
	 *
	 * @param servlets
	 */
	default void collectServlets(Set<ServletInfo> servlets) {}

}
