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
import java.net.MalformedURLException;
import javax.inject.Inject;

import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.DoNotModifyLogOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.karaf.options.configs.CustomProperties;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.web.itest.AbstractControlledTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClient;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;

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
	protected MavenArtifactUrlReference karafStandardFeatures;

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

		this.karafStandardFeatures = maven()
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

		String featureProcessing = null;
		try {
			featureProcessing = new File("target/test-classes/org.apache.karaf.features.xml").toURI().toURL().toString();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
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

//				editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg", "log4j2.pattern", "%d{HH:mm:ss.SSS} %-5level {%thread} [%C] (%F:%L) : %msg%n"),
//				editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg", "log4j2.logger.paxweb.name", "org.ops4j.pax.web"),
//				editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg", "log4j2.logger.paxweb.level", "info"),

				editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.useFallbackRepositories", "false"),
				editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.repositories", "https://repo1.maven.org/maven2@id=central"),

				editConfigurationFilePut("etc/org.apache.karaf.features.cfg", "featureProcessing", featureProcessing),

				editConfigurationFilePut("etc/users.properties", "karaf", "karaf,_g_:admingroup"),
				editConfigurationFilePut("etc/users.properties", "_g_:admingroup", "group,admin,manager,viewer,systembundles,ssh"),

				replaceConfigurationFile("etc/keystore", new File("../etc/security/server.jks")),

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
		return combine(baseConfigure(),
				features(paxWebFeatures, "pax-web-http-jetty", "pax-web-jetty-http2", "pax-web-war", "pax-web-whiteboard"));
	}

	public Option[] tomcatConfig() {
		return combine(baseConfigure(),
				features(paxWebFeatures, "pax-web-http-tomcat", "pax-web-war", "pax-web-whiteboard"));
	}

	public Option[] undertowConfig() {
		return combine(baseConfigure(),
				features(paxWebFeatures, "pax-web-http-undertow", "pax-web-war", "pax-web-whiteboard"));
	}

	public Option scrConfig() {
		return features(karafStandardFeatures, "scr");
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
