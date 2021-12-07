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
package org.ops4j.pax.web.service.undertow.configuration.model;

import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Test;
import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.undertow.internal.configuration.ResolvingContentHandler;
import org.ops4j.pax.web.service.undertow.internal.configuration.UnmarshallingContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class ConfigurationParserTest {

	public static final Logger LOG = LoggerFactory.getLogger(ConfigurationParserTest.class);

	@Test
	public void model() throws Exception {
		LOG.info("Unmarshall undertow-default-template-1.1.xml");

		Map<String, String> pid = new HashMap<>();
		pid.put(PaxWebConfig.PID_CFG_HTTP_PORT, "8123");
		pid.put(PaxWebConfig.PID_CFG_HTTP_PORT_SECURE, "8423");
		pid.put("karaf.etc", "/data/tmp");
		pid.put("karaf.data", "/data/tmp/data");
		pid.put("http.read.timeout", "5000");
		pid.put("http.write.timeout", "10000");

		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
		XMLReader xmlReader = spf.newSAXParser().getXMLReader();
		UnmarshallingContentHandler unmarshallerHandler = new UnmarshallingContentHandler();
		xmlReader.setContentHandler(new ResolvingContentHandler(pid, unmarshallerHandler));
		xmlReader.parse(new InputSource(getClass().getResourceAsStream("/templates/undertow-default-template-1.1.xml")));

		//Configuration cfg = (Configuration) unmarshaller.unmarshal(source);
		UndertowConfiguration cfg = unmarshallerHandler.getConfiguration();
		cfg.init();
		LOG.info("Configuration: {}", cfg);

		assertThat(cfg.getSocketBindings().get(0).getPort(), equalTo(8123));
		assertThat(cfg.getSocketBindings().get(0).getName(), equalTo("http"));
		assertThat(cfg.getSocketBindings().get(1).getPort(), equalTo(8423));
		assertThat(cfg.getSocketBindings().get(1).getName(), equalTo("https"));

		assertThat(cfg.getSecurityRealms().get(1).getIdentities().getSsl().getKeystore().getPath(),
				equalTo("/data/tmp/certs/server.keystore"));
	}

}
