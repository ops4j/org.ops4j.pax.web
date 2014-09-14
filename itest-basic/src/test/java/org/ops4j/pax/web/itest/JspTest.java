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
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.web.itest.util.TestConfiguration.logbackBundles;
import static org.ops4j.pax.web.itest.util.TestConfiguration.mojarraBundles;
import static org.ops4j.pax.web.itest.util.TestConfiguration.paxUndertowBundles;
import static org.ops4j.pax.web.itest.util.TestConfiguration.undertowBundles;
import static org.ops4j.pax.web.itest.util.WebAssertions.assertResourceContainsString;

import java.io.File;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.Filter;


@RunWith(PaxExam.class)
public class JspTest {

    @Inject
    @Filter(timeout = 20000000)
    private ServletContext servletContext;

    @Configuration
    public Option[] config() {

        File tmpDir = new File(System.getProperty("java.io.tmpdir"), "pax-web-undertow");
        tmpDir.mkdirs();

        return options(
            systemProperty("io.undertow.message").value("Hello JSP!"),
            systemProperty("java.io.tmpdir").value(tmpDir.getPath()),

            mavenBundle("org.ops4j.pax.web", "pax-web-jsp", "5.0.0-SNAPSHOT"),
            mavenBundle("org.eclipse.jdt.core.compiler", "ecj", "4.4"),
            mavenBundle("org.apache.logging.log4j", "log4j-taglib", "2.0.2"),
            mavenBundle("org.apache.logging.log4j", "log4j-api", "2.0.2"),
            mavenBundle("org.apache.logging.log4j", "log4j-to-slf4j", "2.0.2"),

            linkBundle("pax-web-sample-jsp"),

            undertowBundles(),
            paxUndertowBundles(),
            mojarraBundles(),
            logbackBundles(),
            junitBundles());
    }

    @Test
    public void runJsp() throws Exception {
        assertThat(servletContext.getContextPath(), is("/jsp"));
        assertResourceContainsString("jsp/index.jsp", "Hello JSP!");
    }
}
