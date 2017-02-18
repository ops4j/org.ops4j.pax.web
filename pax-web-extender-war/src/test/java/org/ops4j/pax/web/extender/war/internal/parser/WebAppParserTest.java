package org.ops4j.pax.web.extender.war.internal.parser;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.io.File;
import java.math.BigInteger;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.descriptor.gen.SessionConfigType;
import org.ops4j.pax.web.descriptor.gen.WebAppType;

public class WebAppParserTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testParseWebXml() throws Exception {
        WebAppParser parser = new WebAppParser(null);
        File file = new File("src/test/resources/web.xml");
        assertTrue(file.exists());
        WebAppType parseWebXml = parser.parseWebXml(file.toURL());
        assertNotNull(parseWebXml);
        List<JAXBElement<?>> list = parseWebXml.getModuleNameOrDescriptionAndDisplayName();
        for (JAXBElement<?> jaxbElement : list) {
            Object value = jaxbElement.getValue();
            if (value instanceof SessionConfigType) {
                SessionConfigType sessionConfig = (SessionConfigType) value;
                BigInteger value2 = sessionConfig.getSessionTimeout().getValue();
                assertThat(value2.intValue(), is(30));
            }
        }
    }

}
