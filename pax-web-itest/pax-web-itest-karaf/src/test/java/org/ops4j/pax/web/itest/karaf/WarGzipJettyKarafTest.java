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
package org.ops4j.pax.web.itest.karaf;

import java.io.File;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFilePutOption;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileReplacementOption;

import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author Grzegorz Grzybek
 */
public class WarGzipJettyKarafTest extends WarBaseKarafTest {

	@Configuration
	public Option[] configuration() {
		Option[] basicOptions = combine(jettyConfig(), jspConfig());
		KarafDistributionConfigurationFileReplacementOption config
				= new KarafDistributionConfigurationFileReplacementOption("etc/jetty.xml",
				new File("target/test-classes/etc/gzip-jetty.xml"));
		KarafDistributionConfigurationFilePutOption pid1
				= new KarafDistributionConfigurationFilePutOption("etc/org.ops4j.pax.web.cfg",
				"org.ops4j.pax.web.config.file", "etc/jetty.xml");
		KarafDistributionConfigurationFilePutOption pid2
				= new KarafDistributionConfigurationFilePutOption("etc/org.ops4j.pax.web.cfg",
				"org.osgi.service.http.enabled", "true");
		KarafDistributionConfigurationFilePutOption pid3
				= new KarafDistributionConfigurationFilePutOption("etc/org.ops4j.pax.web.cfg",
				"org.osgi.service.http.port", "8181");
		KarafDistributionConfigurationFilePutOption pid4
				= new KarafDistributionConfigurationFilePutOption("etc/org.ops4j.pax.web.cfg",
				"org.ops4j.pax.web.log.ncsa.enabled", "true");
		KarafDistributionConfigurationFilePutOption pid5
				= new KarafDistributionConfigurationFilePutOption("etc/org.ops4j.pax.web.cfg",
				"org.ops4j.pax.web.log.ncsa.directory", "data/log");
		return combine(basicOptions, config, pid1, pid2, pid3, pid4, pid5);
	}

	@Override
	protected boolean gzipEncodingEnabled() {
		return true;
	}

}
