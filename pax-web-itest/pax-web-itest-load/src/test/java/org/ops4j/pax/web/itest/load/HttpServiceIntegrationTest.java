package org.ops4j.pax.web.itest.load;

import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.web.itest.base.VersionUtil;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */

@ExamReactorStrategy(PerClass.class)
public class HttpServiceIntegrationTest /*extends ITestBase*/ {

	@Configuration
	public static Option[] configure() {
		
		return options(systemProperty("org.osgi.service.http.port").value("8181"),
	            frameworkProperty("osgi.console").value("6666"),

	            mavenBundle("org.ops4j.pax.web.itest", "pax-web-itest-base").version(VersionUtil.getProjectVersion()),
	            mavenBundle("org.ops4j.pax.web", "pax-web-spi").version(VersionUtil.getProjectVersion()),
	            mavenBundle("org.ops4j.pax.web", "pax-web-api").version(VersionUtil.getProjectVersion()),
	            mavenBundle("org.ops4j.pax.web", "pax-web-extender-war").version(VersionUtil.getProjectVersion()),
	            mavenBundle("org.ops4j.pax.web", "pax-web-extender-whiteboard").version(VersionUtil.getProjectVersion()),
	            mavenBundle("org.ops4j.pax.web", "pax-web-jetty").version(VersionUtil.getProjectVersion()),
	            mavenBundle("org.ops4j.pax.web", "pax-web-runtime").version(VersionUtil.getProjectVersion()),
	            mavenBundle("org.ops4j.pax.web", "pax-web-jsp").version(VersionUtil.getProjectVersion()),
	            mavenBundle("org.eclipse.jdt.core.compiler", "ecj").version("4.2.2"),
	            mavenBundle("org.eclipse.jetty", "jetty-util").version("9.0.3.v20130506"),
	            mavenBundle("org.eclipse.jetty", "jetty-io").version("9.0.3.v20130506"),
	            mavenBundle("org.eclipse.jetty", "jetty-http").version("9.0.3.v20130506"),
	            mavenBundle("org.eclipse.jetty", "jetty-continuation").version("9.0.3.v20130506"),
	            mavenBundle("org.eclipse.jetty", "jetty-server").version("9.0.3.v20130506"),
	            mavenBundle("org.eclipse.jetty", "jetty-security").version("9.0.3.v20130506"),
	            mavenBundle("org.eclipse.jetty", "jetty-xml").version("9.0.3.v20130506"),
	            mavenBundle("org.eclipse.jetty", "jetty-servlet").version("9.0.3.v20130506"),
	            mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_3.0_spec").version("1.0"),
	            mavenBundle("org.osgi", "org.osgi.compendium", "4.3.0"),

	            mavenBundle().groupId("org.apache.xbean").artifactId("xbean-finder").version("3.12"),
	            mavenBundle().groupId("org.apache.xbean").artifactId("xbean-bundleutils").version("3.12"),
	            mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.asm").version("3.3_2"),
	            
	            mavenBundle("org.slf4j", "slf4j-api", "1.6.4"),
	            mavenBundle("org.slf4j", "slf4j-simple", "1.6.4").noStart(),

	            mavenBundle().groupId("org.ops4j.pax.web.samples").artifactId("helloworld-hs").version(VersionUtil.getProjectVersion())

	        );
	}

}
