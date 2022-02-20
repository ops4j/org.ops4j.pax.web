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
import java.io.IOException;
import javax.inject.Inject;

import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.DoNotModifyLogOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.karaf.options.configs.CustomProperties;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.web.itest.AbstractControlledTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClient;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;

import static org.ops4j.pax.exam.Constants.START_LEVEL_TEST_BUNDLE;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemPackage;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

public class AbstractKarafTestBase extends AbstractControlledTestBase {

	protected MavenArtifactUrlReference paxWebFeatures;

	@Inject
	protected FeaturesService featuresService;

	/** To make sure the tests run only when the boot features are fully installed */
	@Inject
	private BootFinished bootFinished;

	/**
	 * Karaf integration tests have completely different <em>base configuration</em>, so no call to
	 * {@code super.baseConfigure()}.
	 * @return
	 */
	@Override
	public Option[] baseConfigure() {
		LOG_DIR.mkdirs();
		String defaultsLogFileName;
		String osgiLogFileName;
		try {
			defaultsLogFileName = new File(LOG_DIR, getClass().getSimpleName() + ".log").getCanonicalPath();
			osgiLogFileName = new File("../etc/log4j2-karaf.properties").getCanonicalPath();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage(), e);
		}

		this.paxWebFeatures = maven()
				.groupId("org.ops4j.pax.web").artifactId("pax-web-features")
				.type("xml").classifier("features").versionAsInProject();

		MavenUrlReference karafStandardFeatures = maven()
				.groupId("org.apache.karaf.features").artifactId("standard")
				.type("xml").classifier("features").version(getKarafVersion());

		MavenArtifactUrlReference karafDistribution = maven()
				.groupId("org.apache.karaf").artifactId("apache-karaf")
				.type("tar.gz").version(getKarafVersion());

		Option[] jdkSpecificOptions = new Option[0];
		if (javaMajorVersion() >= 9) {
			jdkSpecificOptions = new Option[] {
					new VMOption("-classpath"),
					new VMOption("lib/jdk9plus/*" + File.pathSeparator + "lib/boot/*"),
					new VMOption("--add-exports=java.base/org.apache.karaf.specs.locator=java.xml,ALL-UNNAMED"),
					new VMOption("--add-opens=java.base/java.net=ALL-UNNAMED"),
					new VMOption("--add-opens=java.base/java.lang=ALL-UNNAMED"),
					new VMOption("--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED"),
					new VMOption("--add-opens=java.rmi/sun.rmi.transport.tcp=ALL-UNNAMED"),
					new VMOption("--add-opens=java.rmi/sun.rmi.registry=ALL-UNNAMED"),
					new VMOption("--add-reads=java.xml=java.logging"),
					new VMOption("--patch-module=java.base=lib/endorsed/org.apache.karaf.specs.locator-" + getKarafVersion() + ".jar"),
					new VMOption("--patch-module=java.xml=lib/endorsed/org.apache.karaf.specs.java.xml-" + getKarafVersion() + ".jar")
			};
		} else {
			jdkSpecificOptions = new Option[] {
					systemPackage("javax.annotation;version=\"1.3\"")
			};
		}

