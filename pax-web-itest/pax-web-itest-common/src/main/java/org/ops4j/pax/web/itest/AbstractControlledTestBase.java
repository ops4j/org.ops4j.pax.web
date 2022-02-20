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
package org.ops4j.pax.web.itest;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
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
import org.ops4j.pax.web.service.spi.model.events.FilterEventData;
import org.ops4j.pax.web.service.spi.model.events.ServerEvent;
import org.ops4j.pax.web.service.spi.model.events.ServerListener;
import org.ops4j.pax.web.service.spi.model.events.ServletEventData;
import org.ops4j.pax.web.service.spi.model.events.WebApplicationEvent;
import org.ops4j.pax.web.service.spi.model.events.WebApplicationEventListener;
import org.ops4j.pax.web.service.spi.model.events.WebElementEvent;
import org.ops4j.pax.web.service.spi.model.events.WebElementEventData;
import org.ops4j.pax.web.service.spi.model.events.WebElementEventListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
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
import static org.ops4j.pax.exam.CoreOptions.systemPackage;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.url;
import static org.ops4j.pax.exam.CoreOptions.workingDirectory;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.web.itest.utils.WaitCondition.RETRY_DURATION_MILLIS;
import static org.ops4j.pax.web.itest.utils.WaitCondition.SLEEP_DURATION_MILLIS;

/**
 * <p>Single base class for all Pax Exam integration tests. Subclasses may add specific helper methods.</p>
 *
 * <p>For now it's (almost) a duplicate of similar class from pax-web-itest-osgi, but the goal is to have one after
 * Pax Web 8 refactoring ends.</p>
 *
 * <p>{@link PerClass} strategy is needed. maven-failsafe-plugin's {@code reuseForks=false} and {@code forkCount=1} is
 * not enough to properly clean up JVM between methods and we may miss some URL handlers, etc. In other words - don't
 * use {@link org.ops4j.pax.exam.spi.reactors.PerMethod}.</p>
 *
 * <p>This class is part of a Maven module, which is just a jar, not a bundle. So it should be private-packaged
 * in any test-related bundle running in pax-exam-container-native/karaf.</p>
 */
@ExamReactorStrategy(PerClass.class)
public abstract class AbstractControlledTestBase {

	public static final Logger LOG = LoggerFactory.getLogger("org.ops4j.pax.web.itest");
	public static final String PROBE_SYMBOLIC_NAME = "PaxExam-Probe";

	// location of where pax-logging-api will have output file written according to
	// "org.ops4j.pax.logging.useFileLogFallback" system/context property
	// filename will match test class name with ".log" extension
	protected static final File LOG_DIR = new File("target/logs-default");

	@Rule
	public TestName testName = new TestName();

	@Inject
	protected BundleContext context;

	protected WebElementEventListener webElementEventListener;

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

