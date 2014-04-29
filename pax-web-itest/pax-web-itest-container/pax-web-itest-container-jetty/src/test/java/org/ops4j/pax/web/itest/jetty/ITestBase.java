package org.ops4j.pax.web.itest.jetty;

import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.workingDirectory;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.OptionUtils.combine;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.web.itest.base.HttpTestClient;
import org.ops4j.pax.web.itest.base.ServletListenerImpl;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.WaitCondition;
import org.ops4j.pax.web.itest.base.WebListenerImpl;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExamReactorStrategy(PerClass.class)
public class ITestBase {

	protected static final String WEB_CONTEXT_PATH = "Web-ContextPath";
	protected static final String WEB_CONNECTORS = "Web-Connectors";
	protected static final String WEB_VIRTUAL_HOSTS = "Web-VirtualHosts";
	protected static final String WEB_BUNDLE = "webbundle:";

	protected static final String REALM_NAME = "realm.properties";

	static final Logger LOG = LoggerFactory.getLogger(ITestBase.class);
	
    // the name of the system property which captures the jococo coverage agent command
    //if specified then agent would be specified otherwise ignored
    protected static final String COVERAGE_COMMAND = "coverage.command";

	@Inject
	protected BundleContext bundleContext;

	protected WebListener webListener;

	protected ServletListener servletListener;
	
	protected HttpTestClient testClient;

	public static Option[] baseConfigure() {
		return options(
				workingDirectory("target/paxexam/"),
				cleanCaches(true),
				junitBundles(),
				frameworkProperty("osgi.console").value("6666"),
				frameworkProperty("osgi.console.enable.builtin").value("true"),
				frameworkProperty("felix.bootdelegation.implicit").value(
						"false"),
				// frameworkProperty("felix.log.level").value("4"),
				systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level")
						.value("DEBUG"),
				systemProperty("org.osgi.service.http.hostname").value(
						"127.0.0.1"),
				systemProperty("org.osgi.service.http.port").value("8181"),
				systemProperty("java.protocol.handler.pkgs").value(
						"org.ops4j.pax.url"),
				systemProperty("org.ops4j.pax.url.war.importPaxLoggingPackages")
						.value("true"),
				systemProperty("org.ops4j.pax.web.log.ncsa.enabled").value(
						"true"),
				systemProperty("org.ops4j.pax.web.log.ncsa.directory").value(
						"target/logs"),
				systemProperty("ProjectVersion").value(
						VersionUtil.getProjectVersion()),

				addCodeCoverageOption(),
						
				mavenBundle().groupId("org.ops4j.pax.web.itest")
						.artifactId("pax-web-itest-base").versionAsInProject(),

				// do not include pax-logging-api, this is already provisioned
				// by Pax Exam
				mavenBundle().groupId("org.ops4j.pax.logging")
						.artifactId("pax-logging-service").version("1.6.4"),

				mavenBundle().groupId("org.ops4j.pax.url")
						.artifactId("pax-url-war").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.url")
						.artifactId("pax-url-wrap").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.url")
						.artifactId("pax-url-commons").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.swissbox")
						.artifactId("pax-swissbox-bnd").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.swissbox")
						.artifactId("pax-swissbox-property")
						.version(asInProject()),
				mavenBundle().groupId("biz.aQute.bnd").artifactId("bndlib")
						.version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.swissbox")
						.artifactId("pax-swissbox-optional-jcl")
						.version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-spi").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-api").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-extender-war")
						.version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-extender-whiteboard")
						.version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-runtime").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-jsp").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jdt.core.compiler")
						.artifactId("ecj").version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-servlet_3.0_spec")
						.version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.url")
						.artifactId("pax-url-aether").version(asInProject()),
				
                mavenBundle().groupId("org.apache.xbean")
                        .artifactId("xbean-reflect").version(asInProject()),
            	mavenBundle().groupId("org.apache.xbean")
                        .artifactId("xbean-finder-shaded").version(asInProject()),
                mavenBundle().groupId("org.apache.xbean")
                        .artifactId("xbean-bundleutils").version(asInProject()),
                mavenBundle().groupId("org.apache.xbean")
                        .artifactId("xbean-asm4-shaded").version(asInProject()),
                mavenBundle().groupId("org.ow2.asm")
                        .artifactId("asm-all").version(asInProject()),
                        
				mavenBundle("commons-codec", "commons-codec").version(
						asInProject()),
				mavenBundle("org.apache.felix", "org.apache.felix.eventadmin")
						.version(asInProject()),
				wrappedBundle(mavenBundle("org.apache.httpcomponents",
						"httpcore").version(asInProject())),
				wrappedBundle(mavenBundle("org.apache.httpcomponents",
						"httpmime").version(asInProject())),
				wrappedBundle(mavenBundle("org.apache.httpcomponents",
						"httpclient").version(asInProject())));
	}

	public static Option[] configureJetty() {
		return combine(
				baseConfigure(),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-jetty").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-util").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-io").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-http").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-continuation")
						.version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-server").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-security").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-xml").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-servlet").version(asInProject()));
	}

	@Before
	public void setUpITestBase() throws Exception {
		testClient = new HttpTestClient();
	}

	@After
	public void tearDownITestBase() throws Exception {
		testClient.close();
		testClient = null;
	}

	protected void initWebListener() {
		webListener = new WebListenerImpl();
		bundleContext.registerService(WebListener.class, webListener, null);
	}

	protected void initServletListener() {
		initServletListener(null);
	}

	protected void initServletListener(String servletName) {
		if (servletName == null)
			servletListener = new ServletListenerImpl();
		else
			servletListener = new ServletListenerImpl(servletName);
		bundleContext.registerService(ServletListener.class, servletListener,
				null);
	}

	protected void waitForWebListener() throws InterruptedException {
		new WaitCondition("webapp startup") {
			@Override
			protected boolean isFulfilled() {
				return ((WebListenerImpl) webListener).gotEvent();
			}
		}.waitForCondition();
	}

	protected void waitForServletListener() throws InterruptedException {
		new WaitCondition("servlet startup") {
			@Override
			protected boolean isFulfilled() {
				return ((ServletListenerImpl) servletListener).gotEvent();
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

	protected Bundle installAndStartBundle(String bundlePath)
			throws BundleException, InterruptedException {
		final Bundle bundle = bundleContext.installBundle(bundlePath);
		bundle.start();
		new WaitCondition("bundle startup") {
			@Override
			protected boolean isFulfilled() {
				return bundle.getState() == Bundle.ACTIVE;
			}
		}.waitForCondition();
		return bundle;
	}

	private static Option addCodeCoverageOption() {
		String coverageCommand = System.getProperty(COVERAGE_COMMAND);
		if (coverageCommand != null) {
			LOG.info("found coverage option {}", coverageCommand);
			return CoreOptions.vmOption(coverageCommand);
		}
		return null;
	}

}
