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
import static org.ops4j.pax.web.itest.shared.util.TestConfiguration.logbackBundles;
import static org.ops4j.pax.web.itest.shared.util.TestConfiguration.paxUndertowBundles;
import static org.ops4j.pax.web.itest.shared.util.TestConfiguration.undertowBundles;
import static org.ops4j.pax.web.itest.shared.util.WebAssertions.assertResourceContainsString;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class WarDeploymentTest {

    @Inject
    private ServletContext servletContext;

    private static File war;
    private static File deploymentDir;

    @Configuration
    public Option[] config() {

        return options(
            //systemProperty("felix.fileinstall.noInitialDelay").value("true"),
            systemProperty("felix.fileinstall.dir").value("target/deployments"),
            linkBundle("org.apache.felix.fileinstall"),
            linkBundle("org.ops4j.pax.web.pax-web-fileinstall"),
            mavenBundle("org.ops4j.pax.url", "pax-url-war", "2.1.0").classifier("uber").startLevel(2),
            
            undertowBundles(),
            paxUndertowBundles(),
            logbackBundles(),
            junitBundles());
    }
    
    @BeforeClass
    public static void before() throws IOException {
        deploymentDir = new File("target/deployments");
        deploymentDir.mkdirs();
        war = new File(deploymentDir, "sample.war");
        URL url = new URL("mvn:org.ops4j.pax.web.samples/pax-web-sample-war/5.0.0-SNAPSHOT/war");
        try (InputStream is = url.openStream()) {
            Files.copy(is, war.toPath());
        }
    }
    
    @AfterClass
    public static void after() {
        war.delete();
    }

    @Test
    public void runStaticResourceServlet() throws Exception {
        assertThat(servletContext.getContextPath(), is("/sample"));
        assertResourceContainsString("sample/hello", "Hello from Pax Web!");
    }
}
