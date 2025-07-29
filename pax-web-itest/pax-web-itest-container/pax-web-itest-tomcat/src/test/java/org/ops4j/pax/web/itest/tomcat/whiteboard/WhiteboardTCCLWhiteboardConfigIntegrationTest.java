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
package org.ops4j.pax.web.itest.tomcat.whiteboard;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.container.whiteboard.AbstractWhiteboardTCCLConfigIntegrationTest;

import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
public class WhiteboardTCCLWhiteboardConfigIntegrationTest extends AbstractWhiteboardTCCLConfigIntegrationTest {

	@Configuration
	public Option[] configure() {
		Option[] serverOptions = combine(baseConfigure(), paxWebTomcat());
		// we add configadmin after pax-web-runtime
		// 27 -> {org.apache.felix.framework.BundleImpl@4076} "org.ops4j.pax.web.pax-web-runtime [27]"
		// 28 -> {org.apache.felix.framework.BundleImpl@4585} "org.ops4j.pax.web.pax-web-tomcat [28]"
		// 29 -> {org.apache.felix.framework.BundleImpl@4587} "jakarta.security.auth.message-api [29]"
		// 30 -> {org.apache.felix.framework.BundleImpl@4589} "org.apache.felix.configadmin [30]"
		// 31 -> {org.apache.felix.framework.BundleImpl@4591} "org.ops4j.pax.web.pax-web-extender-whiteboard [31]"
		// 32 -> {org.apache.felix.framework.BundleImpl@4593} "PAXEXAM-PROBE-f5422f17-b2cb-4399-8760-42eff0784bf3 [32]"
		//
		// org.ops4j.pax.web.service.internal.PaxWebManagedService will be registered before configadmin
		// starts and thanks to trackInitial, this managed service will receive initial (null) configuration
		// before a configuration updated by AbstractWhiteboardTCCLConfigIntegrationTest.setUp()
		// but even if that's the case, we should not rely on this
		Option[] cmOptions = combine(serverOptions, configAdmin());
		return combine(cmOptions, paxWebExtenderWhiteboard());
	}

	@Override
	protected String getTCCLType() {
		return "whiteboard";
	}

}
