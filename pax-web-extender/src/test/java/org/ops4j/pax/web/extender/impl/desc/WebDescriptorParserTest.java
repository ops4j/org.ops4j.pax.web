/*
 * Copyright 2014 Harald Wellmann.
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
package org.ops4j.pax.web.extender.impl.desc;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.Test;
import org.ops4j.pax.web.descriptor.gen.WebAppType;


public class WebDescriptorParserTest {
    

    @Test 
    public void shouldParseWebXml() throws IOException {
        WebDescriptorParser parser = new WebDescriptorParser();
        File file = new File("../samples/pax-web-sample-auth-basic/src/main/webapp/WEB-INF/web.xml");
        URL url = file.getCanonicalFile().toURI().toURL();
        WebAppType descriptor = parser.parseWebXml(url);
        assertThat(descriptor, is(notNullValue()));
        assertThat(descriptor.getModuleNameOrDescriptionAndDisplayName().size(), is(6));
    }
}
