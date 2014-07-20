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
package org.ops4j.pax.web.itest;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.linkBundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.web.itest.util.TestConfiguration.logbackBundles;

import java.io.File;
import java.net.URL;

import javax.inject.Inject;

import org.apache.tomcat.util.descriptor.web.WebXml;
import org.apache.tomcat.util.descriptor.web.WebXmlParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.swissbox.core.BundleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class WebXmlParserTest {
    
    @Inject
    private BundleContext bc;

    @Configuration
    public Option[] config() {
        return options(
            mavenBundle("org.ops4j.pax.web", "pax-web-parser", "5.0.0-SNAPSHOT"),
            //wrappedBundle(mavenBundle("org.apache.tomcat", "tomcat-juli", "8.0.8")),
            linkBundle("javax.servlet-api"),
            logbackBundles(),
            junitBundles());
    }

    @Test
    public void runStaticResourceServlet() throws Exception {
        //Thread.sleep(10000000);
        Bundle bundle = BundleUtils.getBundle(bc, "org.ops4j.pax.web.pax-web-parser");
        ClassLoader cl = bundle.adapt(BundleWiring.class).getClassLoader();
        //Thread.currentThread().setContextClassLoader(cl);
        WebXmlParser parser = new WebXmlParser(true, false, true);
        File file = new File("../samples/pax-web-sample-auth-basic/src/main/webapp/WEB-INF/web.xml");
        URL url = file.getCanonicalFile().toURI().toURL();
        WebXml webXml = new WebXml();
        boolean result = parser.parseWebXml(url, webXml, false);
        assertThat(result, is(true));
    }
}
