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
package org.ops4j.pax.web.jsf.resourcehandler.internal;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.ops4j.pax.web.resources.api.ResourceInfo;
import org.osgi.framework.Bundle;

class OsgiResourceMatcher extends TypeSafeMatcher<ResourceInfo> {

	private final long bundleId;
	private final String resourcepath;

	private OsgiResourceMatcher(Bundle bundle, String resourcepath) {
		if (bundle == null || resourcepath == null) {
			throw new IllegalArgumentException("OsgiResourceMatcher: all values must be set!");
		}
		this.bundleId = bundle.getBundleId();
		if (resourcepath.charAt(0) == '/') {
			this.resourcepath = resourcepath.substring(1);
		} else {
			this.resourcepath = resourcepath;
		}
	}

	@Override
	public void describeTo(Description description) {
		description
				.appendText("expected result from getUrl(): ")
				.appendValue("file://" + bundleId + ".0:0/META-INF/resources/" + resourcepath);
	}

	@Override
	protected void describeMismatchSafely(ResourceInfo item, Description mismatchDescription) {
		mismatchDescription
				.appendText("was ")
				.appendValue(item.getUrl());
	}

	@Override
	protected boolean matchesSafely(ResourceInfo item) {
		return item.getUrl().toString().equals("file://" + bundleId + ".0:0/META-INF/resources/" + resourcepath);
	}

	static OsgiResourceMatcher isBundleResource(Bundle bundle, String resourcepath) {
		return new OsgiResourceMatcher(bundle, resourcepath);
	}

}
