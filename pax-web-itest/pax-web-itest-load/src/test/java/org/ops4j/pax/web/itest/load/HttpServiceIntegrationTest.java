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
 package org.ops4j.pax.web.itest.load;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.web.itest.base.VersionUtil;

import static org.ops4j.pax.exam.CoreOptions.*;

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
	            mavenBundle().groupId("org.eclipse.jetty").artifactId("jetty-util").version("9.2.6.v20141205"),
	            mavenBundle().groupId("org.eclipse.jetty").artifactId("jetty-io").version("9.2.6.v20141205"),
	            mavenBundle().groupId("org.eclipse.jetty").artifactId("jetty-http").version("9.2.6.v20141205"),
				mavenBundle().groupId("org.eclipse.jetty").artifactId("jetty-continuation").version("9.2.6.v20141205"),
				mavenBundle().groupId("org.eclipse.jetty").artifactId("jetty-server").version("9.2.6.v20141205"),
				mavenBundle().groupId("org.eclipse.jetty").artifactId("jetty-client").version("9.2.6.v20141205"),
				mavenBundle().groupId("org.eclipse.jetty").artifactId("jetty-security").version("9.2.6.v20141205"),
				mavenBundle().groupId("org.eclipse.jetty").artifactId("jetty-xml").version("9.2.6.v20141205"),
				mavenBundle().groupId("org.eclipse.jetty").artifactId("jetty-servlet").version("9.2.6.v20141205"),
				mavenBundle().groupId("javax.servlet").artifactId("javax.servlet-api").version("3.1.0"),
	            mavenBundle("org.osgi", "org.osgi.compendium", "5.0.0"),

	            mavenBundle().groupId("org.apache.xbean").artifactId("xbean-reflect").version("4.1"),
		    	mavenBundle().groupId("org.apache.xbean").artifactId("xbean-finder").version("4.1"),
		        mavenBundle().groupId("org.apache.xbean").artifactId("xbean-bundleutils").version("4.1"),
		        mavenBundle().groupId("org.ow2.asm").artifactId("asm-all").version("5.0.2"),
	            
				mavenBundle().groupId("org.ops4j.pax.logging").artifactId("pax-logging-service").version("1.7.4"),
				mavenBundle().groupId("org.ops4j.pax.logging").artifactId("pax-logging-api").version("1.7.4"),
		        
	            mavenBundle("org.slf4j", "slf4j-api", "1.6.4"),
	            mavenBundle("org.slf4j", "slf4j-simple", "1.6.4").noStart(),

	            mavenBundle().groupId("org.ops4j.pax.web.samples").artifactId("helloworld-hs").version(VersionUtil.getProjectVersion())

	        );
	}

}
