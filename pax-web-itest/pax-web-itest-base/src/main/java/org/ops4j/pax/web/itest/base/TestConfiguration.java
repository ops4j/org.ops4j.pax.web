package org.ops4j.pax.web.itest.base;

import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackages;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.linkBundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.MavenUtils.asInProject;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.ServiceLoader;

import org.ops4j.lang.Ops4jException;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.launch.FrameworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestConfiguration {
	
	static final Logger LOG = LoggerFactory.getLogger(TestConfiguration.class);
	
    static {
        try {
            Properties props = new Properties();
            props.load(TestConfiguration.class.getResourceAsStream("/versions.properties"));
            PAX_CDI_VERSION = props.getProperty("version.pax.cdi");
            PAX_WEB_VERSION = props.getProperty("version.pax.web");
            OWB_VERSION = props.getProperty("version.openwebbeans");
        }
        catch (IOException exc) {
            throw new IllegalArgumentException(exc);
        }
    }
    
    // the name of the system property which captures the jococo coverage agent command
    //if specified then agent would be specified otherwise ignored
    public static final String COVERAGE_COMMAND = "coverage.command";

    public static final String PAX_CDI_VERSION;
    public static final String PAX_WEB_VERSION;
    public static final String OWB_VERSION;

    private static boolean consoleEnabled =
        Boolean.valueOf(System.getProperty("org.ops4j.pax.web.console", "false"));

    
	public static Option addCodeCoverageOption() {
		String coverageCommand = System.getProperty(COVERAGE_COMMAND);
		if (coverageCommand != null && coverageCommand.length() > 0) {
			LOG.info("found coverage option {}", coverageCommand);
			return CoreOptions.vmOption(coverageCommand);
		}
		return null;
	}
	
	public static Option undertowBundles() {
        return composite(
            mavenBundle("org.ops4j.pax.tipi", "org.ops4j.pax.tipi.undertow.servlet", "1.1.0.1"),
            mavenBundle("org.ops4j.pax.tipi", "org.ops4j.pax.tipi.undertow.core", "1.1.0.1"),
            mavenBundle("org.ops4j.pax.tipi", "org.ops4j.pax.tipi.xnio.api", "3.3.0.1"),
            mavenBundle("org.ops4j.pax.tipi", "org.ops4j.pax.tipi.xnio.nio", "3.3.0.1"),
            linkBundle("org.jboss.logging.jboss-logging"),
            mavenBundle("javax.annotation", "javax.annotation-api", "1.2"),
            linkBundle("javax.servlet-api"));
    }
	
	public static Option paxWebBundles() {
		return composite(
				linkBundle("org.apache.felix.scr"),
	            linkBundle("org.apache.xbean.bundleutils"),
	            linkBundle("org.apache.xbean.finder"),
                mavenBundle().groupId("org.apache.xbean")
                        .artifactId("xbean-reflect").version(asInProject()),
	            linkBundle("org.objectweb.asm.all"),
				
	            linkBundle("org.apache.felix.jaas"),
	            linkBundle("org.apache.felix.configadmin").startLevel(2),
	            linkBundle("org.apache.felix.eventadmin"),
	            
	            mavenBundle("javax.annotation", "javax.annotation-api", "1.2"),
	            mavenBundle().groupId("org.ops4j.pax.url")
					.artifactId("pax-url-war")
					.type("jar")
					.classifier("uber")
					.version(asInProject()),
					
	            workspaceBundle("org.ops4j.pax.web", "pax-web-spi"),
	            workspaceBundle("org.ops4j.pax.web", "pax-web-descriptor"),
	            workspaceBundle("org.ops4j.pax.web", "pax-web-jaas"),
	            workspaceBundle("org.ops4j.pax.web", "pax-web-extender"),
	            workspaceBundle("org.ops4j.pax.web", "pax-web-api"),
	            workspaceBundle("org.ops4j.pax.web", "pax-web-extender-whiteboard"),
	            workspaceBundle("org.ops4j.pax.web", "pax-web-jsp"),
	            mavenBundle().groupId("org.eclipse.jdt.core.compiler")
					.artifactId("ecj").version(asInProject())
	            
				);
		
	}
	
	

    public static Option paxUndertowBundles() {
        return composite(linkBundle("org.apache.felix.scr"),
            workspaceBundle("org.ops4j.pax.web", "pax-web-jaas"),
            workspaceBundle("org.ops4j.pax.web", "pax-web-undertow"));
    }

    public static Option logbackBundles() {
        return composite(
            when(consoleEnabled).useOptions(systemProperty("osgi.console").value("6666"),
                systemProperty("osgi.console.enable.builtin").value("true")),

            when(consoleEnabled && isFelix()).useOptions(
                mavenBundle("org.apache.felix", "org.apache.felix.gogo.command", "0.14.0"),
                mavenBundle("org.apache.felix", "org.apache.felix.gogo.runtime", "0.12.1"),
                mavenBundle("org.apache.felix", "org.apache.felix.gogo.shell", "0.10.0")),

            systemProperty("logback.configurationFile").value(
                "file:" + PathUtils.getBaseDir() + "/src/test/resources/logback.xml"),

            linkBundle("slf4j.api"),
            linkBundle("jcl.over.slf4j"),
            linkBundle("ch.qos.logback.core"),
            linkBundle("ch.qos.logback.classic"));
    }

    public static Option paxCdiSharedBundles() {
        return composite(
            linkBundle("org.ops4j.pax.cdi.extender"),
            linkBundle("org.ops4j.pax.cdi.extension"),
            linkBundle("org.ops4j.pax.cdi.api"),
            linkBundle("org.ops4j.pax.cdi.spi"),
            linkBundle("org.ops4j.pax.cdi.servlet"));
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

            linkBundle("org.ops4j.pax.cdi.weld"),

            // there is a classloader conflict when adding this dep to the POM
            mavenBundle("org.ops4j.pax.cdi", "pax-cdi-undertow-weld", PAX_CDI_VERSION),

            mavenBundle("com.google.guava", "guava", "13.0.1"),
            mavenBundle("org.jboss.weld", "weld-osgi-bundle", "2.2.8.Final"));
    }

    public static Option paxCdiWithOwbBundles() {

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

            linkBundle("org.ops4j.pax.cdi.openwebbeans"),

            // there is a classloader conflict when adding this dep to the POM
            mavenBundle("org.ops4j.pax.cdi", "pax-cdi-undertow-openwebbeans", PAX_CDI_VERSION),

            mavenBundle("org.apache.openwebbeans", "openwebbeans-impl", "1.5.0"),
            mavenBundle("org.apache.openwebbeans", "openwebbeans-spi", "1.5.0"),
            mavenBundle("org.apache.openwebbeans", "openwebbeans-web", "1.5.0"),
            mavenBundle("org.apache.openwebbeans", "openwebbeans-el22", "1.5.0"),
            // needed by pax-cdi-web-openwebbeans
            mavenBundle("org.apache.geronimo.specs", "geronimo-jsp_2.2_spec", "1.2"),

            mavenBundle("org.apache.xbean", "xbean-asm5-shaded", "4.1"), //
            mavenBundle("org.apache.xbean", "xbean-finder-shaded", "4.1"), //
            //mavenBundle("org.slf4j", "jul-to-slf4j").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec", "1.1.1"),
            mavenBundle("javax.validation", "validation-api", "1.1.0.Final"),
            mavenBundle("javax.interceptor", "javax.interceptor-api", "1.2"),

            mavenBundle("javax.el", "javax.el-api", "3.0.0"));

    }

    public static boolean isEquinox() {
        FrameworkFactory factory = ServiceLoader.load(FrameworkFactory.class).iterator().next();
        return factory.getClass().getSimpleName().contains("Equinox");
    }

    public static boolean isFelix() {
        FrameworkFactory factory = ServiceLoader.load(FrameworkFactory.class).iterator().next();
        return factory.getClass().getCanonicalName().contains("felix");
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
    		mavenBundle("commons-codec", "commons-codec").version(asInProject()),
            linkBundle("org.apache.httpcomponents.httpcore"),
            linkBundle("org.apache.httpcomponents.httpmime"),
            linkBundle("org.apache.httpcomponents.httpclient"));
    }
    
    

    public static Option workspaceBundle(String groupId, String artifactId) {
        String fileName = null;
        String version = null;
        if (groupId.equals("org.ops4j.pax.cdi")) {
            fileName = String.format("%s/../../../../org.ops4j.pax.cdi/%s/target/classes",
                PathUtils.getBaseDir(), artifactId);
            version = System.getProperty("version.pax.cdi", PAX_CDI_VERSION);
        }
        else {
            fileName = String.format("%s/../../%s/target/classes", PathUtils.getBaseDir(),
                artifactId);
        }
        if (new File(fileName).exists()) {
            String url = "reference:file:" + fileName;
            return bundle(url);
        }
        else {
            if (version == null) {
                return mavenBundle(groupId, artifactId, PAX_WEB_VERSION);
            }
            else {
                return mavenBundle(groupId, artifactId, version);
            }
        }
    }

}
