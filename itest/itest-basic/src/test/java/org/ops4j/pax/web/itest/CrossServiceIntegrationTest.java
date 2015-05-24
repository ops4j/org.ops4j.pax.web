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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.swissbox.tracker.ServiceLookup;
import org.ops4j.pax.web.itest.shared.asset.SimpleFilter;
import org.ops4j.pax.web.itest.shared.asset.TestServlet;
import org.osgi.framework.*;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.ServletContext;
import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.web.itest.shared.util.TestConfiguration.*;
import static org.ops4j.pax.web.itest.shared.util.WebAssertions.assertResourceContainsString;
import static org.ops4j.pax.web.itest.shared.util.WebAssertions.assertResourceNotMapped;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CrossServiceIntegrationTest {

    @Inject
    private BundleContext bc;

    @Inject
    private HttpService httpService;

    private Bundle bundle;
    private File tempFile;

    @Configuration
    public Option[] config() {
        return options(
            undertowBundles(),
            paxUndertowBundles(),
            logbackBundles(),
            junitBundles());
    }

    @Before
    public void before() throws Exception {
        tempFile = File.createTempFile("paxexam", ".txt");
        System.setProperty("pax.exam.messages", tempFile.getAbsolutePath());

        URL bundleUrl = new URL("link:classpath:pax-web-sample-annotations.link");
        bundle = bc.installBundle("test-location", bundleUrl.openStream());
        bundle.start();
    }

    @After
    public void after() throws BundleException {
        bundle.stop();
    }

    @Test
    public void testMultipleServiceCombination() throws Exception {

        HttpContext defaultHttpContext = httpService.createDefaultHttpContext();

        Dictionary<String, Object> contextProps = new Hashtable<String, Object>();
        contextProps.put("httpContext.id", "crosservice");

        bc.registerService(HttpContext.class.getName(), defaultHttpContext, contextProps);

        //registering without an explicit context might be the issue.
        httpService.registerServlet("/crosservice", new TestServlet(), null, defaultHttpContext);

        // Register a servlet filter via whiteboard
        Dictionary<String, Object> filterProps = new Hashtable<String, Object>();
        filterProps.put("filter-name", "Sample Filter");
        filterProps.put("urlPatterns", "/crosservice/*");
        filterProps.put("httpContext.id", "crosservice");
        ServiceRegistration<?> registerService = bc.registerService(Filter.class.getName(), new SimpleFilter(), filterProps);


        assertResourceContainsString("crosservice", "TEST OK");
        assertResourceContainsString("crosservice", "FILTER-INIT: true");

        registerService.unregister();

        httpService.unregister("/crosservice");

    }

    @Test
    public void testMultipleServiceCombinationWithDefaultHttpContext() throws Exception {

        //registering without an explicit context might be the issue.
        httpService.registerServlet("/crosservice", new TestServlet(), null, null);

        // Register a servlet filter via whiteboard
        Dictionary<String, Object> filterProps = new Hashtable<String, Object>();
//        filterProps.put("filter-name", "Sample Filter");
        filterProps.put("urlPatterns", "/crosservice/*");
        ServiceRegistration<?> registerService = bc.registerService(Filter.class.getName(), new SimpleFilter(), filterProps);


        assertResourceContainsString("crosservice", "TEST OK");
        assertResourceContainsString("crosservice", "FILTER-INIT: true");

        registerService.unregister();

        httpService.unregister("/crosservice");

    }

}
