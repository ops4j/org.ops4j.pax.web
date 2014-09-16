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
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.web.itest.shared.util.TestConfiguration.logbackBundles;
import static org.ops4j.pax.web.itest.shared.util.TestConfiguration.paxUndertowBundles;
import static org.ops4j.pax.web.itest.shared.util.TestConfiguration.undertowBundles;
import static org.ops4j.pax.web.itest.shared.util.WebAssertions.assertResourceContainsString;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.http.HttpService;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class WebConsoleTest {

    @Inject
    private HttpService httpService;

    @Configuration
    public Option[] config() {
        return options(
            mavenBundle("org.apache.felix", "org.apache.felix.webconsole", "4.2.2").classifier("all"),

            undertowBundles(),
            paxUndertowBundles(),
            logbackBundles(),
            junitBundles());
    }

    @Test
    public void runWhiteboardServlet() throws Exception {
        assertThat(httpService, is(notNullValue()));
        assertResourceContainsString("system/console/bundles", "Apache Felix Web Console");
        assertResourceContainsString("system/console/res/ui/webconsole.css", "#technav");
    }
}
