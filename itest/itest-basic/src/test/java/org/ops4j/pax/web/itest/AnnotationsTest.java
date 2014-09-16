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
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.web.itest.shared.util.TestConfiguration.logbackBundles;
import static org.ops4j.pax.web.itest.shared.util.TestConfiguration.paxUndertowBundles;
import static org.ops4j.pax.web.itest.shared.util.TestConfiguration.undertowBundles;
import static org.ops4j.pax.web.itest.shared.util.WebAssertions.assertResourceContainsString;
import static org.ops4j.pax.web.itest.shared.util.WebAssertions.assertResourceNotMapped;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletContext;

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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class AnnotationsTest {

    @Inject
    private BundleContext bc;

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
    public void shouldMapAnnotatedServlet() throws Exception {
        ServletContext servletContext = ServiceLookup.getService(bc, ServletContext.class);
        assertThat(servletContext.getContextPath(), is("/annotations"));
        assertResourceContainsString("annotations/hello", "Hello from Pax Web!");
        bundle.stop();

        List<String> lines = Files.readAllLines(tempFile.toPath(), StandardCharsets.UTF_8);
        assertThat(lines, is(Arrays.asList(
            "context initialized",
            "request initialized",
            "filter initialized",
            "filter pre-request",
            "in servlet",
            "filter post-request",
            "request destroyed",
            "context destroyed")));

    }

    @Test
    public void shouldNotMapAnnotatedServletToOtherUrl() throws Exception {
        ServletContext servletContext = ServiceLookup.getService(bc, ServletContext.class);
        assertThat(servletContext.getContextPath(), is("/annotations"));
        assertResourceNotMapped("annotations/foo");
    }
}
