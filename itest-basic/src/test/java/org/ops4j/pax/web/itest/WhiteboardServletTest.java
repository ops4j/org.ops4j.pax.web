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
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.web.itest.TestConfiguration.getHttpPort;
import static org.ops4j.pax.web.itest.TestConfiguration.logbackBundles;
import static org.ops4j.pax.web.itest.TestConfiguration.paxUndertowBundles;
import static org.ops4j.pax.web.itest.TestConfiguration.undertowBundles;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.io.StreamUtils;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class WhiteboardServletTest {

    @Inject
    @Filter(timeout = 10000000)
    private HttpService httpService;
    
    @Inject
    private BundleContext bc;

    @Configuration
    public Option[] config() {
        return options(
            //linkBundle("pax-web-sample-whiteboard"),

            undertowBundles(),
            paxUndertowBundles(),
            logbackBundles(),
            junitBundles());
    }

    @Test
    public void runWhiteboardServlet() throws Exception {
        HttpContext defaultContext = httpService.createDefaultHttpContext();
        httpService.registerServlet("/hello", new HelloServlet(), null, defaultContext);
        httpService.registerServlet("/bye", new GoodbyeServlet(), null, defaultContext);
        URL url = new URL(String.format("http://localhost:%s/hello", getHttpPort()));
        InputStream is = url.openStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        StreamUtils.copyStream(is, os, true);
        assertThat(os.toString(), containsString("Hello from Pax Web!"));
        url = new URL(String.format("http://localhost:%s/bye", getHttpPort()));
        is = url.openStream();
        os = new ByteArrayOutputStream();
        StreamUtils.copyStream(is, os, true);
        assertThat(os.toString(), containsString("Goodbye from Pax Web!"));
    }

    @Test
    public void registerResources() throws Exception {
        httpService.registerResources("/res", "/", new BundleResourceHttpContext(bc.getBundle()));
        
        URL url = new URL(String.format("http://localhost:%s/res/plain.txt", getHttpPort()));
        InputStream is = url.openStream();
        OutputStream os = new ByteArrayOutputStream();
        StreamUtils.copyStream(is, os, true);
        assertThat(os.toString(), containsString("Plain text"));
    }

    @Test
    public void registerResourcesWithName() throws Exception {
        httpService.registerResources("/res", "/subdir", new BundleResourceHttpContext(bc.getBundle()));
        
        URL url = new URL(String.format("http://localhost:%s/res/subdir.txt", getHttpPort()));
        InputStream is = url.openStream();
        OutputStream os = new ByteArrayOutputStream();
        StreamUtils.copyStream(is, os, true);
        assertThat(os.toString(), containsString("subdirectory"));
    }

    @Test
    public void registerResourcesWithNameAndCompositeAlias() throws Exception {
        httpService.registerResources("/path/to", "/subdir", new BundleResourceHttpContext(bc.getBundle()));
        
        URL url = new URL(String.format("http://localhost:%s/path/to/subdir.txt", getHttpPort()));
        InputStream is = url.openStream();
        OutputStream os = new ByteArrayOutputStream();
        StreamUtils.copyStream(is, os, true);
        assertThat(os.toString(), containsString("subdirectory"));
    }

}
