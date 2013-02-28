package org.ops4j.pax.web.itest.load;

import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.web.itest.ITestBase;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public class HttpServiceIntegrationTest extends ITestBase {

	@Configuration
	public static Option[] configure() {
		System.setProperty("ProjectVersion", "3.0.0-SNAPSHOT");
		
//		return combine(configureJetty(),
//				mavenBundle().groupId("org.ops4j.pax.web.samples")
//						.artifactId("helloworld-hs").version("3.0.0-SNAPSHOT")
//				);
		
		return options(systemProperty("org.osgi.service.http.port").value("8181"),
	            frameworkProperty("osgi.console").value("6666"),

	            mavenBundle("org.ops4j.pax.web", "pax-web-spi").version("2.0.2"),
	            mavenBundle("org.ops4j.pax.web", "pax-web-api").version("2.0.2"),
	            mavenBundle("org.ops4j.pax.web", "pax-web-extender-war").version("2.0.2"),
	            mavenBundle("org.ops4j.pax.web", "pax-web-extender-whiteboard").version("2.0.2"),
	            mavenBundle("org.ops4j.pax.web", "pax-web-jetty").version("2.0.2"),
	            mavenBundle("org.ops4j.pax.web", "pax-web-runtime").version("2.0.2"),
	            mavenBundle("org.ops4j.pax.web", "pax-web-jsp").version("2.0.2"),
	            mavenBundle("org.eclipse.jdt.core.compiler", "ecj").version("3.5.1"),
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

	            mavenBundle().groupId("org.ops4j.pax.web.samples").artifactId("helloworld-hs").version("3.0.0-SNAPSHOT")

	        );
	}
	
	

}
