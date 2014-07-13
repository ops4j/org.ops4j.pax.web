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
package org.ops4j.pax.web.itest.util;

import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackages;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.linkBundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.ServiceLoader;

import org.ops4j.lang.Ops4jException;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.launch.FrameworkFactory;

public class TestConfiguration {

    private static boolean consoleEnabled = Boolean.valueOf(System.getProperty("equinox.console",
        "true"));
    public static Option undertowBundles() {
        return composite(
            mavenBundle("org.ops4j.pax.tipi", "org.ops4j.pax.tipi.undertow.servlet", "1.0.15.1"),
            mavenBundle("org.ops4j.pax.tipi", "org.ops4j.pax.tipi.undertow.core", "1.0.15.1"),
            mavenBundle("org.ops4j.pax.tipi", "org.ops4j.pax.tipi.xnio.api", "3.2.2.1"),
            mavenBundle("org.ops4j.pax.tipi", "org.ops4j.pax.tipi.xnio.nio", "3.2.2.1").noStart(),
            linkBundle("org.jboss.logging.jboss-logging"),
            mavenBundle("javax.annotation", "javax.annotation-api", "1.2"),
            linkBundle("javax.servlet-api"));
    }

    public static Option paxUndertowBundles() {
        return composite(linkBundle("org.apache.felix.scr"),
            linkBundle("org.apache.xbean.bundleutils"), linkBundle("org.apache.xbean.finder"),
            linkBundle("org.objectweb.asm.all"), linkBundle("org.apache.felix.jaas"),
            linkBundle("org.apache.karaf.jaas.boot"), linkBundle("org.apache.felix.eventadmin"),

            workspaceBundle("org.ops4j.pax.web", "pax-web-extender"),
            workspaceBundle("org.ops4j.pax.web", "pax-web-api"),
            workspaceBundle("org.ops4j.pax.web", "pax-web-undertow"));
    }

    public static Option logbackBundles() {
        return composite(
            when(consoleEnabled).useOptions(systemProperty("osgi.console").value("6666"),
                systemProperty("osgi.console.enable.builtin").value("true")),

            systemProperty("logback.configurationFile").value(
                "file:" + PathUtils.getBaseDir() + "/src/test/resources/logback.xml"),

            linkBundle("slf4j.api"), 
            linkBundle("jcl.over.slf4j"), 
            linkBundle("ch.qos.logback.core"),
            linkBundle("ch.qos.logback.classic"));
    }

    public static Option paxCdiSharedBundles() {
        return composite(
            systemProperty("org.ops4j.pax.url.mvn.repositories")
                .value(
                    "https://oss.sonatype.org/content/repositories/ops4j-snapshots@id=sonatype-nexus-snapshots@snapshots,"
                        + "http://repo1.maven.org/maven2@id=central"),
            workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-extender"),
            workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-extension"),
            workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-api"),
            workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-spi"),
            workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-servlet"));
    }

    public static Option paxCdiWithWeldBundles() {

        Properties props = new Properties();
        try {
            props.load(TestConfiguration.class.getResourceAsStream("/systemPackages.properties"));
        }
        catch (IOException exc) {
            throw new Ops4jException(exc);
        }

        return composite(
            // do not treat javax.annotation as system package
            when(isEquinox()).useOptions(
                frameworkProperty("org.osgi.framework.system.packages").value(
                    props.get("org.osgi.framework.system.packages"))),

            workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-weld"),
            workspaceBundle("org.ops4j.pax.cdi", "pax-cdi-undertow-weld"),
            mavenBundle("com.google.guava", "guava", "13.0.1"),
            mavenBundle("org.jboss.weld", "weld-osgi-bundle", "2.1.2.Final"));
    }

    public static boolean isEquinox() {
        FrameworkFactory factory = ServiceLoader.load(FrameworkFactory.class).iterator().next();
        return factory.getClass().getSimpleName().contains("Equinox");
    }

    public static Option mojarraBundles() {
        return composite(
            bootDelegationPackages("org.xml.sax", "org.xml.*", "org.w3c.*", "javax.xml.*",
                "javax.activation.*", "com.sun.org.apache.xpath.internal.jaxp"),

            systemPackages("com.sun.org.apache.xalan.internal.res",
                "com.sun.org.apache.xml.internal.utils", "com.sun.org.apache.xml.internal.utils",
                "com.sun.org.apache.xpath.internal", "com.sun.org.apache.xpath.internal.jaxp",
                "com.sun.org.apache.xpath.internal.objects", "org.w3c.dom.traversal"),
            mavenBundle("org.glassfish", "javax.faces", "2.2.7"),
            mavenBundle("javax.servlet.jsp", "javax.servlet.jsp-api", "2.3.1"),
            mavenBundle("javax.servlet.jsp.jstl", "javax.servlet.jsp.jstl-api", "1.2.1"),
            mavenBundle("org.glassfish.web", "javax.servlet.jsp.jstl", "1.2.3"),
            mavenBundle("org.glassfish", "javax.el", "3.0.0"),
            mavenBundle("javax.enterprise", "cdi-api", "1.2"),
            mavenBundle("javax.interceptor", "javax.interceptor-api", "1.2"),
            mavenBundle("javax.validation", "validation-api", "1.1.0.Final"));
    }


    public static Option httpClientBundles() {
        return composite(
            linkBundle("org.apache.httpcomponents.httpcore"),
            linkBundle("org.apache.httpcomponents.httpclient"));
    }

    
    public static Option workspaceBundle(String groupId, String artifactId) {
        String fileName = null;
        String version = null;
        if (groupId.equals("org.ops4j.pax.cdi")) {
            fileName = String.format("%s/../../org.ops4j.pax.cdi/%s/target/classes",
                PathUtils.getBaseDir(), artifactId);
            version = System.getProperty("version.pax.cdi", "0.8.0-SNAPSHOT");
        }
        else {
            fileName = String.format("%s/../%s/target/classes", PathUtils.getBaseDir(), artifactId);
        }
        if (new File(fileName).exists()) {
            String url = "reference:file:" + fileName;
            return bundle(url);
        }
        else {
            if (version == null) {
                return mavenBundle(groupId, artifactId).versionAsInProject();
            }
            else {
                return mavenBundle(groupId, artifactId, version);
            }
        }
    }

}
