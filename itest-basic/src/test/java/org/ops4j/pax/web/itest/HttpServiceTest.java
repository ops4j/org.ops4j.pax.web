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

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.web.itest.util.TestConfiguration.logbackBundles;
import static org.ops4j.pax.web.itest.util.TestConfiguration.paxUndertowBundles;
import static org.ops4j.pax.web.itest.util.TestConfiguration.undertowBundles;
import static org.ops4j.pax.web.itest.util.WebAssertions.assertResourceContainsString;
import static org.ops4j.pax.web.itest.util.WebAssertions.assertResourceNotMapped;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.web.itest.servlet.BundleResourceHttpContext;
import org.ops4j.pax.web.itest.servlet.GoodbyeServlet;
import org.ops4j.pax.web.itest.servlet.HelloServlet;
import org.ops4j.pax.web.itest.servlet.Messages;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class HttpServiceTest {

    @Inject
    private HttpService httpService;
    
    @Inject
    private BundleContext bc;

    @Configuration
    public Option[] config() {
        return options(
            undertowBundles(),
            paxUndertowBundles(),
            logbackBundles(),
            junitBundles());
    }
    
    @After
    public void after() {
        Messages.clear();
    }

    @Test
    public void runWhiteboardServlets() throws Exception {
        HttpContext defaultContext = httpService.createDefaultHttpContext();
        httpService.registerServlet("/hello", new HelloServlet(), null, defaultContext);
        Dictionary<String,String> params = new Hashtable<String, String>();
        params.put("bye.init", "foo");
        httpService.registerServlet("/bye", new GoodbyeServlet(), params, defaultContext);

        assertResourceContainsString("hello", "Hello from Pax Web!");
        assertResourceContainsString("bye", "Goodbye from Pax Web!");
        assertThat(Messages.getMessages(), hasItems("bye.init = foo"));
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
    public void registerAndUnregisterResourcesWithName() throws Exception {
        httpService.registerResources("/res", "/subdir", new BundleResourceHttpContext(bc.getBundle()));
        assertResourceContainsString("res/subdir.txt", "subdirectory");
        httpService.unregister("/res");
        assertResourceNotMapped("res/subdir.txt");
    }

    @Test
    public void registerResourcesWithNameAndCompositeAlias() throws Exception {
        httpService.registerResources("/path/to", "/subdir", new BundleResourceHttpContext(bc.getBundle()));
        assertResourceContainsString("path/to/subdir.txt", "subdirectory");
    }
    
    @Test
    public void registerAndUnregisterServlets() throws Exception {
        HttpContext defaultContext = httpService.createDefaultHttpContext();
        httpService.registerServlet("/hello", new HelloServlet(), null, defaultContext);
        httpService.registerServlet("/bye", new GoodbyeServlet(), null, defaultContext);

        assertResourceContainsString("hello", "Hello from Pax Web!");
        assertResourceContainsString("bye", "Goodbye from Pax Web!");

        httpService.unregister("/hello");
        assertResourceNotMapped("hello");
        assertResourceContainsString("bye", "Goodbye from Pax Web!");
    }

    @Test
    public void shouldCallInitAndDestroy() throws Exception {
        HttpContext defaultContext = httpService.createDefaultHttpContext();
        Dictionary<String,String> params = new Hashtable<String, String>();
        params.put("bye.init", "foo");
        httpService.registerServlet("/bye", new GoodbyeServlet(), params, defaultContext);
        assertResourceContainsString("bye", "Goodbye from Pax Web!");
        httpService.unregister("/bye");
        
        assertThat(Messages.getMessages(), hasItems("bye.init = foo", "destroying GoodbyeServlet"));
    }

    @Test
    public void reregisterServlets() throws Exception {
        HttpContext defaultContext = httpService.createDefaultHttpContext();
        httpService.registerServlet("/hello", new HelloServlet(), null, defaultContext);
        httpService.registerServlet("/bye", new GoodbyeServlet(), null, defaultContext);
        httpService.unregister("/hello");
        httpService.unregister("/bye");
        httpService.registerServlet("/hello", new HelloServlet(), null, defaultContext);
        httpService.registerServlet("/bye", new GoodbyeServlet(), null, defaultContext);

        assertResourceContainsString("hello", "Hello from Pax Web!");
        assertResourceContainsString("bye", "Goodbye from Pax Web!");
    }
}
