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
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.karaf.options.configs.CustomProperties;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.web.itest.base.HttpTestClient;
import org.ops4j.pax.web.itest.base.ServletListenerImpl;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.WebListenerImpl;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	protected HttpTestClient testClient;

	public Option[] baseConfig() {

		MavenUrlReference karafStandardFeature = maven()
				.groupId("org.apache.karaf.features").artifactId("standard")
				.type("xml").classifier("features").version(getKarafVersion());

		return new Option[] {
				karafDistributionConfiguration().frameworkUrl(mvnKarafDist())
						.unpackDirectory(new File("target/paxexam/unpack/"))
						.useDeployFolder(false),

				// KarafDistributionOption.debugConfiguration("5005", true),
				configureConsole().ignoreLocalConsole(),
				when(isEquinox()).useOptions(
					editConfigurationFilePut(
									CustomProperties.KARAF_FRAMEWORK, "equinox"),
					systemProperty("pax.exam.framework").value(
									System.getProperty("pax.exam.framework")),
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
	            editConfigurationFilePut("etc/config.properties", "org.osgi.framework.system.capabilities", " ${eecap-${java.specification.version}}, \\\n" + 
	            		" service-reference;effective:=active;objectClass=org.osgi.service.packageadmin.PackageAdmin, \\\n" + 
	            		" service-reference;effective:=active;objectClass=org.osgi.service.startlevel.StartLevel, \\\n" + 
	            		" service-reference;effective:=active;objectClass=org.osgi.service.url.URLHandlers"),
	            
				KarafDistributionOption.replaceConfigurationFile("etc/keystore", new File("src/test/resources/keystore")),
                KarafDistributionOption.replaceConfigurationFile("/etc/jetty.xml", new File("src/test/resources/jetty.xml")),
				systemProperty("ProjectVersion").value(
						VersionUtil.getProjectVersion()),
				addCodeCoverageOption(),

				
				mavenBundle().groupId("org.ops4j.pax.web.itest")
				        .artifactId("pax-web-itest-base").versionAsInProject(),
				//new ExamBundlesStartLevel(4),
				mavenBundle("org.apache.httpcomponents",
						"httpcore-osgi").version(asInProject()),
				mavenBundle("org.apache.httpcomponents",
						"httpclient-osgi").version(asInProject()),
				mavenBundle().groupId("commons-beanutils")
						.artifactId("commons-beanutils").version(asInProject()),
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
						.version(asInProject()) };
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
						"pax-war")
				);
	}
	
	public Option[] tomcatConfig() {

		return combine(baseConfig(), 
				features(
						maven().groupId("org.ops4j.pax.web")
                                .artifactId("pax-web-features").type("xml")
								.classifier("features").versionAsInProject(),
						"pax-war-tomcat")
				);
	}

	private boolean isEquinox() {
		return "equinox".equals(System.getProperty("pax.exam.framework"));
	}

	private boolean isFelix() {
		return "felix".equals(System.getProperty("pax.exam.framework"));
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
			@Override
			protected boolean isFulfilled() throws Exception {
				return testClient.checkServer(path);
			}
		}.waitForCondition();
	}
	

	@Before
	public void setUpITestBase() throws Exception {
		testClient = new HttpTestClient("karaf", "karaf", "etc/keystore");
	}

	@After
	public void tearDown() throws Exception {
		testClient.close();
		testClient = null;
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

}