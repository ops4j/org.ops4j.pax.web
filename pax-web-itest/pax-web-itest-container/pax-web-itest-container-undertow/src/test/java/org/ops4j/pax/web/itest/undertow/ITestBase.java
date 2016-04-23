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
 package org.ops4j.pax.web.itest.undertow;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.web.itest.base.AbstractTestBase;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.OptionUtils.combine;

@ExamReactorStrategy(PerMethod.class)
public class ITestBase extends AbstractTestBase {

	@Inject
	protected BundleContext bundleContext;


	public static Option[] configureBaseWithServlet() {
		return combine(
				baseConfigure(),
				mavenBundle().groupId("javax.servlet")
				.artifactId("javax.servlet-api").versionAsInProject());
	}

	public static Option[] configureUndertow() {
		return combine(
				configureBaseWithServlet(),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-undertow").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-runtime").version(asInProject()),

				mavenBundle().groupId("javax.annotation")
						.artifactId("javax.annotation-api").version(asInProject()),
				mavenBundle().groupId("org.jboss.logging")
						.artifactId("jboss-logging").version(asInProject()),
				mavenBundle()
						.groupId("org.ops4j.pax.tipi")
						.artifactId("org.ops4j.pax.tipi.xnio.api")
						.version(asInProject()),
				mavenBundle()
						.groupId("org.ops4j.pax.tipi")
						.artifactId("org.ops4j.pax.tipi.xnio.nio")
						.version(asInProject()),
				mavenBundle()
						.groupId("org.ops4j.pax.tipi")
						.artifactId("org.ops4j.pax.tipi.undertow.core")
						.version(asInProject()),
				mavenBundle()
						.groupId("org.ops4j.pax.tipi")
						.artifactId("org.ops4j.pax.tipi.undertow.servlet")
						.version(asInProject())
				);
	}
	
	public static Option[] configureUndertowBundle() {
		return combine(
				baseConfigure(),
				systemPackages("javax.xml.namespace;version=1.0.0","javax.transaction;version=1.1.0"),
				mavenBundle().groupId("org.ops4j.pax.web")
				.artifactId("pax-web-undertow-bundle").version(asInProject())
			);
	}
	
	public static Option[] configureWebSocketUndertow() {
	    return combine(
	                configureUndertow(),
	                mavenBundle().groupId("org.ops4j.pax.tipi")
                        .artifactId("org.ops4j.pax.tipi.undertow.websocket-jsr").version(asInProject())
	            );
	}


	@Override
	protected BundleContext getBundleContext() {
		return bundleContext;
	}
}
