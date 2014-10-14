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
package org.ops4j.pax.web.itest.karaf;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.web.itest.karaf.RegressionConfiguration.PAX_WEB_FEATURES;
import static org.ops4j.pax.web.itest.karaf.RegressionConfiguration.paxWebVersion;
import static org.ops4j.pax.web.itest.karaf.RegressionConfiguration.regressionDefaults;
import static org.ops4j.pax.web.itest.shared.util.WebAssertions.assertResourceContainsString;

import java.io.File;
import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;


@RunWith(PaxExam.class)
public class JspTest {

    @Inject
    private ServletContext servletContext;

    @Configuration
    public Option[] config() {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        tmpDir.mkdirs();

        return options(
            regressionDefaults(),
            systemProperty("io.undertow.message").value("Hello JSP!"),
            // does not work: we have to use a vmOption
            // systemProperty("java.io.tmpdir").value(tmpDir.getPath()),
            vmOption("-Djava.io.tmpdir=" + tmpDir.getPath()),
            features(PAX_WEB_FEATURES, "pax-web-undertow", "pax-web-jsp"),
            mavenBundle("org.apache.logging.log4j", "log4j-taglib", "2.0.2"),
            mavenBundle("org.apache.logging.log4j", "log4j-api", "2.0.2"),
            mavenBundle("org.apache.logging.log4j", "log4j-to-slf4j", "2.0.2"),
            mavenBundle("org.ops4j.pax.web.samples", "pax-web-sample-jsp", paxWebVersion()));
    }

    @Test
    public void runJsp() throws Exception {
        assertThat(servletContext.getContextPath(), is("/jsp"));
        assertResourceContainsString("jsp/index.jsp", "Hello JSP!");
    }
}
