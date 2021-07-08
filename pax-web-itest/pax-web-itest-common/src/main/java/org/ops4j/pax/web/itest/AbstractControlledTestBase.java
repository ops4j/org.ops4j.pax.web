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
import java.util.function.Function;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
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
		String fileName = null;
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
		return new Option[] {
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
	}

	protected Option[] myfaces() {
		return new Option[] {
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
	}

	protected Option[] primefaces() {
		return combine(myfaces(),
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

	protected Option[] ariesCdiAndMyfaces() {
		return new Option[]{
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
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.jboss.weld", "weld-osgi-bundle")
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
				PaxWebConstants.CONTEXT_PATH_KEY, contextPath,
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
				PaxWebConstants.CONTEXT_PATH_KEY, contextPath,
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

}
