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
package org.ops4j.pax.web.resources.api;

import java.net.URL;
import java.time.LocalDateTime;

/**
 * Resource representation with an {@link URL} (usually with {@code bundle:} scheme or similar).
 */
public class ResourceInfo {

	private final URL url;
	private final LocalDateTime lastModified;
	private final long bundleId;

	public ResourceInfo(URL url, LocalDateTime lastModified, long bundleId) {
		this.url = url;
		this.lastModified = lastModified;
		this.bundleId = bundleId;
	}

	public URL getUrl() {
		return url;
	}

	public LocalDateTime getLastModified() {
		return lastModified;
	}

	public long getBundleId() {
		return bundleId;
	}

}
