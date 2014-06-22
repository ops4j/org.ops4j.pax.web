package org.ops4j.pax.web.itest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.linkBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.web.itest.TestConfiguration.logbackBundles;
import static org.ops4j.pax.web.itest.TestConfiguration.mojarraBundles;
import static org.ops4j.pax.web.itest.TestConfiguration.undertowBundles;
import static org.ops4j.pax.web.itest.TestConfiguration.workspaceBundle;

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
public class JsfTest {

    private static boolean consoleEnabled = Boolean.valueOf(System.getProperty("equinox.console",
        "true"));
    private static String httpPortNumber = System.getProperty("test.http.port", "8080");
    
    @Inject
    private ServletContext servletContext;

    @Configuration
    public Option[] config() {
        return options(
            when(consoleEnabled).useOptions(
                systemProperty("osgi.console").value("6666"),
                systemProperty("osgi.console.enable.builtin").value("true")),
               
            undertowBundles(),
            linkBundle("org.apache.felix.scr"),
            linkBundle("org.apache.xbean.bundleutils"),
            linkBundle("org.apache.xbean.finder"),
            linkBundle("org.objectweb.asm.all"),
            
            linkBundle("pax-web-sample-jsf"),
            workspaceBundle("org.ops4j.pax.web", "pax-web-extender"),
            workspaceBundle("org.ops4j.pax.web", "pax-web-api"),
            workspaceBundle("org.ops4j.pax.web", "pax-web-undertow"),
            

            mojarraBundles(),
            logbackBundles(),
            junitBundles());
    }
    
    @Test
    public void runFacelet() throws Exception {
        assertThat(servletContext.getContextPath(), is("/jsf"));
        URL url = new URL(String.format("http://localhost:%s/jsf/poll.jsf", httpPortNumber));
        InputStream is = url.openStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        StreamUtils.copyStream(is, os, true);
        assertThat(os.toString(), containsString("Equinox"));
    }
}
