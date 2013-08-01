package org.ops4j.pax.web.itest.load;

import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;
import org.ops4j.pax.web.itest.base.VersionUtil;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */

@ExamReactorStrategy(EagerSingleStagedReactorFactory.class)
public class HttpServiceIntegrationTest /*extends ITestBase*/ {

	@Configuration
	public static Option[] configure() {
		
		return options(systemProperty("org.osgi.service.http.port").value("8181"),
	            frameworkProperty("osgi.console").value("6666"),

//	            mavenBundle("org.ops4j.pax.web.itest", "pax-web-itest-base").versionAsInProject(),
	            mavenBundle("org.ops4j.pax.web", "pax-web-spi").versionAsInProject(),
	            mavenBundle("org.ops4j.pax.web", "pax-web-api").versionAsInProject(),
	            mavenBundle("org.ops4j.pax.web", "pax-web-extender-war").versionAsInProject(),
	            mavenBundle("org.ops4j.pax.web", "pax-web-extender-whiteboard").versionAsInProject(),
	            mavenBundle("org.ops4j.pax.web", "pax-web-jetty").versionAsInProject(),
	            mavenBundle("org.ops4j.pax.web", "pax-web-runtime").versionAsInProject(),
	            mavenBundle("org.ops4j.pax.web", "pax-web-jsp").versionAsInProject(),
	            mavenBundle("org.eclipse.jdt.core.compiler", "ecj").versionAsInProject(),
	            mavenBundle("org.eclipse.jetty", "jetty-util").version("8.1.4.v20120524"),
	            mavenBundle("org.eclipse.jetty", "jetty-io").version("8.1.4.v20120524"),
	            mavenBundle("org.eclipse.jetty", "jetty-http").version("8.1.4.v20120524"),
	            mavenBundle("org.eclipse.jetty", "jetty-continuation").version("8.1.4.v20120524"),
	            mavenBundle("org.eclipse.jetty", "jetty-server").version("8.1.4.v20120524"),
	            mavenBundle("org.eclipse.jetty", "jetty-security").version("8.1.4.v20120524"),
	            mavenBundle("org.eclipse.jetty", "jetty-xml").version("8.1.4.v20120524"),
	            mavenBundle("org.eclipse.jetty", "jetty-servlet").version("8.1.4.v20120524"),
	            mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_3.0_spec").version("1.0"),
	            mavenBundle("org.osgi", "org.osgi.compendium", "4.3.0"),

	            mavenBundle("org.slf4j", "slf4j-api", "1.6.4"),
	            mavenBundle("org.slf4j", "slf4j-simple", "1.6.4").noStart(),

	            mavenBundle().groupId("org.ops4j.pax.web.samples").artifactId("helloworld-hs").version(VersionUtil.getProjectVersion())

	        );
	}

}
