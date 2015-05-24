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
import org.ops4j.pax.web.itest.shared.asset.SimpleFilter;
import org.ops4j.pax.web.itest.shared.asset.TestServlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

import javax.inject.Inject;
import javax.servlet.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.web.itest.shared.util.TestConfiguration.*;
import static org.ops4j.pax.web.itest.shared.util.WebAssertions.assertResourceContainsString;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class FilterIntegrationTest {

    protected static final String WEB_CONTEXT_PATH = "Web-ContextPath";

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

    /**
     * You will get a list of bundles installed by default plus your testcase,
     * wrapped into a bundle called pax-exam-probe
     */
    @Test
    public void listBundles() {
        for (final Bundle b : bc.getBundles()) {
            if (b.getState() != Bundle.ACTIVE) {
                fail("Bundle should be active: " + b);
            }

            final Dictionary<String,String> headers = b.getHeaders();
            final String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
            if (ctxtPath != null) {
                System.out.println("Bundle " + b.getBundleId() + " : "
                        + b.getSymbolicName() + " : " + ctxtPath);
            } else {
                System.out.println("Bundle " + b.getBundleId() + " : "
                        + b.getSymbolicName());
            }
        }

    }

    @Test
    public void testSimpleFilter() throws Exception {
        ServiceTracker<WebContainer, WebContainer> tracker = new ServiceTracker<WebContainer, WebContainer>(bundleContext, WebContainer.class, null);
        tracker.open();
        WebContainer service = tracker.waitForService(TimeUnit.SECONDS.toMillis(20));

        final String fullContent = "This content is Filtered by a javax.servlet.Filter";
        Filter filter = new Filter() {

            @Override
            public void init(FilterConfig filterConfig) throws ServletException {
            }

            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                PrintWriter writer = response.getWriter();
                writer.write(fullContent);
                writer.flush();
            }

            @Override
            public void destroy() {
            }
        };

        Dictionary<String, String> initParams = new Hashtable<String, String>();

        HttpContext defaultHttpContext = service.createDefaultHttpContext();
        service.begin(defaultHttpContext);
        service.registerResources("/", "default", defaultHttpContext);

        service.registerFilter(filter, new String[]{"/testFilter/*",}, new String[]{"default",}, initParams, defaultHttpContext);

        service.end(defaultHttpContext);

        Thread.sleep(200);

        assertResourceContainsString("testFilter/filter.me",
                "This content is Filtered by a javax.servlet.Filter");

        service.unregisterFilter(filter);
    }

}
