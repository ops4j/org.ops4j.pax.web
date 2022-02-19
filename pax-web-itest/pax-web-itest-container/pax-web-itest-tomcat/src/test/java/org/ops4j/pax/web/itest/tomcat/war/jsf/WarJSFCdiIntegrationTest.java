/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.itest.tomcat.war.jsf;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.container.war.jsf.AbstractWarJSFCdiIntegrationTest;

import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackage;
import static org.ops4j.pax.exam.CoreOptions.systemPackage;
import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
public class WarJSFCdiIntegrationTest extends AbstractWarJSFCdiIntegrationTest {

	@Configuration
	public Option[] configure() {
		Option[] serverOptions = combine(baseConfigure(), paxWebTomcat());
		// myfaces-core-api doesn't import javax.inject and I'm getting
		// WELD-001111: interface javax.faces.annotation.FlowMap defined on org.apache.myfaces.cdi.JsfArtifactFlowMapProducer@2143ca58 is not a qualifier
		// if javax.inject is available only as bundle (== is excluded from maven-failsafe-plugin test classpath)
		Option[] tweakOptions1 = combine(serverOptions, bootDelegationPackage("javax.inject"));
		Option[] tweakOptions2 = combine(tweakOptions1, systemPackage("javax.inject;version=\"1.0\""));
		Option[] osgiOptions = combine(tweakOptions2, configAdmin());
		Option[] whiteboardOptions = combine(osgiOptions, paxWebExtenderWhiteboard());
		Option[] jspOptions = combine(whiteboardOptions, paxWebJsp());
		Option[] cdiOptions = combine(jspOptions, ariesCdiAndMyfaces());
		return combine(cdiOptions, paxWebExtenderWar());
	}

}
