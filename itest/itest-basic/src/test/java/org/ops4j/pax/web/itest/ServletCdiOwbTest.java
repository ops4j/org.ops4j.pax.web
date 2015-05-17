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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.linkBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.web.itest.shared.util.TestConfiguration.logbackBundles;
import static org.ops4j.pax.web.itest.shared.util.TestConfiguration.mojarraBundles;
import static org.ops4j.pax.web.itest.shared.util.TestConfiguration.paxCdiSharedBundles;
import static org.ops4j.pax.web.itest.shared.util.TestConfiguration.paxCdiWithOwbBundles;
import static org.ops4j.pax.web.itest.shared.util.TestConfiguration.paxUndertowBundles;
import static org.ops4j.pax.web.itest.shared.util.TestConfiguration.undertowBundles;
import static org.ops4j.pax.web.itest.shared.util.WebAssertions.assertResourceContainsString;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.Filter;
import org.ops4j.pax.swissbox.core.BundleUtils;
import org.ops4j.pax.swissbox.tracker.ServiceLookup;
import org.ops4j.pax.swissbox.tracker.ServiceLookupException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;


@RunWith(PaxExam.class)
public class ServletCdiOwbTest {

    @Inject
    @Filter(timeout = 10000000)
    private ServletContext servletContext;

    @Inject
    private BundleContext bc;


    @Configuration
    public Option[] config() {

        return options(
            linkBundle("pax-web-sample-cdi"),

            undertowBundles(),
            paxUndertowBundles(),
            mojarraBundles(),
            paxCdiSharedBundles(),
            paxCdiWithOwbBundles(),
            logbackBundles(),
            junitBundles());
    }

    @Test
    public void runCdiServlet() {
        assertThat(servletContext.getContextPath(), is("/cdi"));
        assertResourceContainsString("cdi/message", "Message from managed bean");
    }

    @Test
    @Ignore
    public void shouldRestartWebBeanBundle() throws BundleException {
        ServiceLookup.getService(bc, ServletContext.class);
        ServiceLookup.getService(bc, CdiContainer.class);
        assertResourceContainsString("cdi/message", "Message from managed bean");

        Bundle webBundle = BundleUtils.getBundle(bc, "pax-web-sample-cdi");
        assertThat(webBundle, is(notNullValue()));
        webBundle.stop();
        try {
            ServiceLookup.getService(bc, ServletContext.class, 1000);
            fail("Should not find ServletContext");
        }
        catch (ServiceLookupException exc) {
            // ignore
        }
        webBundle.start();
        ServiceLookup.getService(bc, ServletContext.class);
        ServiceLookup.getService(bc, CdiContainer.class);
        assertResourceContainsString("cdi/message", "Message from managed bean");
    }

    @Test
    @Ignore
    public void shouldRestartWebExtender() throws BundleException, InterruptedException {
        ServiceLookup.getService(bc, ServletContext.class);
        ServiceLookup.getService(bc, CdiContainer.class);
        assertResourceContainsString("cdi/message", "Message from managed bean");

        Bundle webBundle = BundleUtils.getBundle(bc, "org.ops4j.pax.web.pax-web-extender");
        assertThat(webBundle, is(notNullValue()));
        webBundle.stop();
        try {
            ServiceLookup.getService(bc, ServletContext.class, 1000);
            fail("Should not find ServletContext");
        }
        catch (ServiceLookupException exc) {
            // ignore
        }
        webBundle.start();
        ServiceLookup.getService(bc, ServletContext.class);
        ServiceLookup.getService(bc, CdiContainer.class);
        assertResourceContainsString("cdi/message", "Message from managed bean");
    }
}
