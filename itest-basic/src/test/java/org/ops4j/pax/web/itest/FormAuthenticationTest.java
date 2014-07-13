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
import static org.ops4j.pax.web.itest.util.WebAssertions.*;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
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
public class FormAuthenticationTest {

    @Inject
    private ServletContext servletContext;

    @Configuration
    public Option[] config() {
        return options(
            linkBundle("pax-web-sample-auth-form"),
            httpClientBundles(),
            undertowBundles(),
            paxUndertowBundles(), 
            logbackBundles(), 
            junitBundles());
    }

    @Test
    public void shouldRedirectToLoginPage() throws Exception {
        assertThat(servletContext.getContextPath(), is("/form"));

        assertResourceContainsString("form/hello", "action=\"j_security_check\"");
    }
    
    @Test
    public void shouldDisplayProtectedPageAfterLogin() throws Exception {
        String path = String.format("http://localhost:%d/form/hello", getHttpPort());
        CloseableHttpClient client = HttpClients.createDefault();
        HttpClientContext context = HttpClientContext.create();
        
        HttpGet httpGet = new HttpGet(path);
        HttpResponse response = client.execute(httpGet, context);
        
        int statusCode = response.getStatusLine().getStatusCode();
        String text = EntityUtils.toString(response.getEntity());
        assertThat(text, containsString("Login"));      
        
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("j_username", "mustermann"));
        formparams.add(new BasicNameValuePair("j_password", "mustermann"));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
        
        path = String.format("http://localhost:%d/form/j_security_check", getHttpPort());        
        HttpPost httpPost = new HttpPost(path);
        httpPost.setEntity(entity);
        response = client.execute(httpPost, context);

        statusCode = response.getStatusLine().getStatusCode();
        assertThat(statusCode, is(302));
        String location = response.getFirstHeader("Location").getValue();
        assertThat(location, containsString("/form/hello"));
        
        httpGet = new HttpGet(location);
        response = client.execute(httpGet, context);

        statusCode = response.getStatusLine().getStatusCode();
        text = EntityUtils.toString(response.getEntity());
        assertThat(text, containsString("Hello from Pax Web!"));        
    }

    @Test
    public void shouldDenyAccessOnWrongPassword() throws Exception {
        String path = String.format("http://localhost:%d/form/hello", getHttpPort());
        CloseableHttpClient client = HttpClients.createDefault();
        HttpClientContext context = HttpClientContext.create();
        
        HttpGet httpGet = new HttpGet(path);
        HttpResponse response = client.execute(httpGet, context);
        
        int statusCode = response.getStatusLine().getStatusCode();
        String text = EntityUtils.toString(response.getEntity());
        assertThat(text, containsString("Login"));       
        
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("j_username", "mustermann"));
        formparams.add(new BasicNameValuePair("j_password", "wrong"));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
        
        path = String.format("http://localhost:%d/form/j_security_check", getHttpPort());                
        HttpPost httpPost = new HttpPost(path);
        httpPost.setEntity(entity);
        response = client.execute(httpPost, context);

        statusCode = response.getStatusLine().getStatusCode();
        assertThat(statusCode, is(200));
        text = EntityUtils.toString(response.getEntity());
        assertThat(text, containsString("failed"));        
    }

    @Test
    public void shouldPermitAccessToUnprotectedResource() throws Exception {
        assertThat(servletContext.getContextPath(), is("/form"));

        assertResourceContainsString("form/plain.txt", "plain text");
    }
}
