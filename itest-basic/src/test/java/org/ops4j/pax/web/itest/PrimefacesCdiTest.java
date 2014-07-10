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
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.web.itest.TestConfiguration.logbackBundles;
import static org.ops4j.pax.web.itest.TestConfiguration.mojarraBundles;
import static org.ops4j.pax.web.itest.TestConfiguration.paxCdiSharedBundles;
import static org.ops4j.pax.web.itest.TestConfiguration.paxCdiWithWeldBundles;
import static org.ops4j.pax.web.itest.TestConfiguration.paxUndertowBundles;
import static org.ops4j.pax.web.itest.TestConfiguration.undertowBundles;

import java.io.ByteArrayOutputStream;
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


@RunWith(PaxExam.class)
public class PrimefacesCdiTest {

    @Inject
    private ServletContext servletContext;
    

    @Configuration
    public Option[] config() {

        return options(
            linkBundle("pax-web-sample-primefaces-cdi"),
            mavenBundle("org.primefaces", "primefaces", "5.0"),

            undertowBundles(),
            paxUndertowBundles(),
            mojarraBundles(),            
            paxCdiSharedBundles(),
            paxCdiWithWeldBundles(),
            logbackBundles(),
            junitBundles());
    }
    
    @Test
    public void runPrimefacesCdiFacelet() throws Exception {
        assertThat(servletContext.getContextPath(), is("/primefaces-cdi"));

        URL url = new URL(String.format("http://localhost:%s/primefaces-cdi/poll.jsf", WebAssertions.getHttpPort()));
        InputStream is = url.openStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        StreamUtils.copyStream(is, os, true);
        assertThat(os.toString(), containsString("Felix"));
    }

}
