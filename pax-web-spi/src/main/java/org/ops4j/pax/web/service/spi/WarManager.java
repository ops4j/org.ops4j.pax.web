/*
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
package org.ops4j.pax.web.service.spi;

import org.ops4j.pax.web.annotations.Review;

/**
 * Used to manage deployments of WARs discovered by OPS4J Pax Web - Extender -
 * WAR.
 *
 * @author Hiram Chirino
 */
@Review("Extend and integrate with org.osgi.service.http.runtime.HttpServiceRuntime for Karaf commands/services"
		+ " like org.apache.karaf.web.WebContainerService and org.apache.karaf.http.core.ServletService")
public interface WarManager {

	int SUCCESS = 0;
	int WAR_NOT_FOUND = 2;
	int ALREADY_STARTED = 3;
	int ALREADY_STOPPED = 4;

	/**
	 * Starts a war bundle under an optional configurable content name.
	 *
	 * @param bundleId    The bundle id that contains the war.
	 * @param contextName an optional context name to host the war under, if null it
	 *                    will use the context name configured in the war OSGi metadata.
	 * @return {@link #SUCCESS} if the war was started, or
	 * {@link #WAR_NOT_FOUND} if the bundle is not a war bundle, or
	 * {@link #ALREADY_STARTED} if the war had already been started.
	 */
	int start(long bundleId, String contextName);

	/**
	 * Stops a war bundle.
	 *
	 * @param bundleId The bundle id that contains the war.
	 * @return result of stop operation
	 */
	int stop(long bundleId);

}
