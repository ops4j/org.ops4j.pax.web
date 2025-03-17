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
package org.ops4j.pax.web.itest.container;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.web.itest.AbstractControlledTestBase;

import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * <p>Single base class for all container-related tests.</p>
 */
public abstract class AbstractContainerTestBase extends AbstractControlledTestBase {

	protected Option[] baseConfigure() {
		Option[] options = baseConfigureWithoutRuntime();
		options = combine(options, paxWebRuntime());

		return options;
	}

	protected Option[] baseConfigureWithoutRuntime() {
		Option[] options = super.baseConfigure();
		Option[] containerOptions = new Option[] {
				frameworkProperty("org.osgi.service.http.port").value("8181")
		};
		options = combine(options, containerOptions);
		options = combine(options, defaultLoggingConfig());
		// with PAXLOGGING-308 we can simply point to _native_ configuration file understood directly
		// by selected pax-logging backend
		options = combine(options, systemProperty("org.ops4j.pax.logging.property.file")
				.value("../../etc/log4j2-osgi.properties"));

		options = combine(options, paxWebCore());
		options = combine(options, paxWebTestSupport());

		return options;
	}

}
