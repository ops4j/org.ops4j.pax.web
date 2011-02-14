package org.ops4j.pax.web.itest.war;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.*;

import java.util.Dictionary;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;



/**
 * @author Achim Nierbeck
 */
@RunWith( JUnit4TestRunner.class )
public class WarIntegrationTest
{

	Log LOG = LogFactory.getLog(WarIntegrationTest.class);
	
    @Inject
    BundleContext bundleContext = null;

	private Bundle installWarBundle;
    
    private static final String WEB_CONTEXT_PATH="Web-ContextPath";
    private static final String WEB_BUNDLE = "webbundle:";
    
    private WebListener webListener;
    
    @Configuration
    public static Option[] configure() {
    	return options(
    			// systemProperty( "workingDirectory" ).value( "./target/pax-exam" ),
    			// install log service using pax runners profile abstraction (there are more profiles, like DS)
                logProfile(),
                // this is how you set the default log level when using pax logging (logProfile)
                systemProperty( "org.ops4j.pax.logging.DefaultServiceLog.level" ).value( "DEBUG" ),
                systemProperty("org.osgi.service.webcontainer.hostname").value("127.0.0.1"),
                systemProperty("org.osgi.service.webcontainer.http.port").value("8080"),
                systemProperty("java.protocol.handler.pkgs").value("org.ops4j.pax.url"),
                systemProperty("org.ops4j.pax.url.war.importPaxLoggingPackages").value("true"),
                configProfile(),
                compendiumProfile(),
                mavenBundle().groupId( "org.ops4j.pax.url" ).artifactId( "pax-url-war" ).version( asInProject() ),
                mavenBundle().groupId( "org.ops4j.pax.web" ).artifactId( "pax-web-spi" ).version( "1.1.0-SNAPSHOT" ),
                mavenBundle().groupId( "org.ops4j.pax.web" ).artifactId( "pax-web-api" ).version( "1.1.0-SNAPSHOT" ),
                mavenBundle().groupId( "org.ops4j.pax.web" ).artifactId( "pax-web-extender-war" ).version( "1.1.0-SNAPSHOT" ),
                mavenBundle().groupId( "org.ops4j.pax.web" ).artifactId( "pax-web-extender-whiteboard" ).version( "1.1.0-SNAPSHOT" ),
                mavenBundle().groupId( "org.ops4j.pax.web" ).artifactId( "pax-web-jetty" ).version( "1.1.0-SNAPSHOT" ),
                mavenBundle().groupId( "org.ops4j.pax.web" ).artifactId( "pax-web-runtime" ).version( "1.1.0-SNAPSHOT" ),
                mavenBundle().groupId( "org.ops4j.pax.web" ).artifactId( "pax-web-jsp" ).version( "1.1.0-SNAPSHOT" ),
                mavenBundle().groupId( "org.eclipse.jetty" ).artifactId( "jetty-util" ).version( asInProject() ),
                mavenBundle().groupId( "org.eclipse.jetty" ).artifactId( "jetty-io" ).version( asInProject() ),
                mavenBundle().groupId( "org.eclipse.jetty" ).artifactId( "jetty-http" ).version( asInProject() ),
                mavenBundle().groupId( "org.eclipse.jetty" ).artifactId( "jetty-continuation" ).version( asInProject() ),
                mavenBundle().groupId( "org.eclipse.jetty" ).artifactId( "jetty-server" ).version( asInProject() ),
                mavenBundle().groupId( "org.eclipse.jetty" ).artifactId( "jetty-security" ).version( asInProject() ),
                mavenBundle().groupId( "org.eclipse.jetty" ).artifactId( "jetty-xml" ).version( asInProject() ),
                mavenBundle().groupId( "org.eclipse.jetty" ).artifactId( "jetty-servlet" ).version( asInProject() ),
                mavenBundle().groupId( "org.apache.geronimo.specs" ).artifactId( "geronimo-servlet_2.5_spec" ).version( asInProject() ), 
                mavenBundle().groupId( "org.ops4j.pax.url" ).artifactId( "pax-url-mvn" ).version( asInProject() ) ,
                mavenBundle("commons-codec","commons-codec"),
                wrappedBundle(mavenBundle("commons-httpclient","commons-httpclient","3.1"))/*,
                vmOption( "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005" ), waitForFrameworkStartup()*/
    	);
    }

    @Before
    public void setUp() throws BundleException, InterruptedException {
    	LOG.info("Setting up test");
    	webListener = new WebListenerImpl();
    	bundleContext.registerService(WebListener.class.getName(), webListener, null);
    	String bundlePath=WEB_BUNDLE+"mvn:org.ops4j.pax.web.samples/war/1.1.0-SNAPSHOT/war?"+WEB_CONTEXT_PATH+"=/war";
    	installWarBundle = bundleContext.installBundle(bundlePath);
    	installWarBundle.start();
    	
    	while(!((WebListenerImpl)webListener).gotEvent()) {
    		this.wait(100);
    	}
    }
    
    @After
    public void tearDown() throws BundleException {
    	if (installWarBundle != null) {
    		installWarBundle.stop();
    		installWarBundle.uninstall();
    	}
    }
    
    
    /**
     * You will get a list of bundles installed by default
     * plus your testcase, wrapped into a bundle called pax-exam-probe
     */
    @Test
    public void listBundles()
    {
        for( Bundle b : bundleContext.getBundles() )
        {
        	if (b.getState() != Bundle.ACTIVE)
        		fail("Bundle should be active: "+b);
        	
        	Dictionary headers = b.getHeaders();
        	String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
        	if (ctxtPath != null)
        		System.out.println( "Bundle " + b.getBundleId() + " : " + b.getSymbolicName() + " : " + ctxtPath);
        	else
        		System.out.println( "Bundle " + b.getBundleId() + " : " + b.getSymbolicName() );
        }

    }
    
    @Test
    public void testWebContextPath() throws Exception {

    	GetMethod get = null;
    	try {
            HttpClient client = new HttpClient();
            get = new GetMethod("http://127.0.0.1:8080/war/wc/example");
            int executeMethod = client.executeMethod(get);
            assertEquals("HttpResponseCode",200,executeMethod);
            String responseBodyAsString = get.getResponseBodyAsString();
            assertTrue(responseBodyAsString.contains("<h1>Hello World</h1>"));
    	} finally {
    		if (get != null)
    			get.releaseConnection();
    	}
    }
    
    private class WebListenerImpl implements WebListener {

    	private boolean event = false;
    	
		public void webEvent(WebEvent event) {
			LOG.info("Got event: "+event);
			this.event = true;
		}

		public boolean gotEvent() {
			return event;
		}
    	
    }
    
}