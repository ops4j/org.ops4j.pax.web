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

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

import java.io.File;

import javax.inject.Inject;

import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.karaf.options.configs.CustomProperties;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.web.itest.base.AbstractControlledTestBase;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClient;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KarafBaseTest extends AbstractControlledTestBase {

	private static final Logger LOG = LoggerFactory.getLogger(KarafBaseTest.class);
	public static final String RMI_SERVER_PORT = "44445";
	public static final String RMI_REG_PORT = "1100";
	protected static final String COVERAGE_COMMAND = "coverage.command";

	@Inject
	protected FeaturesService featuresService;

	@Inject
	protected BundleContext bundleContext;

	/**
	 * To make sure the tests run only when the boot features are fully
	 * installed
	 */
	@Inject
	BootFinished bootFinished;

	private org.ops4j.pax.web.itest.base.WebListenerImpl webListener;

	private org.ops4j.pax.web.itest.base.ServletListenerImpl servletListener;

	public Option[] baseConfig() {

		MavenUrlReference karafStandardFeature = maven()
				.groupId("org.apache.karaf.features").artifactId("standard")
				.type("xml").classifier("features").version(getKarafVersion());

		return new Option[]{
				karafDistributionConfiguration().frameworkUrl(mvnKarafDist())
						.unpackDirectory(new File("target/paxexam/unpack/"))
						.useDeployFolder(false).runEmbedded(false)/*.runEmbedded(true), //only for debugging*/,

//				KarafDistributionOption.debugConfiguration("5005", true),
				configureConsole().ignoreLocalConsole(),
				when(isEquinox()).useOptions(
						editConfigurationFilePut(CustomProperties.KARAF_FRAMEWORK, "equinox"),
						systemProperty("pax.exam.framework").value(System.getProperty("pax.exam.framework")),
						systemProperty("osgi.console").value("6666"),
						systemProperty("osgi.console.enable.builtin").value("true")
				),
				logLevel(LogLevel.WARN),
				keepRuntimeFolder(),
				when(isKaraf4()).useOptions(
						features(karafStandardFeature, "wrap")
				),
				editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", RMI_REG_PORT),
				editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", RMI_SERVER_PORT),
				editConfigurationFilePut("etc/config.properties", "org.osgi.framework.system.capabilities", " ${eecap-${java.specification.version}}, \n" +
						" osgi.service;effective:=active;objectClass=org.osgi.service.packageadmin.PackageAdmin, \n" +
						" osgi.service;effective:=active;objectClass=org.osgi.service.resolver.Resolver, \n" +
						" osgi.service;effective:=active;objectClass=org.osgi.service.startlevel.StartLevel, \n" +
						" osgi.service;effective:=active;objectClass=org.osgi.service.url.URLHandlers"),

				editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg", "log4j2.pattern", "%d{HH:mm:ss.SSS} %-5level {%thread} [%C] (%F:%L) : %msg%n"),
				editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg", "log4j2.logger.paxweb.name", "org.ops4j.pax.web"),
				editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg", "log4j2.logger.paxweb.level", "trace"),

				KarafDistributionOption.replaceConfigurationFile("etc/keystore", new File(getClass().getClassLoader().getResource("keystore").getFile())),
				KarafDistributionOption.replaceConfigurationFile("/etc/jetty.xml", new File(getClass().getClassLoader().getResource("jetty.xml").getFile())),
				systemProperty("ProjectVersion").value(
						VersionUtil.getProjectVersion()),
				addCodeCoverageOption(),

				mavenBundle().groupId("org.ops4j.pax.web.itest")
						.artifactId("pax-web-itest-base").versionAsInProject(),
				//new ExamBundlesStartLevel(4),
				mavenBundle().groupId("commons-collections")
						.artifactId("commons-collections")
						.version(asInProject()),
				mavenBundle()
						.groupId("org.apache.servicemix.bundles")
						.artifactId(
								"org.apache.servicemix.bundles.commons-digester")
						.version("1.8_4"),
				mavenBundle().groupId("org.apache.geronimo.bundles")
						.artifactId("commons-discovery").version("0.4_1"),
				mavenBundle()
						.groupId("org.apache.servicemix.specs")
						.artifactId(
								"org.apache.servicemix.specs.jsr303-api-1.0.0")
						.version(asInProject()),
				// Jetty HttpClient for testing
				mavenBundle().groupId("org.eclipse.jetty").artifactId("jetty-client").versionAsInProject()
		};
	}

	protected static Option addCodeCoverageOption() {
		String coverageCommand = System.getProperty(COVERAGE_COMMAND);
		if (coverageCommand != null) {
			LOG.info("found coverage option {}", coverageCommand);
			return CoreOptions.vmOption(coverageCommand);
		}
		return null;
	}

	public Option[] jettyConfig() {

		return combine(baseConfig(),
				features(
						maven().groupId("org.ops4j.pax.web")
								.artifactId("pax-web-features").type("xml")
								.classifier("features").versionAsInProject(),
						"pax-http-jetty", "pax-war")
		);
	}

	public Option[] tomcatConfig() {

		return combine(baseConfig(),
				features(
						maven().groupId("org.ops4j.pax.web")
								.artifactId("pax-web-features").type("xml")
								.classifier("features").versionAsInProject(),
						"pax-http-tomcat", "pax-war")
		);
	}

	public Option[] undertowConfig() {

		return combine(baseConfig(),
				features(
						maven().groupId("org.ops4j.pax.web")
								.artifactId("pax-web-features").type("xml")
								.classifier("features").versionAsInProject(),
						"pax-http-undertow", "pax-war")
		);
	}

	protected boolean isEquinox() {
		String frameworkProperty = System.getProperty("pax.exam.framework");
		LOG.info("isEquinox - pax.exam.framework: {}", frameworkProperty);
		System.out.println("Framework: " + frameworkProperty);
		return "equinox".equals(frameworkProperty);
	}

	private boolean isFelix() {
		String frameworkProperty = System.getProperty("pax.exam.framework");
		LOG.info("isFelix - pax.exam.framework: {}", frameworkProperty);
		System.out.println("Framework: " + frameworkProperty);
		return "felix".equals(frameworkProperty);
	}

	private boolean isKaraf4() {
		return getKarafVersion().startsWith("4.");
	}

	private MavenArtifactUrlReference mvnKarafDist() {
		return maven().groupId("org.apache.karaf").artifactId("apache-karaf")
				.type("tar.gz").version(getKarafVersion());
	}

	protected static String getMyFacesVersion() {
		String myFacesVersion = System.getProperty("MyFacesVersion");
		System.out.println("*** The MyFacesVersion is " + myFacesVersion
				+ " ***");
		return myFacesVersion;
	}

	protected static String getProjectVersion() {
		String projectVersion = System.getProperty("ProjectVersion");
		System.out.println("*** The ProjectVersion is " + projectVersion
				+ " ***");
		return projectVersion;
	}

	protected static String getKarafVersion() {
		String karafVersion = System.getProperty("KarafVersion");
		System.out.println("*** The KarafVersion is " + karafVersion + " ***");
		return karafVersion;
	}

	protected HttpTestClient createTestClientForKaraf() {
		return HttpTestClientFactory.createDefaultTestClient()
				.withExternalKeystore("${karaf.base}/etc/keystore");
	}

    @Override
    protected BundleContext getBundleContext() {
        return bundleContext;
    }
}