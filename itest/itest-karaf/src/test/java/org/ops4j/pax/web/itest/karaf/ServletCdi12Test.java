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
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.web.itest.karaf.RegressionConfiguration.PAX_CDI_FEATURES;
import static org.ops4j.pax.web.itest.karaf.RegressionConfiguration.PAX_WEB_FEATURES;
import static org.ops4j.pax.web.itest.karaf.RegressionConfiguration.paxWebVersion;
import static org.ops4j.pax.web.itest.karaf.RegressionConfiguration.regressionDefaults;
import static org.ops4j.pax.web.itest.shared.util.WebAssertions.assertResourceContainsString;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;


@RunWith(PaxExam.class)
public class ServletCdi12Test {

    @Inject
    private ServletContext servletContext;


    @Configuration
    public Option[] config() {

        return options(
            regressionDefaults(),
            features(PAX_CDI_FEATURES),
            features(PAX_WEB_FEATURES, "pax-web-undertow-weld"),
            mavenBundle("org.ops4j.pax.web.samples", "pax-web-sample-cdi", paxWebVersion()));
    }

    @Test
    public void runCdiServlet() throws Exception {
        assertThat(servletContext.getContextPath(), is("/cdi"));
        assertResourceContainsString("cdi/message", "Message from managed bean");
    }
}
