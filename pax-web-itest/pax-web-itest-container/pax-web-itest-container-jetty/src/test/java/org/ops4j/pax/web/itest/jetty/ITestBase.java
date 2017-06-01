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
package org.ops4j.pax.web.itest.jetty;

import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.web.itest.base.AbstractControlledTestBase;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.io.File;

/**
 * Intermediate Base-Class until old base has been removed
 */
@ExamReactorStrategy(PerMethod.class)
public class ITestBase extends AbstractControlledTestBase {
    
    private static final Logger LOG = LoggerFactory.getLogger(ITestBase.class);
	
	@Inject
	protected BundleContext bundleContext;

	@Override
	protected BundleContext getBundleContext() {
		return bundleContext;
	}

	public static Option[] configureJetty() {
		return combine(
				baseConfigure(),
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
}