		Option[] options = new Option[] {
				karafDistributionConfiguration().frameworkUrl(karafDistribution)
						.unpackDirectory(new File("target/paxexam/unpack/"))
						.useDeployFolder(false),
				new DoNotModifyLogOption(),

//				org.ops4j.pax.exam.karaf.options.KarafDistributionOption.debugConfiguration("5005", true),

				configureConsole().ignoreLocalConsole(),

				when(isEquinox()).useOptions(
						editConfigurationFilePut(CustomProperties.KARAF_FRAMEWORK, "equinox"),
						systemProperty("osgi.console").value("6666"),
						systemProperty("osgi.console.enable.builtin").value("true")
				),
				when(isFelix()).useOptions(
						editConfigurationFilePut(CustomProperties.KARAF_FRAMEWORK, "felix")
				),

				logLevel(LogLevel.WARN),
				keepRuntimeFolder(),
				systemTimeout(60 * 60 * 1000),

				// set to "4" to see Felix wiring information
				frameworkProperty("felix.log.level").value("1"),

				editConfigurationFilePut("etc/system.properties", "org.ops4j.pax.logging.DefaultServiceLog.level", "INFO"),
				editConfigurationFilePut("etc/custom.properties", "org.ops4j.pax.logging.service.frameworkEventsLogLevel", "DISABLED"),
				editConfigurationFilePut("etc/custom.properties", "org.ops4j.pax.logging.useFileLogFallback", defaultsLogFileName),
				editConfigurationFilePut("etc/custom.properties", "org.ops4j.pax.logging.property.file", osgiLogFileName),
				editConfigurationFileExtend("etc/config.properties", "karaf-capabilities",
						"osgi.contract;osgi.contract=JavaAnnotation;uses:=\"javax.annotation,javax.annotation.sql,javax.annotation.security\";" +
								"version:List<Version>=\"1.3,1.2,1.1,1.0\""),

				editConfigurationFilePut("etc/branding.properties", "welcome", ""), // No welcome banner
				editConfigurationFilePut("etc/branding-ssh.properties", "welcome", ""),
				configureConsole().ignoreRemoteShell(),
				configureConsole().ignoreLocalConsole(),

				features(karafStandardFeatures, "wrap", "aries-blueprint"),

				editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg", "log4j2.pattern", "%d{HH:mm:ss.SSS} %-5level {%thread} [%C] (%F:%L) : %msg%n"),
				editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg", "log4j2.logger.paxweb.name", "org.ops4j.pax.web"),
				editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg", "log4j2.logger.paxweb.level", "trace"),

				editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.useFallbackRepositories", "false"),
				editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.repositories", "https://repo1.maven.org/maven2@id=central"),

				editConfigurationFilePut("etc/users.properties", "karaf", "karaf,_g_:admingroup"),
				editConfigurationFilePut("etc/users.properties", "_g_:admingroup", "group,admin,manager,viewer,systembundles,ssh"),

				replaceConfigurationFile("etc/keystore", new File("../etc/security/server.jks")),
//				replaceConfigurationFile("/etc/jetty.xml", new File(getClass().getClassLoader().getResource("jetty.xml").getFile())),

				systemProperty("karaf.log.console").value("OFF"),
				systemProperty("pax-web.version").value(System.getProperty("pax-web.version")),

				// contains private packaged Http Client (non-OSGi HttpClient 5)
				mavenBundle("org.ops4j.pax.web.itest", "pax-web-itest-utils").versionAsInProject(),
				mavenBundle("org.ops4j.pax.web.itest", "pax-web-itest-common").versionAsInProject()
		};

		return combine(jdkSpecificOptions, options);
	}

	protected boolean isEquinox() {
		String frameworkProperty = System.getProperty("pax.exam.framework");
		LOG.info("pax.exam.framework: {}", frameworkProperty);
		return "equinox".equals(frameworkProperty);
	}

	private boolean isFelix() {
		String frameworkProperty = System.getProperty("pax.exam.framework");
		LOG.info("pax.exam.framework: {}", frameworkProperty);
		return "felix".equals(frameworkProperty);
	}

	public Option jspConfig() {
		return features(paxWebFeatures, "pax-web-jsp");
	}

	public Option[] jettyConfig() {
		return combine(baseConfigure(),
				features(paxWebFeatures, "pax-web-http-jetty", "pax-web-war", "pax-web-whiteboard"));
	}

	public Option[] jettyHttp2Config() {
		String http2Feature = "pax-web-jetty-http2-jdk8";
		if (javaMajorVersion() > 8) {
			http2Feature = "pax-web-jetty-http2-jdk9";
		}
		return combine(baseConfigure(),
				features(paxWebFeatures, "pax-web-http-jetty", "pax-web-jetty-http2", http2Feature, "pax-web-war", "pax-web-whiteboard"));
	}

	public Option[] tomcatConfig() {
		return combine(baseConfigure(),
				features(paxWebFeatures, "pax-web-http-tomcat", "pax-web-war", "pax-web-whiteboard"));
	}

	public Option[] undertowConfig() {
		return combine(baseConfigure(),
				features(paxWebFeatures, "pax-web-http-undertow", "pax-web-war", "pax-web-whiteboard"));
	}

	protected Option[] ariesCdiAndMyfaces() {
		return new Option[] {
				mavenBundle("jakarta.el", "jakarta.el-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web", "pax-web-compatibility-el2")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 2).noStart(),
				mavenBundle("jakarta.websocket", "jakarta.websocket-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				// it has to be CDI 1.2 for Myfaces 2.3.x, but can't conflict with CDI 2.0 needed by aries-cdi
//				mavenBundle("javax.enterprise", "cdi-api")
//						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("commons-collections", "commons-collections")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("commons-beanutils", "commons-beanutils")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("commons-digester", "commons-digester")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.apache.myfaces.core", "myfaces-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.apache.myfaces.core", "myfaces-impl")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.javax-inject")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 2),
				mavenBundle("org.ops4j.pax.web", "pax-web-fragment-myfaces-inject")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 2).noStart(),
				mavenBundle("org.ops4j.pax.web", "pax-web-fragment-myfaces-spifly")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 2).noStart(),

				// These 4 would be required because of osgi.contract capabilities. But Pax Web provides proper
				// compatibility bundles that fix _canonical_ jakarta API bundles
