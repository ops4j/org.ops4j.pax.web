/*
 * Copyright 2020 OPS4J.
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
package org.ops4j.pax.web.itest.container;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;
import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.logging.PaxLoggingConstants;
import org.ops4j.pax.web.itest.utils.VersionUtils;
import org.ops4j.pax.web.itest.utils.WaitCondition;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.events.ElementEvent;
import org.ops4j.pax.web.service.spi.model.events.ElementEventData;
import org.ops4j.pax.web.service.spi.model.events.FilterEventData;
import org.ops4j.pax.web.service.spi.model.events.ServerEvent;
import org.ops4j.pax.web.service.spi.model.events.ServerListener;
import org.ops4j.pax.web.service.spi.model.events.ServletEventData;
import org.ops4j.pax.web.service.spi.model.events.WebElementListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
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
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.url;
import static org.ops4j.pax.exam.CoreOptions.workingDirectory;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * <p>Single base class for all Pax Exam integration tests.</p>
 *
 * <p>For now it's (almost) a duplicate of similar class from pax-web-itest-osgi, but the goal is to have one after
 * Pax Web 8 refactoring ends.</p>
 */
@ExamReactorStrategy(PerClass.class)
public abstract class AbstractControlledTestBase {

	public static final Logger LOG = LoggerFactory.getLogger(AbstractControlledTestBase.class);
	public static final String PROBE_SYMBOLIC_NAME = "PaxExam-Probe";

	// location of where pax-logging-api will have output file written according to
	// "org.ops4j.pax.logging.useFileLogFallback" system/context property
	// filename will match test class name with ".log" extension
	static final File LOG_DIR = new File("target/logs-default");

	@Rule
	public TestName testName = new TestName();

	@Inject
	protected BundleContext context;

	protected WebElementListener webElementListener;

	@Before
	public void beforeEach() {
		LOG.info("========== Running {}.{}() ==========", getClass().getName(), testName.getMethodName());
	}

	@After
	public void afterEach() {
		LOG.info("========== Finished {}.{}() ==========", getClass().getName(), testName.getMethodName());
	}

