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
package org.ops4j.pax.web.itest.base;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.exam.Constants.START_LEVEL_SYSTEM_BUNDLES;
import static org.ops4j.pax.exam.Constants.START_LEVEL_TEST_BUNDLE;
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackage;
import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.frameworkStartLevel;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.linkBundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.url;
import static org.ops4j.pax.exam.CoreOptions.workingDirectory;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.web.itest.base.TestConfiguration.paxWebBundles;

/**
 * <p>Test base to be used with <code>pax.exam.system=default</code>, where no implicit bundles
 * are installed by pax-exam itself - all bundles have to be installed explicitly</p>
 * <p>Removes redundant code (including tests that are the same in Jetty-, Tomcat-, Undertow-Servers)</p>
 */
public abstract class AbstractControlledTestBase {

	public static Logger LOG = LoggerFactory.getLogger("org.ops4j.pax.web.itest");

	@Rule
	public TestName testName = new TestName();

	// the name of the system property which captures the jococo coverage agent command
	// if specified then agent would be specified otherwise ignored
	protected static final String COVERAGE_COMMAND = "coverage.command";
	protected static final String WEB_CONTEXT_PATH = "Web-ContextPath";
	protected static final String WEB_CONNECTORS = "Web-Connectors";
	protected static final String WEB_VIRTUAL_HOSTS = "Web-VirtualHosts";
	protected static final String WEB_BUNDLE = "webbundle:";
	protected static final String REALM_NAME = "realm.properties";

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	protected WebListener webListener;
	protected ServletListener servletListener;

	@Before
	public void beforeEach() {
		LOG.info("========== Running {}.{}() ==========", getClass().getName(), testName.getMethodName());
	}

	@After
	public void afterEach() {
		LOG.info("========== Finished {}.{}() ==========", getClass().getName(), testName.getMethodName());
	}