//				mavenBundle("org.apache.geronimo.specs", "geronimo-el_2.2_spec")
//						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
//				mavenBundle("org.apache.geronimo.specs", "geronimo-interceptor_1.2_spec")
//						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
//				mavenBundle("org.apache.geronimo.specs", "geronimo-jcdi_2.0_spec")
//						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
//				mavenBundle("org.apache.geronimo.specs", "geronimo-annotation_1.3_spec")
//						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),

				mavenBundle("jakarta.enterprise", "jakarta.enterprise.cdi-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1).noStart(),
				mavenBundle("org.ops4j.pax.web", "pax-web-compatibility-cdi12")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 2).noStart(),
				mavenBundle("jakarta.interceptor", "jakarta.interceptor-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web", "pax-web-compatibility-interceptor12")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 2).noStart(),
				// Aries CDI extension.servlet.weld and extension.el.jsp require JavaServlet 3.1 capability...
				mavenBundle("org.ops4j.pax.web", "pax-web-compatibility-servlet31")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1).noStart(),

				mavenBundle("org.osgi", "org.osgi.service.cdi")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.apache.felix", "org.apache.felix.converter")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),

				mavenBundle("org.apache.aries.cdi", "org.apache.aries.cdi.spi")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.apache.aries.cdi", "org.apache.aries.cdi.extension.spi")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.apache.aries.cdi", "org.apache.aries.cdi.extender")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.apache.aries.cdi", "org.apache.aries.cdi.weld")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.apache.aries.cdi", "org.apache.aries.cdi.extension.servlet.common")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.apache.aries.cdi", "org.apache.aries.cdi.extension.servlet.weld")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.apache.aries.cdi", "org.apache.aries.cdi.extension.el.jsp")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.apache.aries.cdi", "org.apache.aries.cdi.extra")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.jboss.weld", "weld-osgi-bundle")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.jboss.classfilewriter", "jboss-classfilewriter")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),

				mavenBundle("org.apache.aries.spifly", "org.apache.aries.spifly.dynamic.bundle")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ow2.asm", "asm")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ow2.asm", "asm-commons")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ow2.asm", "asm-util")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ow2.asm", "asm-tree")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ow2.asm", "asm-analysis")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),

				mavenBundle("jakarta.validation", "jakarta.validation-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.jboss.classfilewriter", "jboss-classfilewriter")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1)
		};
	}

	protected static String getMyFacesVersion() {
		return System.getProperty("myfaces.version");
	}

	protected static String getProjectVersion() {
		return System.getProperty("pax-web.version");
	}

	protected static String getKarafVersion() {
		return System.getProperty("karaf.version");
	}

	protected HttpTestClient createTestClientForKaraf() {
		return HttpTestClientFactory.createDefaultTestClient().withExternalKeystore("${karaf.base}/etc/keystore");
	}

}
