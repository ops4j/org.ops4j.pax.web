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

import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.karaf.options.configs.CustomProperties;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.web.itest.base.ServletListenerImpl;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.WebListenerImpl;
import org.ops4j.pax.web.itest.base.client.HttpTestClient;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;

import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;

@RunWith(PaxExam.class)
public class KarafBaseTest {
	
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

		return new Option[] {
				karafDistributionConfiguration().frameworkUrl(mvnKarafDist())
						.unpackDirectory(new File("target/paxexam/unpack/"))
						.useDeployFolder(false)/*.runEmbedded(true), //only for debugging*/ ,

				// KarafDistributionOption.debugConfiguration("5005", true),
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

				KarafDistributionOption.replaceConfigurationFile("etc/keystore", new File(getClass().getClassLoader().getResource("keystore").getFile())),
                KarafDistributionOption.replaceConfigurationFile("/etc/jetty.xml", new File(getClass().getClassLoader().getResource("jetty.xml").getFile())),
				systemProperty("ProjectVersion").value(
						VersionUtil.getProjectVersion()),
				addCodeCoverageOption(),

				
				mavenBundle().groupId("org.ops4j.pax.web.itest")
				        .artifactId("pax-web-itest-base").versionAsInProject(),
				//new ExamBundlesStartLevel(4),
				// FIXME still needed ?
				mavenBundle("org.apache.httpcomponents",
						"httpcore-osgi").version(asInProject()),
                mavenBundle("org.apache.httpcomponents",
                        "httpclient-osgi").version(asInProject()),
                mavenBundle("org.apache.httpcomponents",
                        "httpasyncclient-osgi").version(asInProject()),
				mavenBundle().groupId("commons-beanutils")
						.artifactId("commons-beanutils").version(asInProject()),
				// ---------------------------
				mavenBundle().groupId("commons-collections")
						.artifactId("commons-collections")
						.version(asInProject()),
				mavenBundle().groupId("commons-codec")
						.artifactId("commons-codec").version(asInProject()),
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
				// FIXME necessary to get rid of ClassNotFoundException
				mavenBundle().groupId("org.eclipse.jetty.websocket").artifactId("websocket-api").version("9.3.6.v20151106"),
				mavenBundle().groupId("org.eclipse.jetty.websocket").artifactId("websocket-common").version("9.3.6.v20151106"),
				mavenBundle().groupId("org.eclipse.jetty").artifactId("jetty-io").version("9.3.6.v20151106"),
				mavenBundle().groupId("org.eclipse.jetty").artifactId("jetty-util").version("9.3.6.v20151106"),
				// Jetty HttpClient for testing
				mavenBundle().groupId("org.eclipse.jetty").artifactId("jetty-client").versionAsInProject()
		};
	}
	private static Option addCodeCoverageOption() {
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
	
	private boolean isEquinox() {
		String frameworkProperty = System.getProperty("pax.exam.framework");
		LOG.info("isEquinox - pax.exam.framework: {}", frameworkProperty);
		System.out.println("Framework: "+frameworkProperty);
		return "equinox".equals(frameworkProperty);
	}

	private boolean isFelix() {
		String frameworkProperty = System.getProperty("pax.exam.framework");
		LOG.info("isFelix - pax.exam.framework: {}", frameworkProperty);
		System.out.println("Framework: "+frameworkProperty);
		return "felix".equals(frameworkProperty);
	}

	private boolean isKaraf4() {
		return getKarafVersion().startsWith("4.");
	}

	private MavenArtifactUrlReference mvnKarafDist() {
		return maven().groupId("org.apache.karaf").artifactId("apache-karaf")
				.type("tar.gz").version(getKarafVersion());
	}

	protected void initWebListener() {
		webListener = new WebListenerImpl();
		bundleContext.registerService(WebListener.class, webListener, null);
	}
	
	protected void initServletListener() {
		servletListener = new ServletListenerImpl();
		bundleContext.registerService(ServletListener.class, servletListener, null);
	}
	
	protected void waitForWebListener() throws InterruptedException {
		new WaitCondition("webapp startup") {
			@Override
			protected boolean isFulfilled() {
				return ((WebListenerImpl)webListener).gotEvent();
			}
		}.waitForCondition();
	}
	
	protected void waitForServletListener() throws InterruptedException {
		new WaitCondition("servlet startup") {
			@Override
			protected boolean isFulfilled() {
				return ((ServletListenerImpl)servletListener).gotEvent();
			}
		}.waitForCondition();
	}
	
	protected void waitForServer(final String path) throws InterruptedException {
		new WaitCondition("server") {
			private org.ops4j.pax.web.itest.base.client.HttpTestClient client = HttpTestClientFactory.createDefaultTestClient();

			@Override
			protected boolean isFulfilled() throws Exception {
				try{
					HttpTestClientFactory.createDefaultTestClient().doGETandExecuteTest(path);
					return true;
				}catch(AssertionError e){
					return false;
				}
			}
		}.waitForCondition();
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

	protected HttpTestClient createTestClientForKaraf(){
		return HttpTestClientFactory.createDefaultTestClient()
				.withExternalKeystore("${karaf.base}/etc/keystore");
	}

}