	protected Option[] baseConfigure() {
		LOG_DIR.mkdirs();

		Option[] baseOptions = new Option[] {
				// basic options
				bootDelegationPackage("sun.*"),
				bootDelegationPackage("com.sun.*"),

				frameworkStartLevel(START_LEVEL_TEST_BUNDLE),

				workingDirectory("target/paxexam"),
				// needed for PerClass strategy and I had problems running more test classes without cleaning
				// caches (timeout waiting for ProbeInvoker with particular UUID)
				cleanCaches(true),
				systemTimeout(60 * 60 * 1000),

				// set to "4" to see Felix wiring information
				frameworkProperty("felix.log.level").value("0"),

				// added implicitly by pax-exam, if pax.exam.system=test
				// these resources are provided inside org.ops4j.pax.exam:pax-exam-link-mvn jar
				// for example, "link:classpath:META-INF/links/org.ops4j.base.link" = "mvn:org.ops4j.base/ops4j-base/1.5.0"
				url("link:classpath:META-INF/links/org.ops4j.base.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
				url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.core.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
				url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.extender.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
				url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.framework.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
				url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.lifecycle.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
				url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.tracker.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
				url("link:classpath:META-INF/links/org.ops4j.pax.exam.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
				url("link:classpath:META-INF/links/org.ops4j.pax.exam.inject.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
				url("link:classpath:META-INF/links/org.ops4j.pax.extender.service.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),

				linkBundle("org.apache.servicemix.bundles.javax-inject").startLevel(START_LEVEL_SYSTEM_BUNDLES),

				junitBundles(),

				// configadmin is not needed. Useful for better configuration, but not needed - both by
				// pax-logging and pax-web
//				mavenBundle("org.apache.felix", "org.apache.felix.configadmin")
//						.versionAsInProject().startLevel(START_LEVEL_SYSTEM_BUNDLES),

				mavenBundle("org.ops4j.pax.logging", "pax-logging-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.logging", "pax-logging-log4j2")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.apache.felix", "org.apache.felix.metatype")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),

				// with PAXLOGGING-308 we can simply point to _native_ configuration file understood directly
				// by selected pax-logging backend
				systemProperty("org.ops4j.pax.logging.property.file").value("../etc/log4j2-osgi.properties"),

				frameworkProperty("org.osgi.service.http.port").value("8181")
		};

		Option[] loggingOptions = defaultLoggingConfig();
		Option[] infraOptions = combine(baseOptions, loggingOptions);
		Option[] paxWebCoreOptions = combine(infraOptions, paxWebCore());
		Option[] paxWebHttpServiceOptions = combine(paxWebCoreOptions, paxWebRuntime());
		Option[] paxWebTestOptions = combine(paxWebHttpServiceOptions, paxWebTestSupport());

		return combine(paxWebTestOptions/*...*/);
	}

	/**
	 * Reasonable defaults for default logging level (actually a threshold), framework logger level and usage
	 * of file-based default/fallback logger.
	 * @return
	 */
	protected Option[] defaultLoggingConfig() {
		String fileName = null;
		try {
			fileName = new File(LOG_DIR, getClass().getSimpleName() + ".log").getCanonicalPath();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage(), e);
		}

		return new Option[] {
				// every log with level higher or equal to INFO (i.e., not DEBUG) will be logged
				frameworkProperty(PaxLoggingConstants.LOGGING_CFG_DEFAULT_LOG_LEVEL).value("DEBUG"),
				// threshold for R7 Compendium 101.8 logging statements (from framework/bundle/service events)
				frameworkProperty(PaxLoggingConstants.LOGGING_CFG_FRAMEWORK_EVENTS_LOG_LEVEL).value("ERROR"),
				// default log will be written to file
				frameworkProperty(PaxLoggingConstants.LOGGING_CFG_USE_FILE_FALLBACK_LOGGER).value(fileName)
		};
	}

	// --- methods that add logical sets of bundles (or just single bundles) to pax-exam-container-native

	/**
	 * Installation of 3 Pax Web fundamental bundles: API, SPI + Servlet API.
	 * @return
	 */
	protected Option[] paxWebCore() {
		return new Option[] {
				mavenBundle("jakarta.annotation", "jakarta.annotation-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("jakarta.servlet", "jakarta.servlet-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web", "pax-web-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1).start(),
				mavenBundle("org.ops4j.pax.web", "pax-web-spi")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1).start()
		};
	}

	protected Option[] paxWebTestSupport() {
		return new Option[] {
				mavenBundle("org.ops4j.pax.web.itest", "pax-web-itest-container-common")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web.itest", "pax-web-itest-utils")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1)
		};
	}

	/**
	 * Installation of Pax Web Runtime - the <em>first</em> Pax Web bundle with an activator and implementation
	 * of {@link org.osgi.service.http.HttpService} (though it needs an instance of
	 * {@link org.ops4j.pax.web.service.spi.ServerControllerFactory} to actually register
	 * {@link org.osgi.service.http.HttpService}.
	 * @return
	 */
	protected Option paxWebRuntime() {
		return mavenBundle("org.ops4j.pax.web", "pax-web-runtime")
				.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1);
	}

	/**
	 * Installation of all the bundles required by {@code pax-web-jetty}
	 * @return
	 */
	protected Option[] paxWebJetty() {
		return new Option[] {
				mavenBundle("org.ops4j.pax.web", "pax-web-jetty")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web", "pax-web-jetty-servlet-compatibility")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1).noStart(),

				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-util").versionAsInProject(),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-io").versionAsInProject(),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-http").versionAsInProject(),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-server").versionAsInProject(),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-xml").versionAsInProject(),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-servlet").versionAsInProject(),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-security").versionAsInProject()
//				mavenBundle().groupId("org.eclipse.jetty")
//						.artifactId("jetty-continuation")
//						.version(asInProject()),
		};
	}

	/**
	 * Installation of all the bundles required by {@code pax-web-tomcat}
	 * @return
	 */
	protected Option[] paxWebTomcat() {
		return new Option[] {
				mavenBundle("org.ops4j.pax.web", "pax-web-tomcat-common")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web", "pax-web-tomcat")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("jakarta.security.auth.message", "jakarta.security.auth.message-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1)
		};
	}

	/**
	 * Installation of all the bundles required by {@code pax-web-undertow}
	 * @return
	 */
	protected Option[] paxWebUndertow() {
		return new Option[] {
				mavenBundle("org.jboss.xnio", "xnio-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.jboss.xnio", "xnio-nio")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("io.undertow", "undertow-core")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("io.undertow", "undertow-servlet")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web", "pax-web-undertow")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1)
		};
	}

	protected Option[] configAdmin() {
		return new Option[] {
				mavenBundle("org.apache.felix", "org.apache.felix.configadmin")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1)
		};
	}

	// --- helper methods to be used in all the tests

	/**
	 * Returns {@code mvn:} URI for standard Pax Web sample with version set as current version of Pax Web.
	 *
	 * @param artifactId
	 * @return
	 */
	protected String sampleURI(String artifactId) {
		return "mvn:org.ops4j.pax.web.samples/" + artifactId + "/" + VersionUtils.getProjectVersion();
	}

	protected Bundle installAndStartBundle(String uri) {
		try {
			final Bundle bundle = context.installBundle(uri);
			bundle.start();
			new WaitCondition("Starting bundle " + uri) {
				@Override
				protected boolean isFulfilled() {
					return bundle.getState() == Bundle.ACTIVE;
				}
			}.waitForCondition();
			return bundle;
		} catch (InterruptedException | BundleException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a listener for generic {@link ElementEvent} events with
	 * associated {@link org.ops4j.pax.web.itest.utils.WaitCondition} fulfilled after satisfying passed
	 * {@link java.util.function.BiPredicate} operating on single {@link ElementEvent}. This method sets up
	 * the listener, calls the passed {@code action} and waits for the condition that's satisfied according
	 * to passed {@code expectation}.
	 */
	protected void configureAndWait(Runnable action, final BiPredicate<ElementEvent.State, ElementEventData> expectation) {
		final List<ElementEvent> events = new CopyOnWriteArrayList<>();
		webElementListener = events::add;
		context.registerService(WebElementListener.class, webElementListener, null);

		action.run();

		try {
			new WaitCondition("Waiting for " + expectation) {
				@Override
				protected boolean isFulfilled() throws Exception {
					return events.stream().anyMatch(e -> expectation.test(e.getType(), e.getData()));
				}
			}.waitForCondition();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Creates a listener for deployment of named {@link javax.servlet.Servlet}.
	 * @param servletName
	 * @param action
	 */
	protected void configureAndWaitForNamedServlet(final String servletName, Action action) throws Exception {
		final List<ElementEvent> events = new CopyOnWriteArrayList<>();
		webElementListener = events::add;
		context.registerService(WebElementListener.class, webElementListener, null);

		action.run();

		try {
			new WaitCondition("Waiting for " + servletName + " servlet") {
				@Override
				protected boolean isFulfilled() throws Exception {
					return events.stream().anyMatch(e ->
							e.getType() == ElementEvent.State.DEPLOYED
									&& e.getData() instanceof ServletEventData
									&& ((ServletEventData) e.getData()).getServletName().equals(servletName));
				}
			}.waitForCondition();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Creates a listener for deployment of a {@link javax.servlet.Servlet} mapped to some URL.
	 * @param mapping
	 * @param action
	 */
	protected void configureAndWaitForServletWithMapping(final String mapping, Action action) throws Exception {
		final List<ElementEvent> events = new CopyOnWriteArrayList<>();
		webElementListener = events::add;
		context.registerService(WebElementListener.class, webElementListener, null);

		action.run();

		try {
			new WaitCondition("Waiting for servlet mapped to " + mapping) {
				@Override
				protected boolean isFulfilled() throws Exception {
					return events.stream().anyMatch(e ->
							e.getType() == ElementEvent.State.DEPLOYED
									&& e.getData() instanceof ServletEventData
									&& Arrays.asList(((ServletEventData) e.getData()).getUrlPatterns()).contains(mapping));
				}
			}.waitForCondition();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Creates a listener for deployment of a {@link javax.servlet.Filter} mapped to some URL.
	 * @param mapping
	 * @param action
	 */
	protected void configureAndWaitForFilterWithMapping(final String mapping, Action action) throws Exception {
		final List<ElementEvent> events = new CopyOnWriteArrayList<>();
		webElementListener = events::add;
		context.registerService(WebElementListener.class, webElementListener, null);

		action.run();

		try {
			new WaitCondition("Waiting for filter mapped to " + mapping) {
				@Override
				protected boolean isFulfilled() throws Exception {
					return events.stream().anyMatch(e ->
							e.getType() == ElementEvent.State.DEPLOYED
									&& e.getData() instanceof FilterEventData
									&& Arrays.asList(((FilterEventData) e.getData()).getUrlPatterns()).contains(mapping));
				}
			}.waitForCondition();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Performs an action and waits for {@link org.ops4j.pax.web.service.spi.model.events.ServerEvent} related
	 * to started container at given port
	 * @param port
	 * @param action
	 */
	protected void configureAndWaitForListener(int port, Action action) throws Exception {
		final List<ServerEvent> events = new CopyOnWriteArrayList<>();
		ServerListener listener = events::add;
		ServiceRegistration<ServerListener> reg = context.registerService(ServerListener.class, listener, null);

		action.run();

		try {
			new WaitCondition("Waiting for server listening at " + port) {
				@Override
				protected boolean isFulfilled() throws Exception {
					return events.stream().anyMatch(e ->
							e.getState() == ServerEvent.State.STARTED
									&& Arrays.stream(e.getAddresses()).map(InetSocketAddress::getPort)
									.anyMatch(p -> p == port));
				}
			}.waitForCondition();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			if (reg != null) {
				reg.unregister();
			}
		}
	}

	public static HttpService getHttpService(final BundleContext bundleContext) {
		ServiceTracker<HttpService, HttpService> tracker = new ServiceTracker<>(bundleContext, HttpService.class, null);
		tracker.open();
		try {
			return tracker.waitForService(5000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	public static WebContainer getWebContainer(final BundleContext bundleContext) {
		ServiceTracker<WebContainer, WebContainer> tracker = new ServiceTracker<>(bundleContext, WebContainer.class, null);
		tracker.open();
		try {
			return tracker.waitForService(5000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	/**
	 * Creates {@link javax.servlet.Servlet} init parameters with legacy name that can be (which is deprecated, but
	 * the only way with pure {@link HttpService} to specify servlet name) used to configure {@link javax.servlet.Servlet}
	 * name.
	 * @param servletName
	 * @return
	 */
	@SuppressWarnings("deprecation")
	protected Dictionary<?,?> legacyName(String servletName) {
		Dictionary<String, Object> initParams = new Hashtable<>();
		initParams.put(PaxWebConstants.INIT_PARAM_SERVLET_NAME, servletName);
		return initParams;
	}

	@FunctionalInterface
	public interface Action {
		void run() throws Exception;
	}

}
