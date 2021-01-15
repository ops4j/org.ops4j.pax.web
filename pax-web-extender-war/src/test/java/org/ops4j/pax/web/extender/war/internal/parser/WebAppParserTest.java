/* Copyright 2017 Achim Nierbeck.
 *
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
package org.ops4j.pax.web.extender.war.internal.parser;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class WebAppParserTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testParseWebXml() throws Exception {
        WebAppParser parser = new WebAppParser(null);
        File file = new File("src/test/resources/web.xml");
        assertTrue(file.exists());
//        WebAppType parseWebXml = parser.parseWebXml(file.toURI().toURL());
//        assertNotNull(parseWebXml);
//        List<JAXBElement<?>> list = parseWebXml.getModuleNameOrDescriptionAndDisplayName();
//        for (JAXBElement<?> jaxbElement : list) {
//            Object value = jaxbElement.getValue();
//            if (value instanceof SessionConfigType) {
//                SessionConfigType sessionConfig = (SessionConfigType) value;
//                BigInteger value2 = sessionConfig.getSessionTimeout().getValue();
//                assertThat(value2.intValue(), is(30));
//            }
//        }
    }

}
