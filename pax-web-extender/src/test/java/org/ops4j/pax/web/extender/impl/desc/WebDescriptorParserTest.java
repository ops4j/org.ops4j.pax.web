package org.ops4j.pax.web.extender.impl.desc;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.xml.bind.JAXBElement;

import org.junit.Test;
import org.ops4j.pax.web.descriptor.gen.WebAppType;


public class WebDescriptorParserTest {
    

    @Test 
    public void shouldParseWebXml() throws IOException {
        WebDescriptorParser parser = new WebDescriptorParser();
        File file = new File("../samples/pax-web-sample-auth-basic/src/main/webapp/WEB-INF/web.xml");
        URL url = file.getCanonicalFile().toURI().toURL();
        WebAppType descriptor = parser.parseWebXml(url);
        for (JAXBElement<?> elem :descriptor.getModuleNameOrDescriptionAndDisplayName()) {
            System.out.println(elem.getValue().getClass());
        }
    }
}
