/* Copyright 2016 Marc Schlegel
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
package org.ops4j.pax.web.resources.api.query;

import org.ops4j.pax.web.resources.api.ResourceInfo;

/**
 * Implementations are used together with a {@link ResourceQueryMatcher}. The result needs one callback to store the
 * actual {@link ResourceInfo} that matched the query, but additional information may be added for later processing.
 */
public interface ResourceQueryResult {

	/**
	 * Callback to enhance the external result with the actuall matched resource
	 *
	 * @param resourceInfo descriptor to the matched resource
	 */
	void addMatchedResourceInfo(ResourceInfo resourceInfo);

}
