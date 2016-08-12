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
package org.ops4j.pax.web.extender.war.internal.model;

public class WebAppTagLib {

	private String tagLibLocation;
	private String tagLibUri;

	public void addTagLibLocation(String tagLibLocation) {
		this.tagLibLocation = tagLibLocation;
	}

	public String getTagLibLocation() {
		return tagLibLocation;
	}

	public void addTagLibUri(String tagLibUri) {
		this.tagLibUri = tagLibUri;
	}

	public String getTagLibUri() {
		return tagLibUri;
	}

}
