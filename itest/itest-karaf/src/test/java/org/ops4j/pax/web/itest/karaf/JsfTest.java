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
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.web.itest.karaf.RegressionConfiguration.PAX_WEB_FEATURES;
import static org.ops4j.pax.web.itest.karaf.RegressionConfiguration.paxWebVersion;
import static org.ops4j.pax.web.itest.karaf.RegressionConfiguration.regressionDefaults;
import static org.ops4j.pax.web.itest.shared.util.WebAssertions.assertResourceContainsString;
import static org.ops4j.pax.web.itest.shared.util.WebAssertions.assertResourceIsMapped;

import java.io.File;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
public class JsfTest {

    @Inject
    private ServletContext servletContext;

    @Configuration
    public Option[] config() {
        return options(
            regressionDefaults(),
            features(PAX_WEB_FEATURES, "pax-web-undertow", "mojarra"),
            composite(editConfigurationFilePut("etc/custom.properties",
                new File("src/test/resources/custom.properties"))),
            mavenBundle("org.ops4j.pax.web.samples", "pax-web-sample-jsf", paxWebVersion()));
    }

    @Test
    public void runFacelet() throws Exception {
        assertThat(servletContext.getContextPath(), is("/jsf"));
        assertResourceIsMapped("jsf/javax.faces.resource/ops4j_logo_final.png.jsf?ln=img");
        assertResourceContainsString("jsf/poll.jsf", "Equinox");
    }
}
