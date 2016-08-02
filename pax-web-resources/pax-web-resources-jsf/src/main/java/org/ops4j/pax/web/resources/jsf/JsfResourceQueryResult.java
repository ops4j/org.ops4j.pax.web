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
package org.ops4j.pax.web.resources.jsf;

import org.ops4j.pax.web.resources.api.ResourceInfo;
import org.ops4j.pax.web.resources.api.query.ResourceQueryResult;

public class JsfResourceQueryResult implements ResourceQueryResult {

	private boolean matchedLocalePrefix;
	private boolean matchedLibraryName;
	private String libraryVersion;
	private String resourceVersion;
	private ResourceInfo resourceInformation;


	public JsfResourceQueryResult(boolean matchedLocalePrefix, boolean matchedLibraryName, String libraryVersion, String resourceVersion) {
		this.matchedLocalePrefix = matchedLocalePrefix;
		this.matchedLibraryName = matchedLibraryName;
		this.libraryVersion = libraryVersion;
		this.resourceVersion = resourceVersion;
	}


	@Override
	public void addMatchedResourceInfo(ResourceInfo resourceInfo) {
		this.resourceInformation = resourceInfo;
	}


	public boolean isMatchedLocalePrefix() {
		return matchedLocalePrefix;
	}

	public boolean isMatchedLibraryName() {
		return matchedLibraryName;
	}


	public String getLibraryVersion() {
		return libraryVersion;
	}


	public String getResourceVersion() {
		return resourceVersion;
	}


	public ResourceInfo getResourceInformation() {
		return resourceInformation;
	}

}
