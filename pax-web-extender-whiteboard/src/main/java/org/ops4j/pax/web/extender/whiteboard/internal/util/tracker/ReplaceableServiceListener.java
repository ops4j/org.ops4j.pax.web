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
package org.ops4j.pax.web.extender.whiteboard.internal.util.tracker;

import java.util.Map;

/**
 * Listener for events related to replaceable service.
 */
public interface ReplaceableServiceListener<T> {

	/**
	 * Called when the backing service gets changed.
	 *
	 * @param oldService old service or null if there was no service
	 * @param newService new service or null if there is no new service
	 * @param serviceProperties of the new serivce or null if no properties have been requested in ReplaceableService 
	 */
	void serviceChanged(T oldService, T newService, Map<String, Object> serviceProperties);

}
