package org.ops4j.pax.web.extender.impl.desc;

import java.io.IOException;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.sax.SAXSource;

import org.ops.pax.web.spi.WebAppModel;
import org.ops4j.pax.web.descriptor.gen.WebAppType;
import org.xml.sax.InputSource;


public class WebDescriptorParser {
    
    
    public WebAppModel createWebAppModel(URL url) {
        WebAppType webAppType = parseWebXml(url);
        WebAppModelBuilder builder = new WebAppModelBuilder(webAppType);
        return builder.build();
    }
    
    public WebAppType parseWebXml(URL url)  {
        try {
            JAXBContext context = JAXBContext.newInstance(WebAppType.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            SAXSource source = new SAXSource(new InputSource(url.openStream()));
            return unmarshaller.unmarshal(source, WebAppType.class).getValue();
        }
        catch (JAXBException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

}
