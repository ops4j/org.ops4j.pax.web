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
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.web.itest.shared.util.TestConfiguration.httpClientBundles;
import static org.ops4j.pax.web.itest.shared.util.TestConfiguration.logbackBundles;
import static org.ops4j.pax.web.itest.shared.util.TestConfiguration.paxUndertowBundles;
import static org.ops4j.pax.web.itest.shared.util.TestConfiguration.undertowBundles;
import static org.ops4j.pax.web.itest.shared.util.WebAssertions.getHttpPort;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class VirtualHostsTest {

    @Inject
    private ServletContext servletContext;

    @Configuration
    public Option[] config() {
        return options(
            systemProperty("felix.fileinstall.noInitialDelay").value("true"),
            systemProperty("felix.fileinstall.dir").value("src/test/config/VirtualHostsTest"),

            linkBundle("pax-web-sample-static"),
            linkBundle("org.apache.felix.fileinstall"),
            linkBundle("org.apache.felix.configadmin"),
            
            undertowBundles(),
            paxUndertowBundles(),
            httpClientBundles(),
            logbackBundles(),
            junitBundles());
    }

    @Test
    public void shouldFindResourceOnVirtualHost() throws Exception {
        assertThat(servletContext.getContextPath(), is("/cm-static"));

        String path = String.format("http://localhost:%d/cm-static/hello", getHttpPort());
        HttpClientContext context = HttpClientContext.create();
        CloseableHttpClient client = HttpClients.custom().build();
        HttpGet httpGet = new HttpGet(path);
        httpGet.setHeader("Host", "alias1");
        HttpResponse response = client.execute(httpGet, context);
        
        int statusCode = response.getStatusLine().getStatusCode();
        assertThat(statusCode, is(200));

        String text = EntityUtils.toString(response.getEntity());
        assertThat(text, containsString("Hello from Pax Web!"));        
    }

    @Test
    public void shouldNotFindResourceOnDefaultHost() throws Exception {
        assertThat(servletContext.getContextPath(), is("/cm-static"));

        String path = String.format("http://localhost:%d/cm-static/hello", getHttpPort());
        HttpClientContext context = HttpClientContext.create();
        CloseableHttpClient client = HttpClients.custom().build();
        HttpGet httpGet = new HttpGet(path);
        HttpResponse response = client.execute(httpGet, context);
        
        int statusCode = response.getStatusLine().getStatusCode();
        assertThat(statusCode, is(404));
    }

    @Test
    public void shouldNotFindResourceOnIncorrectVirtualHost() throws Exception {
        assertThat(servletContext.getContextPath(), is("/cm-static"));

        String path = String.format("http://localhost:%d/cm-static/hello", getHttpPort());
        HttpClientContext context = HttpClientContext.create();
        CloseableHttpClient client = HttpClients.custom().build();
        HttpGet httpGet = new HttpGet(path);
        httpGet.setHeader("Host", "noway");
        HttpResponse response = client.execute(httpGet, context);
        
        int statusCode = response.getStatusLine().getStatusCode();
        assertThat(statusCode, is(404));
    }
}