		return new Option[] {
				// basic options
				bootDelegationPackage("sun.*"),
				bootDelegationPackage("com.sun.*"),
				systemPackage("sun.misc"),

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

				// this bundle provides correct osgi.contract;osgi.contract=JavaInject
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
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1)
		};
	}

	/**
	 * Reasonable defaults for default logging level (actually a threshold), framework logger level and usage
	 * of file-based default/fallback logger.
	 * @return
	 */
	protected Option[] defaultLoggingConfig() {
		String fileName;
		try {
			fileName = new File(LOG_DIR, getClass().getSimpleName() + ".log").getCanonicalPath();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage(), e);
		}

		return new Option[] {
				// every log with level higher or equal to DEBUG (i.e., not TRACE) will be logged
				frameworkProperty(PaxLoggingConstants.LOGGING_CFG_DEFAULT_LOG_LEVEL).value("DEBUG"),
				// threshold for R7 Compendium 101.8 logging statements (from framework/bundle/service events)
				frameworkProperty(PaxLoggingConstants.LOGGING_CFG_FRAMEWORK_EVENTS_LOG_LEVEL).value("ERROR"),
				// default log will be written to file
				frameworkProperty(PaxLoggingConstants.LOGGING_CFG_USE_FILE_FALLBACK_LOGGER).value(fileName)
		};
	}

	// --- methods that add logical sets of bundles (or just single bundles) to pax-exam-container-native

	/**
	 * Installation of 3 Pax Web fundamental bundles: API, SPI + Annotation API + Servlet API.
	 * @return
	 */
	protected Option[] paxWebCore() {
		return new Option[] {
				mavenBundle("jakarta.annotation", "jakarta.annotation-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web", "pax-web-compatibility-annotation13")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 2).noStart(),
				mavenBundle("jakarta.servlet", "jakarta.servlet-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web", "pax-web-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1).start(),
				mavenBundle("org.ops4j.pax.web", "pax-web-spi")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1).start()
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

	protected Option[] paxWebTestSupport() {
		return new Option[] {
				mavenBundle("org.ops4j.pax.web.itest", "pax-web-itest-container-common")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web.itest", "pax-web-itest-utils")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1)
		};
	}

	protected Option paxWebExtenderWhiteboard() {
		return mavenBundle("org.ops4j.pax.web", "pax-web-extender-whiteboard")
				.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1);
	}

	protected Option[] paxWebExtenderWar() {
		return new Option[] {
				mavenBundle("org.ops4j.pax.web", "pax-web-tomcat-common")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web", "pax-web-extender-war")
								.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				// pax-url-war requires osgi promise+function
				mavenBundle("org.osgi", "org.osgi.util.promise")
								.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.osgi", "org.osgi.util.function")
								.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.url", "pax-url-war").classifier("uber")
								.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1)
		};
	}

	protected Option[] paxWebJsp() {
		return new Option[] {
				mavenBundle("jakarta.el", "jakarta.el-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web", "pax-web-compatibility-el2")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 2).noStart(),
				mavenBundle("org.ops4j.pax.web", "pax-web-tomcat-common")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.eclipse.jdt", "ecj")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web", "pax-web-jsp")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1)
		};
	}

	/**
	 * Installation of all the bundles required by {@code pax-web-jetty}
	 * @return
	 */
	protected Option[] paxWebJetty() {
		return new Option[] {
				mavenBundle("org.ops4j.pax.web", "pax-web-jetty")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web", "pax-web-compatibility-servlet31")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1).noStart(),

				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-util").versionAsInProject(),
				// required since https://github.com/eclipse/jetty.project/issues/5539
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-util-ajax").versionAsInProject(),
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
						.artifactId("jetty-security").versionAsInProject(),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-continuation").versionAsInProject(),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-jaas").versionAsInProject()
		};
	}

	/**
	 * Installation of all the bundles required by {@code pax-web-jetty} with HTTP2 support
	 * @return
	 */
	protected Option[] paxWebJettyHttp2() {
		Option[] common = combine(paxWebJetty(),
				mavenBundle().groupId("org.eclipse.jetty.http2")
						.artifactId("http2-hpack").versionAsInProject(),
				mavenBundle().groupId("org.eclipse.jetty.http2")
						.artifactId("http2-common").versionAsInProject(),
				mavenBundle().groupId("org.eclipse.jetty.http2")
						.artifactId("http2-server").versionAsInProject(),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-alpn-server").versionAsInProject()
		);
		if (javaMajorVersion() >= 9) {
			return combine(common,
					mavenBundle().groupId("org.eclipse.jetty")
							.artifactId("jetty-alpn-java-server").versionAsInProject()
			);
		} else {
			return combine(common,
					mavenBundle().groupId("org.eclipse.jetty.alpn")
							.artifactId("alpn-api").versionAsInProject(),
					mavenBundle().groupId("org.eclipse.jetty")
							.artifactId("jetty-alpn-openjdk8-server").versionAsInProject()
			);
		}
	}

	protected Option[] jettyWebSockets() {
		return new Option[] {
				mavenBundle("jakarta.websocket", "jakarta.websocket-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-client").versionAsInProject(),
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("javax-websocket-client-impl").versionAsInProject(),
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("javax-websocket-server-impl").versionAsInProject(),
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("websocket-api").versionAsInProject(),
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("websocket-common").versionAsInProject(),
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("websocket-client").versionAsInProject(),
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("websocket-server").versionAsInProject(),
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("websocket-servlet").versionAsInProject(),
				mavenBundle("org.ops4j.pax.web", "pax-web-websocket")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1)
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

	protected Option[] tomcatWebSockets() {
		return new Option[] {
				mavenBundle("jakarta.websocket", "jakarta.websocket-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web", "pax-web-tomcat-websocket")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web", "pax-web-websocket")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1)
		};
	}

	/**
	 * Installation of all the bundles required by {@code pax-web-undertow}
	 * @return
	 */
	protected Option[] paxWebUndertow() {
		Option[] options = new Option[] {
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

		return options;
	}

	protected Option[] undertowWebSockets() {
		return new Option[] {
				mavenBundle("jakarta.websocket", "jakarta.websocket-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("io.undertow", "undertow-websockets-jsr")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web", "pax-web-undertow-websocket")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web", "pax-web-websocket")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1)
		};
	}

	protected Option[] configAdmin() {
		return new Option[] {
				mavenBundle("org.apache.felix", "org.apache.felix.configadmin")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1)
		};
	}

	protected Option[] eventAdmin() {
		return new Option[] {
				mavenBundle("org.apache.felix", "org.apache.felix.eventadmin")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1)
		};
	}

	protected Option[] scr() {
		return new Option[] {
				mavenBundle("org.apache.felix", "org.apache.felix.scr")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.osgi", "org.osgi.util.promise")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.osgi", "org.osgi.util.function")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1)
		};
	}

	protected Option[] jersey() {
		return new Option[] {
				mavenBundle("jakarta.validation", "jakarta.validation-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("com.sun.activation", "javax.activation")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.aopalliance")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("jakarta.xml.bind", "jakarta.xml.bind-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("jakarta.ws.rs", "jakarta.ws.rs-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.glassfish.hk2", "hk2-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.glassfish.hk2", "hk2-locator")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.glassfish.hk2", "hk2-utils")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.glassfish.hk2", "osgi-resource-locator")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.glassfish.jersey.core", "jersey-common")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.glassfish.jersey.core", "jersey-server")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.glassfish.jersey.core", "jersey-client")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.glassfish.jersey.containers", "jersey-container-servlet-core")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.glassfish.jersey.containers", "jersey-container-servlet")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.glassfish.jersey.inject", "jersey-hk2")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.javassist", "javassist")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1)
		};
	}

	protected Option[] myfacesDependencies() {
		Option[] options = new Option[] {
				mavenBundle("jakarta.websocket", "jakarta.websocket-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("jakarta.el", "jakarta.el-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web", "pax-web-compatibility-el2")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 2).noStart(),
				mavenBundle("jakarta.interceptor", "jakarta.interceptor-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web", "pax-web-compatibility-interceptor12")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 2).noStart(),
				// it has to be CDI 1.2 for Myfaces 2.3.x
				mavenBundle("jakarta.enterprise", "jakarta.enterprise.cdi-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				// but it's ok to have compatibility bundle
				mavenBundle("org.ops4j.pax.web", "pax-web-compatibility-cdi12")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 2).noStart(),

		};
		if (javaMajorVersion() >= 9) {
			return combine(options,
					mavenBundle("jakarta.xml.bind", "jakarta.xml.bind-api").versionAsInProject(),
					mavenBundle("com.sun.activation", "javax.activation").versionAsInProject()
			);
		}
		return options;
	}

	protected Option[] myfaces() {
		Option[] options = new Option[] {
				mavenBundle("jakarta.websocket", "jakarta.websocket-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("jakarta.el", "jakarta.el-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web", "pax-web-compatibility-el2")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 2).noStart(),
				mavenBundle("jakarta.interceptor", "jakarta.interceptor-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web", "pax-web-compatibility-interceptor12")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 2).noStart(),
				mavenBundle("jakarta.enterprise", "jakarta.enterprise.cdi-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1).noStart(),
				mavenBundle("org.ops4j.pax.web", "pax-web-compatibility-cdi12")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 2).noStart(),
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
		};
		if (javaMajorVersion() >= 9) {
			return combine(options,
					mavenBundle("jakarta.xml.bind", "jakarta.xml.bind-api").versionAsInProject(),
					mavenBundle("com.sun.activation", "javax.activation").versionAsInProject()
			);
		}
		return options;
	}

	protected Option[] primefaces() {
		return combine(myfaces(),
				mavenBundle("jakarta.persistence", "jakarta.persistence-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web", "pax-web-compatibility-jpa2")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 2).noStart(),
				mavenBundle("jakarta.xml.bind", "jakarta.xml.bind-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("com.sun.activation", "javax.activation")
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
				mavenBundle("org.primefaces", "primefaces")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1)
		);
	}

	protected Option jasypt() {
		return mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.jasypt")
				.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 2);
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

	/**
	 * Returns {@code mvn:} URI for standard Pax Web sample with version set as current version of Pax Web.
	 *
	 * @param artifactId
	 * @return
	 */
	protected String sampleWarURI(String artifactId) {
		return "mvn:org.ops4j.pax.web.samples/" + artifactId + "/" + VersionUtils.getProjectVersion() + "/war";
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
	 * Installs a bundle using {@code webbundle:} protocol
	 * @param artifactId
	 * @param contextPath
	 * @return
	 */
	protected Bundle installAndStartWebBundle(String artifactId, String contextPath) {
		String uri = String.format("webbundle:%s?%s=%s&%s=org.ops4j.pax.web.samples.%s",
				sampleWarURI(artifactId),
				PaxWebConstants.HEADER_CONTEXT_PATH, contextPath,
				Constants.BUNDLE_SYMBOLICNAME, artifactId);
		return installAndStartBundle(uri);
	}

	/**
	 * Installs a bundle using {@code webbundle:} protocol
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @param symbolicName
	 * @param contextPath
	 * @return
	 */
	protected Bundle installAndStartWebBundle(String groupId, String artifactId, String version,
			String symbolicName, String contextPath, Function<String, String> convertURI) {
		String uri = String.format("webbundle:mvn:%s/%s/%s/war?%s=%s&%s=%s",
				groupId, artifactId, version,
				PaxWebConstants.HEADER_CONTEXT_PATH, contextPath,
				Constants.BUNDLE_SYMBOLICNAME, symbolicName);
		return installAndStartBundle(convertURI == null ? uri : convertURI.apply(uri));
	}

	/**
	 * Get a bundle by symbolic name.
	 * @param symbolicName
	 * @return
	 */
	protected Bundle bundle(String symbolicName) {
		return Arrays.stream(context.getBundles())
				.filter(b -> symbolicName.equals(b.getSymbolicName())).findFirst().orElse(null);
	}

	/**
	 * Get a sample Bundle by its artifactId.
	 * @param sample
	 * @return
	 */
	protected Bundle sampleBundle(String sample) {
		return bundle("org.ops4j.pax.web.samples." + sample);
	}

	// --- helper methods to be used in all the tests

	/**
	 * Creates a listener for generic {@link WebElementEvent} events with
	 * associated {@link org.ops4j.pax.web.itest.utils.WaitCondition} fulfilled after satisfying passed
	 * {@link java.util.function.BiPredicate} operating on single {@link WebElementEvent}. This method sets up
	 * the listener, calls the passed {@code action} and waits for the condition that's satisfied according
	 * to passed {@code expectation}.
	 */
	protected void configureAndWait(Runnable action, final BiPredicate<WebElementEvent.State, WebElementEventData> expectation) {
		final List<WebElementEvent> events = new CopyOnWriteArrayList<>();
		webElementEventListener = events::add;
		ServiceRegistration<WebElementEventListener> reg
				= context.registerService(WebElementEventListener.class, webElementEventListener, null);

		action.run();

		try {
			new WaitCondition("Waiting for " + expectation) {
				@Override
				protected boolean isFulfilled() {
					return events.stream().anyMatch(e -> expectation.test(e.getType(), e.getData()));
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

	/**
	 * "configure and wait" method that allows to check entire stream of {@link WebElementEvent}s.
	 * @param action
	 * @param expectation
	 */
	protected void configureAndWait(Runnable action, final Predicate<List<WebElementEvent>> expectation) {
		final List<WebElementEvent> events = new CopyOnWriteArrayList<>();
		webElementEventListener = events::add;
		ServiceRegistration<WebElementEventListener> reg
				= context.registerService(WebElementEventListener.class, webElementEventListener, null);

		action.run();

		try {
			new WaitCondition("Waiting for " + expectation) {
				@Override
				protected boolean isFulfilled() {
					return expectation.test(events);
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

	/**
	 * Creates a listener for deployment of named {@link javax.servlet.Servlet}.
	 * @param servletName
	 * @param action
	 */
	protected void configureAndWaitForNamedServlet(final String servletName, Action action) throws Exception {
		final List<WebElementEvent> events = new CopyOnWriteArrayList<>();
		webElementEventListener = events::add;
		ServiceRegistration<WebElementEventListener> reg
				= context.registerService(WebElementEventListener.class, webElementEventListener, null);

		action.run();

		try {
			new WaitCondition("Waiting for " + servletName + " servlet") {
				@Override
				protected boolean isFulfilled() {
					return events.stream().anyMatch(e ->
							e.getType() == WebElementEvent.State.DEPLOYED
									&& e.getData() instanceof ServletEventData
									&& ((ServletEventData) e.getData()).getServletName().equals(servletName));
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

	/**
	 * Creates a listener for deployment of a {@link javax.servlet.Servlet} mapped to some URL.
	 * @param mapping
	 * @param action
	 */
	protected void configureAndWaitForServletWithMapping(final String mapping, Action action) throws Exception {
		final List<WebElementEvent> events = new CopyOnWriteArrayList<>();
		webElementEventListener = events::add;
		ServiceRegistration<WebElementEventListener> reg
				= context.registerService(WebElementEventListener.class, webElementEventListener, null);

		action.run();

		try {
			new WaitCondition("Waiting for servlet mapped to " + mapping) {
				@Override
				protected boolean isFulfilled() {
					return events.stream().anyMatch(e ->
							e.getType() == WebElementEvent.State.DEPLOYED
									&& e.getData() instanceof ServletEventData
									&& Arrays.asList(((ServletEventData) e.getData()).getUrlPatterns()).contains(mapping));
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

	/**
	 * Creates a listener for deployment of a {@link javax.servlet.Filter} mapped to some URL.
	 * @param mapping
	 * @param action
	 */
	protected void configureAndWaitForFilterWithMapping(final String mapping, Action action) throws Exception {
		final List<WebElementEvent> events = new CopyOnWriteArrayList<>();
		webElementEventListener = events::add;
		ServiceRegistration<WebElementEventListener> reg
				= context.registerService(WebElementEventListener.class, webElementEventListener, null);

		action.run();

		try {
			new WaitCondition("Waiting for filter mapped to " + mapping) {
				@Override
				protected boolean isFulfilled() {
					return events.stream().anyMatch(e ->
							e.getType() == WebElementEvent.State.DEPLOYED
									&& e.getData() instanceof FilterEventData
									&& Arrays.asList(((FilterEventData) e.getData()).getUrlPatterns()).contains(mapping));
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

	/**
	 * Performs an action and waits for {@link org.ops4j.pax.web.service.spi.model.events.WebApplicationEvent} related
	 * to started WAB
	 * @param action
	 */
	protected void configureAndWaitForDeployment(Action action) throws Exception {
		configureAndWaitForDeployment(action, RETRY_DURATION_MILLIS);
	}

	/**
	 * Performs an action and waits for {@link org.ops4j.pax.web.service.spi.model.events.WebApplicationEvent} related
	 * to started WAB
	 * @param action
	 * @param timeoutInMs
	 */
	protected void configureAndWaitForDeployment(Action action, long timeoutInMs) throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		WebApplicationEventListener listener = event -> {
			if (event.getType() == WebApplicationEvent.State.DEPLOYED) {
				latch.countDown();
			}
		};
		ServiceRegistration<WebApplicationEventListener> reg
				= context.registerService(WebApplicationEventListener.class, listener, null);

		action.run();

		try {
			new WaitCondition("Waiting for deployment") {
				@Override
				protected boolean isFulfilled() {
					return latch.getCount() == 0L;
				}
			}.waitForCondition(timeoutInMs, SLEEP_DURATION_MILLIS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			if (reg != null) {
				reg.unregister();
			}
		}
	}

	/**
	 * Performs an action and waits for {@link org.ops4j.pax.web.service.spi.model.events.WebApplicationEvent} related
	 * to started WAB, but only if the provided sample bundle is not ACTIVE.
	 * @param sample
	 * @param action
	 */
	protected Bundle configureAndWaitForDeploymentUnlessInstalled(String sample, Action action) throws Exception {
		Bundle b = sampleBundle(sample);
		if (b != null && b.getState() == Bundle.ACTIVE) {
			return b;
		}

		configureAndWaitForDeployment(action);

		return sampleBundle(sample);
	}

	/**
	 * Performs an action and waits for {@link org.ops4j.pax.web.service.spi.model.events.ServerEvent} related
	 * to started container at given port
	 * @param port
	 * @param actions
	 */
	protected void configureAndWaitForListener(int port, Action ... actions) throws Exception {
		final List<ServerEvent> events = new CopyOnWriteArrayList<>();
		ServerListener listener = events::add;
		ServiceRegistration<ServerListener> reg = context.registerService(ServerListener.class, listener, null);

		if (actions != null) {
			for (Action a : actions) {
				a.run();
			}
		}

		try {
			new WaitCondition("Waiting for server listening at " + port) {
				@Override
				protected boolean isFulfilled() {
					return events.stream().anyMatch(e ->
							e.getState() == ServerEvent.State.STARTED
									&& Arrays.stream(e.getAddresses()).map(a -> a.getAddress().getPort())
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

	/**
	 * Checks whether an event is for {@link org.ops4j.pax.web.service.spi.model.elements.ElementModel} registration
	 * to all the passed context names.
	 * @param data
	 * @param names
	 * @return
	 */
	public static boolean usesContexts(WebElementEventData data, String ... names) {
		Set<String> used = new HashSet<>(data.getContextNames());
		return used.containsAll(Arrays.asList(names));
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

	protected int javaMajorVersion() {
		String v = System.getProperty("java.specification.version");
		if (v.contains(".")) {
			// before Java 9
			v = v.split("\\.")[1];
		}
		return Integer.parseInt(v);
	}

	@FunctionalInterface
	public interface Action {
		void run() throws Exception;
	}

}
