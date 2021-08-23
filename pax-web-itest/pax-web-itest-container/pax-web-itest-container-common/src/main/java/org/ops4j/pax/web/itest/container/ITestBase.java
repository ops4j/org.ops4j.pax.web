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
package org.ops4j.pax.web.itest.container;

/**
 * Base class for Integration test related tests, designed to work with <code>pax.exam.system=default</code>
 */
public abstract class ITestBase /*extends AbstractControlledTestBase*/ {
//
//	public static Option[] configureJettyBundle() {
//		return combine(
//				baseConfigure(),
//				systemPackages("javax.xml.namespace;version=1.0.0","javax.transaction;version=1.1.0"),
//				mavenBundle().groupId("org.ops4j.pax.web")
//				.artifactId("pax-web-jetty-bundle").version(asInProject())
//			);
//	}
//
//	public static Option[] configureSpdyJetty() {
//
//	    String alpnBoot = System.getProperty("alpn-boot");
//        if (alpnBoot == null) {
//            throw new IllegalStateException("Define path to alpn boot jar as system property -Dmortbay-alpn-boot");
//        }
//        File checkALPNBoot = new File(alpnBoot);
//        if (!checkALPNBoot.exists()) {
//            throw new IllegalStateException("Unable to find the alpn boot jar here: " + alpnBoot);
//        }
//
//        LOG.warn("found alpn: {}", alpnBoot);
//
//		return combine(
//				configureJetty(),
//				    CoreOptions.vmOptions("-Xbootclasspath/p:" + alpnBoot),
//				    mavenBundle().groupId("org.eclipse.jetty.osgi").artifactId("jetty-osgi-alpn").version(asInProject()).noStart(),
//					mavenBundle().groupId("org.eclipse.jetty").artifactId("jetty-alpn-server").version(asInProject()),
//                   mavenBundle().groupId("org.eclipse.jetty.http2")
//                       .artifactId("http2-common").version(asInProject()),
//                   mavenBundle().groupId("org.eclipse.jetty.http2")
//                           .artifactId("http2-hpack").version(asInProject()),
//                   mavenBundle().groupId("org.eclipse.jetty.http2")
//                           .artifactId("http2-server").version(asInProject())
//				);
//	}

}
