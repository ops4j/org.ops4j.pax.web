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
 package org.ops4j.pax.web.itest.tomcat;

import org.apache.catalina.Globals;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.web.itest.base.AbstractTestBase;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;

import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.OptionUtils.combine;

@ExamReactorStrategy(PerClass.class)
public class ITestBase extends AbstractTestBase {

	@Inject
	protected BundleContext bundleContext;

	public Option[] configureBaseWithServlet() {
		return combine(
				baseConfigure(),
				systemProperty("org.osgi.service.http.port").value("8282"),
				systemProperty("org.ops4j.pax.url.mvn.certificateCheck").value("false"),
				systemProperty("javax.servlet.context.tempdir").value("target"),
				systemProperty("org.ops4j.pax.web.log.ncsa.directory").value("logs"),
				systemPackages("javax.xml.namespace;version=1.0.0"),
				systemProperty(Globals.CATALINA_BASE_PROP).value("target"),
				mavenBundle().groupId("javax.servlet").artifactId("javax.servlet-api").versionAsInProject());
	}



	public Option[] configureTomcat() {
		return combine(
				configureBaseWithServlet(),
				mavenBundle().groupId("org.ops4j.pax.web")
				.artifactId("pax-web-runtime").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-tomcat").version(asInProject()),
			
				mavenBundle().groupId("javax.annotation").artifactId("javax.annotation-api").versionAsInProject(),

				mavenBundle().groupId("org.ops4j.pax.tipi")
						.artifactId("org.ops4j.pax.tipi.tomcat-embed-core")
						.version(asInProject()),

				mavenBundle()
						.groupId("org.ops4j.pax.tipi")
						.artifactId(
								"org.ops4j.pax.tipi.tomcat-embed-logging-juli")
						.version(asInProject()),

				mavenBundle().groupId("org.ops4j.pax.tipi")
						.artifactId("org.ops4j.pax.tipi.tomcat-embed-websocket")
						.version(asInProject()),

				mavenBundle().groupId("org.apache.servicemix.specs")
						.artifactId("org.apache.servicemix.specs.saaj-api-1.3")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.servicemix.specs")
						.artifactId("org.apache.servicemix.specs.jaxb-api-2.2")
						.version(asInProject()),

				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-jaxws_2.2_spec")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-jaxrpc_1.1_spec")
						.version(asInProject()),

				mavenBundle().groupId("javax.websocket")
						.artifactId("javax.websocket-api")
						.versionAsInProject(),
						
				mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-jta_1.1_spec").versionAsInProject(),		
				
				mavenBundle()
						.groupId("org.apache.servicemix.specs")
						.artifactId(
								"org.apache.servicemix.specs.jsr303-api-1.0.0")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-activation_1.1_spec")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-stax-api_1.2_spec")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-ejb_3.1_spec")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-jpa_2.0_spec")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-javamail_1.4_spec")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-osgi-registry")
						.version(asInProject())
						);
	}
	

	@Override
	protected BundleContext getBundleContext() {
		return bundleContext;
	}

}
