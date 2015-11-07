package org.ops4j.pax.web.itest.jetty;

import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.workingDirectory;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.web.itest.base.TestConfiguration.addCodeCoverageOption;
import static org.ops4j.pax.web.itest.base.TestConfiguration.paxWebBundles;
import static org.ops4j.pax.web.itest.base.TestConfiguration.logbackBundles;
import static org.ops4j.pax.web.itest.base.TestConfiguration.paxJettyBundles;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.web.itest.base.HttpTestClient;
import org.ops4j.pax.web.itest.base.ServletListenerImpl;
import org.ops4j.pax.web.itest.base.TestConfiguration;
import org.ops4j.pax.web.itest.base.WaitCondition;
import org.ops4j.pax.web.itest.base.WebListenerImpl;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExamReactorStrategy(PerMethod.class)
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
				systemProperty("org.ops4j.pax.web.jsp.scratch.dir").value("target/paxexam/scratch-dir"),
				systemProperty("ProjectVersion").value(
						TestConfiguration.PAX_WEB_VERSION),
				systemProperty("org.ops4j.pax.url.mvn.certificateCheck").value("false"),
				addCodeCoverageOption(),
				
				logbackBundles(),
						
				mavenBundle().groupId("org.ops4j.pax.web.itest")
						.artifactId("pax-web-itest-base").versionAsInProject(),

				paxWebBundles(),
						
				wrappedBundle(mavenBundle("org.apache.httpcomponents",
                        "httpcore").version(asInProject())),
				wrappedBundle(mavenBundle("org.apache.httpcomponents",
                        "httpcore-nio").version(asInProject())),
				wrappedBundle(mavenBundle("org.apache.httpcomponents",
						"httpmime").version(asInProject())),
				wrappedBundle(mavenBundle("org.apache.httpcomponents",
						"httpclient").version(asInProject())),
				wrappedBundle(mavenBundle("org.apache.httpcomponents",
                        "httpasyncclient").version(asInProject()))
						
				);
	}

	public static Option[] configureBaseWithServlet() {
		return combine(
				baseConfigure(),
				mavenBundle().groupId("javax.servlet")
				.artifactId("javax.servlet-api").versionAsInProject());
	}

	public static Option[] configureJetty() {
		return combine(
				configureBaseWithServlet(),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-jetty").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-runtime").version(asInProject()),
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
						.artifactId("jetty-client").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-security").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-xml").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-servlet").version(asInProject()));
	}
	
	public static Option[] configureJettyBundle() {
		return combine(
				baseConfigure(),
				systemPackages("javax.xml.namespace;version=1.0.0","javax.transaction;version=1.1.0"),
				mavenBundle().groupId("org.ops4j.pax.web")
				.artifactId("pax-web-jetty-bundle").version(asInProject())
			);
	}
	
//	public static Option[] configureSpdyJetty() {
//		return combine(
//				configureJetty(),
//					mavenBundle().groupId("org.eclipse.jetty").artifactId("jetty-alpn-server").version(asInProject()),
//					mavenBundle().groupId("org.eclipse.jetty.osgi").artifactId("jetty-osgi-alpn").version(asInProject()),
//					mavenBundle().groupId("org.eclipse.jetty.spdy")
//							.artifactId("spdy-core").version(asInProject()),
//					mavenBundle().groupId("org.eclipse.jetty.spdy")
//							.artifactId("spdy-client").version(asInProject()),
//					mavenBundle().groupId("org.eclipse.jetty.spdy")
//							.artifactId("spdy-server").version(asInProject()),
//					mavenBundle().groupId("org.eclipse.jetty.spdy")
//							.artifactId("spdy-http-server").version(asInProject()),
//					mavenBundle().groupId("org.eclipse.jetty.spdy")
//							.artifactId("spdy-http-common").version(asInProject())
//				);
//	}

	public static Option[] configureWebSocketJetty() {
		return combine(
				configureJetty(),
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("websocket-server").version(asInProject()),
						
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("websocket-client").version(asInProject()),

				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("websocket-common").version(asInProject()),
						
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("websocket-servlet").version(asInProject()),
						
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("websocket-api").version(asInProject()),
						
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("javax-websocket-server-impl").version(asInProject()),

				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("javax-websocket-client-impl").version(asInProject()),
						
				mavenBundle().groupId("javax.websocket")
						.artifactId("javax.websocket-api").versionAsInProject(),

				mavenBundle().groupId("org.glassfish").artifactId("javax.json")
						.versionAsInProject(),

				mavenBundle().groupId("javax.json")
						.artifactId("javax.json-api").versionAsInProject(),
						
				mavenBundle().groupId("org.apache.aries").artifactId("org.apache.aries.util").versionAsInProject(),
				mavenBundle().groupId("org.apache.aries.spifly").artifactId("org.apache.aries.spifly.dynamic.bundle").versionAsInProject()

				);
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
		if (coverageCommand != null && coverageCommand.length() > 0) {
			LOG.info("found coverage option {}", coverageCommand);
			return CoreOptions.vmOption(coverageCommand);
		}
		return null;
	}

}
