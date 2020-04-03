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
package org.ops4j.pax.web.service.undertow.internal.configuration;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshallerHandler;
import javax.xml.parsers.SAXParserFactory;

import org.junit.BeforeClass;
import org.junit.Test;
import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.undertow.internal.configuration.model.UndertowConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ConfigurationParserTest {

	public static Logger LOG = LoggerFactory.getLogger(ConfigurationParserTest.class);
	private static JAXBContext context;

	@BeforeClass
	public static void jaxbContext() throws JAXBException {
		context = JAXBContext.newInstance("org.ops4j.pax.web.service.undertow.internal.configuration.model");
	}

	@Test
	public void jaxbModel() throws Exception {
		LOG.info("Unmarshall undertow-default-template.xml");
//		StreamSource source = new StreamSource(getClass().getResourceAsStream("/templates/undertow-default-template.xml"));
		Unmarshaller unmarshaller = context.createUnmarshaller();

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
		UnmarshallerHandler unmarshallerHandler = unmarshaller.getUnmarshallerHandler();
		xmlReader.setContentHandler(new ResolvingContentHandler(pid, unmarshallerHandler));
		xmlReader.parse(new InputSource(getClass().getResourceAsStream("/templates/undertow-default-template.xml")));

		//Configuration cfg = (Configuration) unmarshaller.unmarshal(source);
		UndertowConfiguration cfg = (UndertowConfiguration) unmarshallerHandler.getResult();
		LOG.info("Configuration: {}", cfg);

		assertThat(cfg.getSocketBindings().get(0).getPort(), equalTo(8123));
		assertThat(cfg.getSocketBindings().get(0).getName(), equalTo("http"));
		assertThat(cfg.getSocketBindings().get(1).getPort(), equalTo(8423));
		assertThat(cfg.getSocketBindings().get(1).getName(), equalTo("https"));

		assertThat(cfg.getSecurityRealms().get(1).getIdentities().getSsl().getKeystore().getPath(),
				equalTo("/data/tmp/certs/server.keystore"));

		LOG.info("Marshall configuration");
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		marshaller.marshal(cfg, System.out);
	}

}
