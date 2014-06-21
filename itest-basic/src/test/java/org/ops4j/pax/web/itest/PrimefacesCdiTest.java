package org.ops4j.pax.web.itest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackages;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.linkBundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Properties;

import javax.inject.Inject;

import org.apache.xbean.osgi.bundle.util.DelegatingBundle;
import org.apache.xbean.osgi.bundle.util.equinox.EquinoxBundleClassLoader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.io.StreamUtils;
import org.ops4j.lang.Ops4jException;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.PathUtils;
import org.ops4j.pax.swissbox.core.BundleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;


@RunWith(PaxExam.class)
public class PrimefacesCdiTest {

    private static boolean consoleEnabled = Boolean.valueOf(System.getProperty("equinox.console",
        "true"));
    private static String httpPortNumber = System.getProperty("test.http.port", "8080");
    
//    @Inject
//    private ServletContext servletContext;
    
    @Inject
    private BundleContext bc;
private Bundle primefaces;

    @Configuration
    public Option[] config() {
        Properties props = new Properties();
        try {
            props.load(getClass().getResourceAsStream("/systemPackages.properties"));
        }
        catch (IOException exc) {
            throw new Ops4jException(exc);
        }
        

        return options(
            when(consoleEnabled).useOptions(
                systemProperty("osgi.console").value("6666"),
                systemProperty("osgi.console.enable.builtin").value("true")),
                systemTimeout(100000000),

            bootDelegationPackages(
                "org.xml.sax", "org.xml.*", "org.w3c.*", "javax.xml.*",
                "javax.activation.*", "com.sun.org.apache.xpath.internal.jaxp"
                ),
                
            // do not treat javax.annotation as system package
            frameworkProperty("org.osgi.framework.system.packages").value(props.get("org.osgi.framework.system.packages")),

            systemProperty("logback.configurationFile").value(
                "file:" + PathUtils.getBaseDir() + "/src/test/resources/logback.xml"),

            systemPackages(
                "com.sun.org.apache.xalan.internal.res",
                    "com.sun.org.apache.xml.internal.utils", "com.sun.org.apache.xml.internal.utils",
                    "com.sun.org.apache.xpath.internal", "com.sun.org.apache.xpath.internal.jaxp",
                    "com.sun.org.apache.xpath.internal.objects"
                    ),
                
                
            linkBundle("slf4j.api"), linkBundle("ch.qos.logback.core"),
            linkBundle("ch.qos.logback.classic"),

            linkBundle("io.undertow.core"),
            linkBundle("io.undertow.servlet"),
            linkBundle("org.jboss.xnio.api"),
            linkBundle("org.jboss.xnio.nio"),
//            mavenBundle("org.ops4j.pax.tipi", "org.ops4j.pax.tipi.undertow.servlet", "1.0.15.1-SNAPSHOT"),
//            mavenBundle("org.ops4j.pax.tipi", "org.ops4j.pax.tipi.undertow.core", "1.0.15.1-SNAPSHOT"),
//            mavenBundle("org.ops4j.pax.tipi", "org.ops4j.pax.tipi.xnio.api", "3.2.2.1-SNAPSHOT"),
//            mavenBundle("org.ops4j.pax.tipi", "org.ops4j.pax.tipi.xnio.nio", "3.2.2.1-SNAPSHOT").noStart(),
            linkBundle("org.jboss.logging.jboss-logging"),
            linkBundle("javax.servlet-api"),
            linkBundle("org.apache.felix.scr"),
            linkBundle("org.apache.xbean.bundleutils"),
            linkBundle("org.apache.xbean.finder"),
            linkBundle("org.objectweb.asm.all"),
            
            linkBundle("pax-web-sample-primefaces-cdi"),
            linkBundle("org.primefaces"),
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
            
            workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-extender"),
            workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-extension"),
            workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-api"),
            workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-spi"),
            workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-weld"),
            workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-undertow-weld"),
            workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-servlet"),
            
            mavenBundle("com.google.guava", "guava", "13.0.1"),
            mavenBundle("org.jboss.weld", "weld-osgi-bundle", "2.1.2.Final"),
            
            

            junitBundles());
    }
    
    public static Option workspaceBundle(String groupId, String artifactId) {
        String fileName = null;
        if (groupId.equals("org.ops4j.pax.cdi")) {
            fileName = String.format("/home/hwellmann/work/pax-cdi/%s/target/classes", artifactId);            
        }
        else {
            fileName = String.format("%s/../%s/target/classes", PathUtils.getBaseDir(), artifactId);
        }
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

    //@Test
    public void shouldLoadResources() {
        primefaces = BundleUtils.getBundle(bc, "org.primefaces");
        Bundle jsf = BundleUtils.getBundle(bc, "org.glassfish.javax.faces");
        ClassLoader cl1 = primefaces.adapt(BundleWiring.class).getClassLoader();
        URL url1 = cl1.getResource("META-INF/primefaces-p.taglib.xml");
        URL url2 = cl1.getResource("META-INF/");
        System.out.println();
        
        EquinoxBundleClassLoader bcl = new EquinoxBundleClassLoader(new DelegatingBundle(Arrays.asList(primefaces, jsf)), true, true);
        URL resource = bcl.getResource("META-INF/");
        System.out.println();
    }
}
