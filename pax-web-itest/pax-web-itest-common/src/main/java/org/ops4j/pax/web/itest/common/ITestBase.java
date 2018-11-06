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
package org.ops4j.pax.web.itest.common;

import javax.inject.Inject;

import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.web.itest.base.AbstractControlledTestBase;
import org.osgi.framework.BundleContext;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.io.File;

/**
 * Base class for Integration test related tests, designed to work with <code>pax.exam.system=default</code>
 */
public abstract class ITestBase extends AbstractControlledTestBase {

	@Inject
	protected BundleContext bundleContext;

	public static Option[] configureJetty() {
		return combine(
				baseConfigure(),
				mavenBundle().groupId("org.ops4j.pax.web.itest").artifactId("pax-web-itest-common").versionAsInProject(),

				// previously used for all containers - as jetty-client
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-http").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-util").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-io").version(asInProject()),

				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-jetty").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-runtime").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-continuation")
						.version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-server").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-security").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-xml").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty")
						.artifactId("jetty-servlet").version(asInProject()));
	}

	public static Option[] configureJettyBundle() {
		return combine(
				baseConfigure(),
				mavenBundle().groupId("org.ops4j.pax.web.itest").artifactId("pax-web-itest-common").versionAsInProject(),
				systemPackages("javax.xml.namespace;version=1.0.0","javax.transaction;version=1.1.0"),
				mavenBundle().groupId("org.ops4j.pax.web")
				.artifactId("pax-web-jetty-bundle").version(asInProject())
			);
	}

	public static Option[] configureSpdyJetty() {
	    
	    String alpnBoot = System.getProperty("alpn-boot");
        if (alpnBoot == null) { 
            throw new IllegalStateException("Define path to alpn boot jar as system property -Dmortbay-alpn-boot"); 
        }
        File checkALPNBoot = new File(alpnBoot);
        if (!checkALPNBoot.exists()) { 
            throw new IllegalStateException("Unable to find the alpn boot jar here: " + alpnBoot); 
        }
        
        LOG.warn("found alpn: {}", alpnBoot);
	    
		return combine(
				configureJetty(),
				    CoreOptions.vmOptions("-Xbootclasspath/p:" + alpnBoot),
				    mavenBundle().groupId("org.eclipse.jetty.osgi").artifactId("jetty-osgi-alpn").version(asInProject()).noStart(),
					mavenBundle().groupId("org.eclipse.jetty").artifactId("jetty-alpn-server").version(asInProject()),
                   mavenBundle().groupId("org.eclipse.jetty.http2")
                       .artifactId("http2-common").version(asInProject()),
                   mavenBundle().groupId("org.eclipse.jetty.http2")
                           .artifactId("http2-hpack").version(asInProject()),
                   mavenBundle().groupId("org.eclipse.jetty.http2")
                           .artifactId("http2-server").version(asInProject())
				);
	}

	public static Option[] configureWebSocketJetty() {
		return combine(
				configureJetty(),
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("websocket-server").version(asInProject()),
						
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("websocket-client").version(asInProject()),

				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("websocket-common").version(asInProject()),
						
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("websocket-servlet").version(asInProject()),
						
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("websocket-api").version(asInProject()),
						
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("javax-websocket-server-impl").version(asInProject()),

				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("javax-websocket-client-impl").version(asInProject()),
						
				mavenBundle().groupId("org.glassfish").artifactId("javax.json")
						.versionAsInProject(),

				mavenBundle().groupId("javax.json")
						.artifactId("javax.json-api").versionAsInProject(),
						
				mavenBundle().groupId("org.apache.aries").artifactId("org.apache.aries.util").versionAsInProject(),
				mavenBundle().groupId("org.apache.aries.spifly").artifactId("org.apache.aries.spifly.dynamic.bundle").versionAsInProject()

				);
	}

	public static Option[] configureTomcat() {
		return combine(
				baseConfigure(),
				systemProperty("catalina.base").value("target"),
				mavenBundle().groupId("org.ops4j.pax.web.itest").artifactId("pax-web-itest-common").versionAsInProject(),

				systemPackages("javax.xml.namespace;version=1.0.0","javax.transaction;version=1.1.0"),
				mavenBundle().groupId("javax.annotation").artifactId("javax.annotation-api").versionAsInProject(),

				mavenBundle().groupId("org.ops4j.pax.web").artifactId("pax-web-runtime").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web").artifactId("pax-web-tomcat").version(asInProject()),

				mavenBundle().groupId("org.ops4j.pax.tipi").artifactId("org.ops4j.pax.tipi.tomcat-embed-core").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.tipi").artifactId("org.ops4j.pax.tipi.tomcat-embed-websocket").version(asInProject()),

				mavenBundle().groupId("org.apache.servicemix.specs").artifactId("org.apache.servicemix.specs.saaj-api-1.3").version(asInProject()),
				mavenBundle().groupId("org.apache.servicemix.specs").artifactId("org.apache.servicemix.specs.jaxb-api-2.2").version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-jaxws_2.2_spec").version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-jaxrpc_1.1_spec").version(asInProject()),
				/*
				mavenBundle().groupId("javax.websocket")
						.artifactId("javax.websocket-api")
						.versionAsInProject(),
				*/
				mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-jta_1.1_spec").versionAsInProject(),
				mavenBundle().groupId("org.apache.servicemix.specs").artifactId("org.apache.servicemix.specs.jsr303-api-1.0.0").version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-activation_1.1_spec").version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-stax-api_1.2_spec").version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-ejb_3.1_spec").version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-jpa_2.0_spec").version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-javamail_1.4_spec").version(asInProject()),
				// mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-osgi-registry").version(asInProject()),
				mavenBundle().groupId("javax.security.auth.message").artifactId("javax.security.auth.message-api").version(asInProject()),
				systemProperty("org.ops4j.pax.url.war.importPaxLoggingPackages").value("true"),
				systemProperty("org.ops4j.pax.web.log.ncsa.directory").value("logs")
		);
	}

	public static Option[] configureUndertow() {
		return combine(
				baseConfigure(),
				mavenBundle().groupId("org.ops4j.pax.web.itest").artifactId("pax-web-itest-common").versionAsInProject(),

				mavenBundle().groupId("javax.annotation").artifactId("javax.annotation-api").version(asInProject()),

				mavenBundle().groupId("org.ops4j.pax.web").artifactId("pax-web-runtime").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web").artifactId("pax-web-undertow").version(asInProject()),

				mavenBundle().groupId("org.jboss.xnio").artifactId("xnio-api").version(asInProject()),
				mavenBundle().groupId("org.jboss.xnio").artifactId("xnio-nio").version(asInProject()),
				mavenBundle().groupId("io.undertow").artifactId("undertow-core").version(asInProject()),
				mavenBundle().groupId("io.undertow").artifactId("undertow-servlet").version(asInProject())
		);
	}

	public static Option[] configureWebSocketUndertow() {
		return combine(
				configureUndertow(),
				mavenBundle().groupId("io.undertow").artifactId("undertow-websockets-jsr").version(asInProject())
		);
	}

	public static Option[] configureJersey() {
		return OptionUtils.expand(
				mavenBundle().groupId("com.sun.jersey").artifactId("jersey-core").version("1.19"),
				mavenBundle().groupId("com.sun.jersey").artifactId("jersey-server").version("1.19"),
				mavenBundle().groupId("com.sun.jersey").artifactId("jersey-servlet").version("1.19"),
				mavenBundle().groupId("javax.ws.rs").artifactId("jsr311-api").version("1.1.1"));
	}

	@Override
	protected BundleContext getBundleContext() {
		return bundleContext;
	}

}