	protected static Option[] baseConfigure() {
		return options(
				// basic options
				bootDelegationPackage("sun.*"),
				frameworkStartLevel(START_LEVEL_TEST_BUNDLE),
				workingDirectory("target/paxexam"),
				cleanCaches(true),
				systemTimeout(60 * 60 * 1000),

				// path relative to pax-web-itest-container-<containerName>
				systemProperty("org.ops4j.pax.logging.property.file").value("src/test/resources/pax-logging.properties"),
				frameworkProperty("felix.bootdelegation.implicit").value("false"),
				// set to "4" to see Felix wiring information
				frameworkProperty("felix.log.level").value("1"),

				// added implicitly by pax-exam, if pax.exam.system=test
				// these resources are provided inside org.ops4j.pax.exam:pax-exam-link-mvn jar
				url("link:classpath:META-INF/links/org.ops4j.base.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
				url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.core.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
				url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.extender.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
				url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.framework.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
				url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.lifecycle.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
				url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.tracker.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
				url("link:classpath:META-INF/links/org.ops4j.pax.exam.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
				url("link:classpath:META-INF/links/org.ops4j.pax.exam.inject.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
				url("link:classpath:META-INF/links/org.ops4j.pax.extender.service.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),

				// configadmin should start before org.ops4j.pax.logging.pax-logging-log4j2
				linkBundle("org.apache.felix.configadmin").startLevel(START_LEVEL_SYSTEM_BUNDLES),

				// added implicitly by pax-exam, if pax.exam.system=test
				//  - url("link:classpath:META-INF/links/org.ops4j.pax.logging.api.link").startLevel( START_LEVEL_SYSTEM_BUNDLES),
				//  - url("link:classpath:META-INF/links/org.apache.geronimo.specs.atinject.link") .startLevel(START_LEVEL_SYSTEM_BUNDLES),
				//  - url("link:classpath:META-INF/links/org.osgi.compendium.link").startLevel( START_LEVEL_SYSTEM_BUNDLES),
				// but we will use versions aligned to pax-web:
				linkBundle("org.ops4j.pax.logging.pax-logging-api").startLevel(START_LEVEL_SYSTEM_BUNDLES),
				linkBundle("org.ops4j.pax.logging.pax-logging-log4j2").startLevel(START_LEVEL_SYSTEM_BUNDLES),
				linkBundle("org.apache.servicemix.bundles.javax-inject").startLevel(START_LEVEL_SYSTEM_BUNDLES),
				// this should not be installed as bundle - it's compile-time dependency only
				//url("link:classpath:osgi.cmpn.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),

				// org.ops4j.pax.exam.nat.internal.NativeTestContainer.start() adds this explicitly
				systemProperty("java.protocol.handler.pkgs").value("org.ops4j.pax.url"),

				// last Option[] that's required to run simplest @Test
				junitBundles(),

				// pax-web specific properties
				systemProperty("org.osgi.service.http.hostname").value("127.0.0.1"),
				systemProperty("org.osgi.service.http.port").value("8181"),
//				systemProperty("org.ops4j.pax.url.war.importPaxLoggingPackages").value("true"),
				systemProperty("org.ops4j.pax.web.log.ncsa.enabled").value("true"),
				systemProperty("org.ops4j.pax.web.log.ncsa.directory").value("target/logs"),
//				systemProperty("org.ops4j.pax.web.jsp.scratch.dir").value("target/paxexam/scratch-dir"),
				systemProperty("ProjectVersion").value(TestConfiguration.PAX_WEB_VERSION),
//				systemProperty("org.ops4j.pax.url.mvn.certificateCheck").value("false"),

//				addCodeCoverageOption(),

				mavenBundle().groupId("org.ops4j.pax.web.itest").artifactId("pax-web-itest-base").versionAsInProject(),

				mavenBundle().groupId("javax.servlet").artifactId("javax.servlet-api").versionAsInProject(),
				mavenBundle().groupId("javax.websocket").artifactId("javax.websocket-api").version(asInProject()),
				paxWebBundles(),

				// Jetty HttpClient for testing
				mavenBundle().groupId("org.eclipse.jetty").artifactId("jetty-client").versionAsInProject(),
				mavenBundle().groupId("org.eclipse.jetty").artifactId("jetty-http").versionAsInProject(),
				mavenBundle().groupId("org.eclipse.jetty").artifactId("jetty-util").versionAsInProject(),
				mavenBundle().groupId("org.eclipse.jetty").artifactId("jetty-io").versionAsInProject()
		);
	}

	protected boolean isEquinox() {
		return "equinox".equals(System.getProperty("pax.exam.framework"));
	}

	protected void initWebListener() {
		webListener = new WebListenerImpl();
		getBundleContext().registerService(WebListener.class, webListener, null);
	}

	protected void initServletListener() {
		initServletListener(null);
	}

	protected void initServletListener(String servletName) {
		if (servletName == null) {
			servletListener = new ServletListenerImpl();
		} else {
			servletListener = new ServletListenerImpl(servletName);
		}
		getBundleContext().registerService(ServletListener.class, servletListener,
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
		waitForServletListener(null);
	}

	protected void waitForServletListener(Long timeOut) throws InterruptedException {
		if (timeOut == null) {
			new WaitCondition("servlet startup") {
				@Override
				protected boolean isFulfilled() {
					return ((ServletListenerImpl) servletListener).gotEvent();
				}
			}.waitForCondition();
		} else {
			new WaitCondition("servlet startup") {
				@Override
				protected boolean isFulfilled() {
					return ((ServletListenerImpl) servletListener).gotEvent();
				}
			}.waitForCondition(timeOut);
		}
	}

	protected void waitForServer(final String path) throws InterruptedException {
		new WaitCondition("server") {
			@Override
			protected boolean isFulfilled() throws Exception {
				try {
					HttpTestClientFactory.createDefaultTestClient()
							.withReturnCode(200, 404)
							.doGET(path).executeTest();
					return true;
				} catch (AssertionError | Exception e) {
					return false;
				}
			}
		}.waitForCondition();
	}

	protected Bundle installAndStartBundle(String bundlePath) {
		try {
			final Bundle bundle = getBundleContext().installBundle(bundlePath);
			bundle.start();
			new WaitCondition("bundle startup") {
				@Override
				protected boolean isFulfilled() {
					return bundle.getState() == Bundle.ACTIVE;
				}
			}.waitForCondition();
			return bundle;
		} catch (BundleException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	protected static Option addCodeCoverageOption() {
		String coverageCommand = System.getProperty(COVERAGE_COMMAND);
		if (coverageCommand != null && coverageCommand.length() > 0) {
//            logger.info("found coverage option {}", coverageCommand);
			return CoreOptions.vmOption(coverageCommand);
		}
		return null;
	}

	public static HttpService getHttpService(final BundleContext bundleContext) {
		ServiceReference<HttpService> ref = bundleContext.getServiceReference(HttpService.class);
		Assert.assertNotNull("Failed to get HttpService", ref);
		HttpService httpService = bundleContext.getService(ref);
		Assert.assertNotNull("Failed to get HttpService", httpService);
		return httpService;
	}

	/**
	 * Assuming that <code>serviceClass</code> represents a service related to <code>pid</code>, this method
	 * synchronously performs some operation (e.g., configadmin update) and waits for service to be modified.
	 * @param bundleContext
	 * @param serviceClass
	 */
	protected <T> boolean waitForServiceReregistration(BundleContext bundleContext, Class<T> serviceClass, ServiceUpdateKind updateKind, Runnable callback) throws InterruptedException, IOException {
		// first get current service instance
		ServiceTracker<T, T> tracker = new ServiceTracker<>(bundleContext, serviceClass, null);
		tracker.open();
		tracker.waitForService(TimeUnit.SECONDS.toMillis(5));

		// listener to wait for modified service
		final CountDownLatch latch = new CountDownLatch(updateKind.getEventCount());
		ServiceListener listener = (event) -> {
			switch (updateKind) {
				case MODIFY: {
					if (event.getType() == ServiceEvent.MODIFIED) {
						latch.countDown();
					}
					break;
				}
				case UNREGISTER_REGISTER: {
					if (event.getType() == ServiceEvent.UNREGISTERING
							|| event.getType() == ServiceEvent.REGISTERED) {
						latch.countDown();
					}
					break;
				}
			}
		};
		try {
			bundleContext.addServiceListener(listener, "(objectClass=" + serviceClass.getName() + ")");
		} catch (InvalidSyntaxException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		// update and wait
		try {
			callback.run();
			return latch.await(20, TimeUnit.SECONDS);
		} finally {
			bundleContext.removeServiceListener(listener);
		}
	}

	/**
	 * Callback to get access to the injected BundleContext
	 *
	 * @return the frameworks BundleContext
	 */
	protected abstract BundleContext getBundleContext();

	public void testSimpleFilter() throws Exception {
		ServiceTracker<WebContainer, WebContainer> tracker = new ServiceTracker<>(getBundleContext(), WebContainer.class, null);
		tracker.open();
		WebContainer service = tracker.waitForService(TimeUnit.SECONDS.toMillis(20));

		final String fullContent = "This content is Filtered by a javax.servlet.Filter";
		Filter filter = new Filter() {

			@Override
			public void init(FilterConfig filterConfig) throws ServletException {
			}

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
				PrintWriter writer = response.getWriter();
				writer.write(fullContent);
				writer.flush();
			}

			@Override
			public void destroy() {
			}
		};

		Dictionary<String, String> initParams = new Hashtable<>();

		HttpContext defaultHttpContext = service.createDefaultHttpContext();
		service.begin(defaultHttpContext);
		service.registerResources("/", "default", defaultHttpContext);

		service.registerFilter(filter, new String[] { "/testFilter/*", }, new String[] { "default", }, initParams, defaultHttpContext);

		service.end(defaultHttpContext);

		Thread.sleep(200);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain test from previous FilterChain",
						resp -> resp.contains("This content is Filtered by a javax.servlet.Filter"))
				.doGET("http://127.0.0.1:8181/testFilter/filter.me")
				.executeTest();

		service.unregisterFilter(filter);
	}

	public static enum ServiceUpdateKind {
		UNREGISTER_REGISTER(2),
		MODIFY(1);

		private int eventCount;

		ServiceUpdateKind(int eventCount) {
			this.eventCount = eventCount;
		}

		public int getEventCount() {
			return eventCount;
		}
	}

	/**
	 * <p>
	 *     JSF uses a hidden input-field which carries around a JSF internal View-State. The View-State is necessary
	 *     when form submits are tested with a POST-request.
	 * </p>
	 * <p>
	 *     When testing a POST against JSF, a prior GET has to be made!
	 *     This method extracts the View-State from prior GET.
	 * </p>
	 * @param response the response from a initial GET-request
	 * @return found View-State
	 * @throws IllegalStateException when no View-State was found
	 */
	protected String extractJsfViewState(String response){
		String intermediate = response.substring(response.indexOf("name=\"javax.faces.ViewState\""));
		int indexOf = intermediate.indexOf("value=\"");
		String substring = intermediate.substring(indexOf + 7);
		indexOf = substring.indexOf("\"");
		String viewstate = substring.substring(0, indexOf);
		if(viewstate == null || viewstate.trim().length() == 0){
			throw new IllegalStateException("No JSF-View-State was found in response!");
		}
		return viewstate;
	}

}
