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
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.web.itest.util.TestConfiguration.logbackBundles;
import static org.ops4j.pax.web.itest.util.TestConfiguration.paxUndertowBundles;
import static org.ops4j.pax.web.itest.util.TestConfiguration.undertowBundles;
import static org.ops4j.pax.web.itest.util.WebAssertions.assertResourceContainsString;
import static org.ops4j.pax.web.itest.util.WebAssertions.assertResourceNotMapped;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ResourcesTest {

    @Inject
    @Filter(timeout = 10000000)
    private ServletContext servletContext;

    @Configuration
    public Option[] config() {
        return options(
            linkBundle("pax-web-sample-static"),

            undertowBundles(),
            paxUndertowBundles(),
            logbackBundles(),
            junitBundles());
    }

    @Test
    public void runStaticResourceServlet() throws Exception {
        assertThat(servletContext.getContextPath(), is("/sample1"));
        assertResourceContainsString("sample1/hello", "Hello from Pax Web!");
    }

    public void shouldNotServeOsgiInf() throws Exception {
        assertResourceNotMapped("sample1/OSGI-INF/protected.txt");
    }

    public void shouldNotServeOsgiOpt() throws Exception {
        assertResourceNotMapped("sample1/OSGI-OPT/protected.txt");
    }

    public void shouldNotServeMetaInf() throws Exception {
        assertResourceNotMapped("sample1/META-INF/MANIFEST.MF");
    }
    
    public void shouldNotServeWebInf() throws Exception {
        assertResourceNotMapped("sample1/WEB-INF/web.xml");
    }
}
