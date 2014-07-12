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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.linkBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.web.itest.util.TestConfiguration.logbackBundles;
import static org.ops4j.pax.web.itest.util.TestConfiguration.paxUndertowBundles;
import static org.ops4j.pax.web.itest.util.TestConfiguration.undertowBundles;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.io.StreamUtils;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.ops4j.pax.web.itest.util.WebAssertions;


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
        URL url = new URL(String.format("http://localhost:%s/sample1/hello", WebAssertions.getHttpPort()));
        InputStream is = url.openStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        StreamUtils.copyStream(is, os, true);
        assertThat(os.toString(), containsString("Hello from Pax Web!"));
    }

    @Test(expected = FileNotFoundException.class)
    public void shouldNotServeOsgiInf() throws Exception {
        URL url = new URL(String.format("http://localhost:%s/sample1/OSGI-INF/protected.txt", WebAssertions.getHttpPort()));
        url.openStream();
    }

    @Test(expected = FileNotFoundException.class)
    public void shouldNotServeOsgiOpt() throws Exception {
        URL url = new URL(String.format("http://localhost:%s/sample1/OSGI-OPT/protected.txt", WebAssertions.getHttpPort()));
        url.openStream();
    }

    @Test(expected = FileNotFoundException.class)
    public void shouldNotServeMetaInf() throws Exception {
        URL url = new URL(String.format("http://localhost:%s/sample1/META-INF/MANIFEST.MF", WebAssertions.getHttpPort()));
        url.openStream();
    }
    
    @Test(expected = FileNotFoundException.class)
    public void shouldNotServeWebInf() throws Exception {
        URL url = new URL(String.format("http://localhost:%s/sample1/WEB-INF/web.xml", WebAssertions.getHttpPort()));
        url.openStream();
    }
}
