/*
 * Copyright 2019 OPS4J.
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
package org.ops4j.pax.web.itest.osgi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.ops4j.pax.web.itest.AbstractControlledTestBase;
import org.osgi.framework.Constants;

import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * <p>Base class for all OSGi Pax Exam tests not related to any special container (Jetty, Tomcat or Undertow).</p>
 */
public abstract class AbstractOsgiTestBase extends AbstractControlledTestBase {

	protected Option[] baseConfigure() {
		// pax-web-itest-container-common private packages org.ops4j.pax.web.itest package, but here
		// there's no additional bundle, so we have to create it on-fly
		InputStream helper = TinyBundles.bundle()
				.set("Bundle-ManifestVersion", "2")
				.set("Export-Package", "org.ops4j.pax.web.itest")
				.set("DynamicImport-Package", "*")
				.add(AbstractControlledTestBase.class)
				.set("Bundle-SymbolicName", "infra")
				.build();
		File dir = new File("target/bundles");
		dir.mkdirs();
		String bundleURL = null;
		try {
			File bundle = new File(dir, "infra-bundle.jar");
			IOUtils.copy(helper, new FileOutputStream(bundle));
			bundleURL = bundle.toURI().toURL().toString();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		Option[] options = super.baseConfigure();
		options = combine(options, defaultLoggingConfig());
		// with PAXLOGGING-308 we can simply point to _native_ configuration file understood directly
		// by selected pax-logging backend
		options = combine(options, systemProperty("org.ops4j.pax.logging.property.file")
				.value("../etc/log4j2-osgi.properties"));

		return combine(options, CoreOptions.bundle(bundleURL));
	}

	/**
	 * Configuring symbolic name in test probe we can easily locate related log entries in the output.
	 * @param builder
	 * @return
	 */
	@ProbeBuilder
	public TestProbeBuilder probeBuilder(TestProbeBuilder builder) {
		builder.setHeader(Constants.BUNDLE_SYMBOLICNAME, PROBE_SYMBOLIC_NAME);
		return builder;
	}

}
