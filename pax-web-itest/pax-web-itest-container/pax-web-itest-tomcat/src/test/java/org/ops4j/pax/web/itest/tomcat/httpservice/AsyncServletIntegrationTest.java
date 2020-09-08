/*
 * Copyright 2020 OPS4J.
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
package org.ops4j.pax.web.itest.tomcat.httpservice;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.container.httpservice.AbstractAsyncServletIntegrationTest;

import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
public class AsyncServletIntegrationTest extends AbstractAsyncServletIntegrationTest {

	@Configuration
	public Option[] configure() {
		return combine(baseConfigure(), paxWebTomcat());
	}

	/*
15:46:10.022 [FelixDispatchQueue] ERROR (NativeTestContainer.java:198) org.ops4j.pax.exam.nat.internal.NativeTestContainer - Framework ERROR event org.osgi.framework.FrameworkEvent[source=org.ops4j.pax.web.pax-web-tomcat [25]]
org.osgi.framework.BundleException: Unable to resolve org.ops4j.pax.web.pax-web-tomcat [25](R 25.0): missing requirement [org.ops4j.pax.web.pax-web-tomcat [25](R 25.0)] osgi.contract; (&(osgi.contract=JavaJASPIC)(version=1.1.0)) Unresolved requirements: [[org.ops4j.pax.web.pax-web-tomcat [25](R 25.0)] osgi.contract; (&(osgi.contract=JavaJASPIC)(version=1.1.0))]
	at org.apache.felix.framework.Felix.resolveBundleRevision(Felix.java:4149) ~[org.apache.felix.framework-5.6.12.jar:?]
	at org.apache.felix.framework.Felix.startBundle(Felix.java:2119) ~[org.apache.felix.framework-5.6.12.jar:?]
	at org.apache.felix.framework.Felix.setActiveStartLevel(Felix.java:1373) ~[org.apache.felix.framework-5.6.12.jar:?]
	at org.apache.felix.framework.FrameworkStartLevelImpl.run(FrameworkStartLevelImpl.java:308) ~[org.apache.felix.framework-5.6.12.jar:?]
	at java.lang.Thread.run(Thread.java:748) [?:1.8.0_261]
	 */

}
