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

import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.web.itest.WebAssertions.assertResourceContainsString;
import static org.ops4j.pax.web.itest.TestConfiguration.logbackBundles;
import static org.ops4j.pax.web.itest.TestConfiguration.paxUndertowBundles;
import static org.ops4j.pax.web.itest.TestConfiguration.undertowBundles;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class WhiteboardServletTest {

    @Inject
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

        assertResourceContainsString("hello", "Hello from Pax Web!");
        assertResourceContainsString("bye", "Goodbye from Pax Web!");
    }

    @Test
    public void registerResources() throws Exception {
        httpService.registerResources("/res", "/", new BundleResourceHttpContext(bc.getBundle()));
        assertResourceContainsString("res/plain.txt", "Plain text");
    }

    @Test
    public void registerResourcesWithName() throws Exception {
        httpService.registerResources("/res", "/subdir", new BundleResourceHttpContext(bc.getBundle()));
        assertResourceContainsString("res/subdir.txt", "subdirectory");
    }

    @Test
    public void registerResourcesWithNameAndCompositeAlias() throws Exception {
        httpService.registerResources("/path/to", "/subdir", new BundleResourceHttpContext(bc.getBundle()));
        assertResourceContainsString("path/to/subdir.txt", "subdirectory");
    }
}
