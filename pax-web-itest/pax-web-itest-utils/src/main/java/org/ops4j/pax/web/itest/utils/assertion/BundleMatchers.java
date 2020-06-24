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
package org.ops4j.pax.web.itest.utils.assertion;

import java.util.Arrays;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class BundleMatchers {

	private BundleMatchers() {
	}

	public static void isBundleActive(String bundleSymbolicName, BundleContext bundleContext) {
		Assert.assertThat(String.format("Bundle '%s' must be active", bundleSymbolicName),
				Arrays.stream(bundleContext.getBundles()),
				bundles ->
						bundles.filter(
								bundle -> bundle.getState() == Bundle.ACTIVE
										&& bundle.getSymbolicName().equals(bundleSymbolicName)).count() == 1);
	}

}
