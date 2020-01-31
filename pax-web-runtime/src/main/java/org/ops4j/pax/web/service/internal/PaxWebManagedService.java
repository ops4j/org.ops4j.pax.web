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
package org.ops4j.pax.web.service.internal;

import java.util.Dictionary;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * {@link ManagedService} used by pax-web-runtime Activator, only when {@link org.osgi.service.cm.ConfigurationAdmin}
 * is available.
 */
public class PaxWebManagedService implements ManagedService {

	private ConfigurationUpdater updater;

	public PaxWebManagedService(ConfigurationUpdater updater) {
		this.updater = updater;
	}

	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		this.updater.updateConfiguration(properties);
	}

	/**
	 * Interface
	 */
	public interface ConfigurationUpdater {

		/**
		 * Decoupling from {@link ConfigurationAdmin}, so pax-web-runtime can use CM or work without it. This method
		 * is used to inform configuration mechanisms that properties in {@code org.ops4j.pax.web} PID has changed.
		 * @param properties
		 */
		void updateConfiguration(Dictionary<String, ?> properties);
	}

}
