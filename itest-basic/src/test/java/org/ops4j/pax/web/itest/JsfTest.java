package org.ops4j.pax.web.itest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.linkBundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;

import java.io.ByteArrayOutputStream;
import java.io.File;
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
import org.ops4j.pax.exam.util.PathUtils;


@RunWith(PaxExam.class)
public class JsfTest {

    private static boolean consoleEnabled = Boolean.valueOf(System.getProperty("equinox.console",
        "true"));
    private static String httpPortNumber = System.getProperty("test.http.port", "8080");
    
//    @Inject
//    private ServletContext servletContext;

    @Configuration
    public Option[] config() {
        return options(
            when(consoleEnabled).useOptions(
                systemProperty("osgi.console").value("6666"),
                systemProperty("osgi.console.enable.builtin").value("true")),

            systemProperty("logback.configurationFile").value(
                "file:" + PathUtils.getBaseDir() + "/src/test/resources/logback.xml"),

            systemPackages("com.sun.org.apache.xalan.internal.res",
                    "com.sun.org.apache.xml.internal.utils", "com.sun.org.apache.xml.internal.utils",
                    "com.sun.org.apache.xpath.internal", "com.sun.org.apache.xpath.internal.jaxp",
                    "com.sun.org.apache.xpath.internal.objects"),
                
                
            linkBundle("slf4j.api"), linkBundle("ch.qos.logback.core"),
            linkBundle("ch.qos.logback.classic"),

            linkBundle("io.undertow.core"),
            linkBundle("io.undertow.servlet"),
            linkBundle("org.jboss.xnio.api"),
            linkBundle("org.jboss.xnio.nio"),
            linkBundle("org.jboss.logging.jboss-logging"),
            linkBundle("javax.servlet-api"),
            linkBundle("org.apache.felix.scr"),
            linkBundle("org.apache.xbean.bundleutils"),
            linkBundle("org.apache.xbean.finder"),
            linkBundle("org.objectweb.asm.all"),
            
            linkBundle("pax-web-sample-jsf"),
            workspaceBundle("org.ops4j.pax.web", "pax-web-extender"),
            workspaceBundle("org.ops4j.pax.web", "pax-web-api"),
            workspaceBundle("org.ops4j.pax.web", "pax-web-undertow"),
            
            mavenBundle("org.glassfish", "javax.faces", "2.2.7"),
            mavenBundle("javax.servlet.jsp", "javax.servlet.jsp-api", "2.3.1"),
            mavenBundle("javax.servlet.jsp.jstl", "javax.servlet.jsp.jstl-api", "1.2.1"),
            mavenBundle("org.glassfish.web", "javax.servlet.jsp.jstl", "1.2.3"),
            mavenBundle("org.glassfish", "javax.el", "3.0.0"),
            mavenBundle("javax.enterprise", "cdi-api", "1.2"),
            mavenBundle("javax.annotation", "javax.annotation-api", "1.2"),
            mavenBundle("javax.interceptor", "javax.interceptor-api", "1.2"),
            mavenBundle("javax.validation", "validation-api", "1.1.0.Final"),
            

            junitBundles());
    }
    
    public static Option workspaceBundle(String groupId, String artifactId) {
        String fileName = String.format("%s/../%s/target/classes",
            PathUtils.getBaseDir(), artifactId);
        
        if (new File(fileName).exists()) {
            String url = "reference:file:" + fileName;
            return bundle(url);            
        }
        else {
            return mavenBundle(groupId, artifactId).versionAsInProject();
        }
    }
    
    

    @Test
    public void runWabServlet() throws Exception {
        Thread.sleep(10000000);

        URL url = new URL(String.format("http://localhost:%s/wab/WABServlet", httpPortNumber));
        InputStream is = url.openStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        StreamUtils.copyStream(is, os, true);
        assertThat(os.toString(), containsString("symbolic name : wab-sample"));
    }
}
