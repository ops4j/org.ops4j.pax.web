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
import static org.ops4j.pax.web.itest.util.TestConfiguration.httpClientBundles;
import static org.ops4j.pax.web.itest.util.TestConfiguration.logbackBundles;
import static org.ops4j.pax.web.itest.util.TestConfiguration.paxUndertowBundles;
import static org.ops4j.pax.web.itest.util.TestConfiguration.undertowBundles;
import static org.ops4j.pax.web.itest.util.WebAssertions.getHttpPort;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
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
public class DigestAuthenticationTest {

    @Inject
    private ServletContext servletContext;

    @Configuration
    public Option[] config() {
        return options(
            linkBundle("pax-web-sample-auth-digest"), 
            httpClientBundles(),
            linkBundle("pax-web-sample-login"), 
            undertowBundles(),
            paxUndertowBundles(), 
            logbackBundles(), 
            junitBundles());
    }

    @Test
    public void shouldPermitAccess() throws Exception {
        assertThat(servletContext.getContextPath(), is("/digest"));
        
        String path = String.format("http://localhost:%d/digest/hello", getHttpPort());
        HttpClientContext context = HttpClientContext.create();
        BasicCredentialsProvider cp = new BasicCredentialsProvider();
        cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("mustermann", "mustermann"));
        CloseableHttpClient client = HttpClients.custom().
            setDefaultCredentialsProvider(cp).
            build();
        
        HttpGet httpGet = new HttpGet(path);
        HttpResponse response = client.execute(httpGet, context);
        
        int statusCode = response.getStatusLine().getStatusCode();
        assertThat(statusCode, is(200));
        String text = EntityUtils.toString(response.getEntity());
        assertThat(text, containsString("Hello from Pax Web!"));        
    }

    @Test
    public void shouldDenyAccessOnWrongPassword() throws Exception {
        assertThat(servletContext.getContextPath(), is("/digest"));

        String path = String.format("http://localhost:%d/digest/hello", getHttpPort());
        HttpClientContext context = HttpClientContext.create();
        BasicCredentialsProvider cp = new BasicCredentialsProvider();
        cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("mustermann", "wrong"));
        CloseableHttpClient client = HttpClients.custom().
            setDefaultCredentialsProvider(cp).
            build();
        
        HttpGet httpGet = new HttpGet(path);
        HttpResponse response = client.execute(httpGet, context);
        
        int statusCode = response.getStatusLine().getStatusCode();
        assertThat(statusCode, is(401));
    }

    @Test
    public void shouldPermitAccessToUnprotectedResource() throws Exception {
        assertThat(servletContext.getContextPath(), is("/digest"));

        String path = String.format("http://localhost:%d/digest/plain.txt", getHttpPort());
        HttpClientContext context = HttpClientContext.create();
        BasicCredentialsProvider cp = new BasicCredentialsProvider();
        cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("mustermann", "wrong"));
        CloseableHttpClient client = HttpClients.custom().
            setDefaultCredentialsProvider(cp).
            build();
        HttpGet httpGet = new HttpGet(path);
        HttpResponse response = client.execute(httpGet, context);
        
        int statusCode = response.getStatusLine().getStatusCode();
        assertThat(statusCode, is(200));

        String text = EntityUtils.toString(response.getEntity());
        assertThat(text, containsString("plain text"));        
    }

    @Test
    public void shouldPermitUnauthenticatedAccessToUnprotectedResource() throws Exception {
        String path = String.format("http://localhost:%d/digest/plain.txt", getHttpPort());
        HttpClientContext context = HttpClientContext.create();
        CloseableHttpClient client = HttpClients.custom().build();
        HttpGet httpGet = new HttpGet(path);
        HttpResponse response = client.execute(httpGet, context);
        
        int statusCode = response.getStatusLine().getStatusCode();
        assertThat(statusCode, is(200));

        String text = EntityUtils.toString(response.getEntity());
        assertThat(text, containsString("plain text"));        
    }
}
