/*
 * Copyright 2019 OPS4J.
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
package org.ops4j.pax.web.itest.osgi;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.logging.PaxLoggingConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
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
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.url;
import static org.ops4j.pax.exam.CoreOptions.workingDirectory;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * Base class for all integration tests - manually sets up pax-exam configuration (without implicit configuration).
 * {@link PerClass} strategy is needed. maven-failsafe-plugin's {@code reuseForks=false} and {@code forkCount=1} is
 * not enough to properly clean up JVM between methods and we may miss some URL handlers, etc. In other words - don't
 * use {@link org.ops4j.pax.exam.spi.reactors.PerMethod}.
 */
@ExamReactorStrategy(PerClass.class)
public class AbstractControlledBase2 {

	public static final Logger LOG = LoggerFactory.getLogger(AbstractControlledBase2.class);
	public static final String PROBE_SYMBOLIC_NAME = "PaxExam-Probe";

	// location of where pax-logging-api will have output file written according to
	// "org.ops4j.pax.logging.useFileLogFallback" system/context property
	// filename will match test class name with ".log" extension
	static final File LOG_DIR = new File("target/logs-default");

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

		Option[] baseOptions = new Option[] {
				// basic options
				bootDelegationPackage("com.sun.*"),

				frameworkStartLevel(START_LEVEL_TEST_BUNDLE),

				workingDirectory("target/paxexam"),
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

//				mavenBundle("org.apache.felix", "org.apache.felix.configadmin")
//						.versionAsInProject().startLevel(START_LEVEL_SYSTEM_BUNDLES),

				mavenBundle("org.ops4j.pax.logging", "pax-logging-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.logging", "pax-logging-log4j2")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1)
		};

		Option[] loggingOptions = defaultLoggingConfig();

		return combine(baseOptions, loggingOptions);
	}

	/**
	 * Installation of 3 Pax Web fundamental bundles: API, SPI + javax.servlet-api.
	 * @return
	 */
	protected Option[] paxWebCore() {
		return new Option[] {
				mavenBundle("jakarta.servlet", "jakarta.servlet-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ops4j.pax.web", "pax-web-api")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1).start(),
				mavenBundle("org.ops4j.pax.web", "pax-web-spi")
						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1).start()

//				// requirement from pax-web-spi
//				mavenBundle("org.apache.xbean", "xbean-finder")
//						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
//				// xbean-finder requires xbean-bundleutils
//				mavenBundle("org.apache.xbean", "xbean-bundleutils")
//						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
//				// xbean-finder requires asm and asm-commons, and asm-commons requires asm-tree
//				mavenBundle("org.ow2.asm", "asm")
//						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
//				mavenBundle("org.ow2.asm", "asm-commons")
//						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
//				mavenBundle("org.ow2.asm", "asm-tree")
//						.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
		};
	}

	/**
	 * Installation of Pax Web Runtime - the <em>first</em> Pax Web bundle with an activator.
	 * @return
	 */
	protected Option paxWebRuntime() {
		return mavenBundle("org.ops4j.pax.web", "pax-web-runtime")
				.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1);
	}

	protected Option metaTypeService() {
		return mavenBundle("org.apache.felix", "org.apache.felix.metatype")
				.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1);
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

	/**
	 * Configuring symbolic name in test probe we can easily locate related log entries in the output.
	 * @param builder
	 * @return
	 */
	@ProbeBuilder
	public TestProbeBuilder probeBuilder(TestProbeBuilder builder) {
		builder.setHeader(Constants.BUNDLE_SYMBOLICNAME, PROBE_SYMBOLIC_NAME);
		return builder;
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

}
