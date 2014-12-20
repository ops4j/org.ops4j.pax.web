package org.ops4j.pax.web.itest.tomcat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.workingDirectory;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;

import javax.inject.Inject;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

import org.apache.catalina.Globals;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.web.itest.base.HttpTestClient;
import org.ops4j.pax.web.itest.base.ServletListenerImpl;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.WaitCondition;
import org.ops4j.pax.web.itest.base.WebListenerImpl;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExamReactorStrategy(PerClass.class)
public class ITestBase {

	protected static final String WEB_CONTEXT_PATH = "Web-ContextPath";
	protected static final String WEB_CONNECTORS = "Web-Connectors";
	protected static final String WEB_VIRTUAL_HOSTS = "Web-VirtualHosts";
	protected static final String WEB_BUNDLE = "webbundle:";

  protected static final String COVERAGE_COMMAND = "coverage.command";


	protected static final String REALM_NAME = "realm.properties";

	private static final Logger LOG = LoggerFactory.getLogger(ITestBase.class);

	@Inject
	protected BundleContext bundleContext;

	protected WebListener webListener;

	protected ServletListener servletListener;

	protected HttpTestClient testClient;

	public static Option[] baseConfigure() {
		return options(
				workingDirectory("target/paxexam/"),
				cleanCaches(true),
				junitBundles(),
				frameworkProperty("osgi.console").value("6666"),
				frameworkProperty("osgi.console.enable.builtin").value("true"),
				frameworkProperty("felix.bootdelegation.implicit").value(
						"false"),
				// frameworkProperty("felix.log.level").value("4"),
				systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level")
						.value("INFO"),
				systemProperty("org.osgi.service.http.hostname").value(
						"127.0.0.1"),
				systemProperty("org.osgi.service.http.port").value("8181"),
				systemProperty("java.protocol.handler.pkgs").value(
						"org.ops4j.pax.url"),
				systemProperty("org.ops4j.pax.url.war.importPaxLoggingPackages")
						.value("true"),
				systemProperty("org.ops4j.pax.web.log.ncsa.enabled").value(
						"true"),
				systemProperty("org.ops4j.pax.web.log.ncsa.directory").value(
						"target/logs"),
				systemProperty("ProjectVersion").value(
						VersionUtil.getProjectVersion()),

				mavenBundle().groupId("org.ops4j.pax.web.itest")
						.artifactId("pax-web-itest-base").versionAsInProject(),

				// do not include pax-logging-api, this is already provisioned
				// by Pax Exam
				mavenBundle().groupId("org.ops4j.pax.logging")
						.artifactId("pax-logging-service")
						.version("1.8.1"),
				mavenBundle().groupId("org.ops4j.pax.logging")
					.artifactId("pax-logging-api")
					.version("1.8.1"),

				mavenBundle().groupId("org.ops4j.pax.url")
						.artifactId("pax-url-war").type("jar").classifier("uber").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-spi").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-api").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-extender-war")
						.version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-extender-whiteboard")
						.version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-runtime").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-jsp").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jdt.core.compiler")
						.artifactId("ecj").version(asInProject()),
				mavenBundle().groupId("org.ops4j.pax.url")
						.artifactId("pax-url-aether").version(asInProject()),
						
                mavenBundle().groupId("org.apache.xbean")
                        .artifactId("xbean-reflect").version(asInProject()),
            	mavenBundle().groupId("org.apache.xbean")
                        .artifactId("xbean-finder").version(asInProject()),
                mavenBundle().groupId("org.apache.xbean")
                        .artifactId("xbean-bundleutils").version(asInProject()),
                mavenBundle().groupId("org.ow2.asm")
                        .artifactId("asm-all").version(asInProject()),
                        
                mavenBundle("commons-codec", "commons-codec").version(
						asInProject()),
				mavenBundle("org.apache.felix", "org.apache.felix.eventadmin")
						.version(asInProject()),
				wrappedBundle(mavenBundle("org.apache.httpcomponents",
						"httpcore").version(asInProject())),
				wrappedBundle(mavenBundle("org.apache.httpcomponents",
						"httpmime").version(asInProject())),
				wrappedBundle(mavenBundle("org.apache.httpcomponents",
						"httpclient").version(asInProject())));
	}

	public static Option[] configureBaseWithServlet() {
		return combine(
				baseConfigure(),
				mavenBundle().groupId("javax.servlet")
				.artifactId("javax.servlet-api").versionAsInProject());
	}

	public static Option[] configureTomcat() {
		return combine(
				configureBaseWithServlet(),
				systemPackages(
						"javax.xml.namespace;version=1.0.0",
						"javax.transaction;version=1.1.0",
						"javax.servlet;version=2.6.0",
						"javax.servlet;version=3.0.0",
						"javax.servlet.descriptor;version=2.6.0",
						"javax.servlet.descriptor;version=3.0.0",
						"javax.annotation.processing;uses:=javax.tools,javax.lang.model,javax.lang.model.element,javax.lang.model.util;version=1.1",
						"javax.annotation;version=1.1",
						"javax.annotation.security;version=1.1"),
				systemProperty("org.osgi.service.http.hostname").value(
						"127.0.0.1"),
				systemProperty("org.osgi.service.http.port").value("8282"),
				systemProperty("javax.servlet.context.tempdir").value("target"),
				systemProperty("org.ops4j.pax.web.log.ncsa.directory").value(
						"logs"),
				systemProperty(Globals.CATALINA_BASE_PROP).value("target"),
				mavenBundle().groupId("org.ops4j.pax.web")
						.artifactId("pax-web-tomcat").version(asInProject()),

				mavenBundle().groupId("org.ops4j.pax.tipi")
						.artifactId("org.ops4j.pax.tipi.tomcat-embed-core")
						.version(asInProject()),

				mavenBundle()
						.groupId("org.ops4j.pax.tipi")
						.artifactId(
								"org.ops4j.pax.tipi.tomcat-embed-logging-juli")
						.version(asInProject()),

				mavenBundle().groupId("org.ops4j.pax.tipi")
						.artifactId("org.ops4j.pax.tipi.tomcat-embed-websocket")
						.version(asInProject()),

				mavenBundle().groupId("org.apache.servicemix.specs")
						.artifactId("org.apache.servicemix.specs.saaj-api-1.3")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.servicemix.specs")
						.artifactId("org.apache.servicemix.specs.jaxb-api-2.2")
						.version(asInProject()),

				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-jaxws_2.2_spec")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-jaxrpc_1.1_spec")
						.version(asInProject()),

				mavenBundle().groupId("javax.websocket")
						.artifactId("javax.websocket-api")
						.versionAsInProject(),
						
				mavenBundle()
						.groupId("org.apache.servicemix.specs")
						.artifactId(
								"org.apache.servicemix.specs.jsr303-api-1.0.0")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-activation_1.1_spec")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-stax-api_1.2_spec")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-ejb_3.1_spec")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-jpa_2.0_spec")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-javamail_1.4_spec")
						.version(asInProject()),
				mavenBundle().groupId("org.apache.geronimo.specs")
						.artifactId("geronimo-osgi-registry")
						.version(asInProject()));
	}

	@Before
	public void setUpITestBase() throws Exception {
		testClient = new HttpTestClient();
	}

	@After
	public void tearDownITestBase() throws Exception {
	    testClient.close();
	    testClient = null;
	}

    private static Option addCodeCoverageOption() {
		String coverageCommand = System.getProperty(COVERAGE_COMMAND);
		//System.out.println("*********** coverag command " + coverageCommand);
		if (coverageCommand != null) {
			LOG.info("covering code with command " + coverageCommand);
			return CoreOptions.vmOption(coverageCommand);
		}
		return null;
	}

	protected void initWebListener() {
		webListener = new WebListenerImpl();
		bundleContext.registerService(WebListener.class, webListener, null);
	}

	protected void initServletListener() {
		initServletListener(null);
	}

	protected void initServletListener(String servletName) {
		if (servletName == null)
			servletListener = new ServletListenerImpl();
		else
			servletListener = new ServletListenerImpl(servletName);
		bundleContext.registerService(ServletListener.class, servletListener,
				null);
	}

	protected void waitForWebListener() throws InterruptedException {
		new WaitCondition("webapp startup") {
			@Override
			protected boolean isFulfilled() {
				return ((WebListenerImpl) webListener).gotEvent();
			}
		}.waitForCondition(); 
	}

	protected void waitForServletListener() throws InterruptedException {
		new WaitCondition("servlet startup") {
			@Override
			protected boolean isFulfilled() {
				return ((ServletListenerImpl) servletListener).gotEvent();
			}
		}.waitForCondition(); 
	}

	protected void waitForServer(final String path) throws InterruptedException {
		new WaitCondition("server") {
			@Override
			protected boolean isFulfilled() throws Exception {
				return testClient.checkServer(path);
			}
		}.waitForCondition();
	}

	protected Bundle installAndStartBundle(String bundlePath)
			throws BundleException, InterruptedException {
		final Bundle bundle = bundleContext.installBundle(bundlePath);
		bundle.start();
		new WaitCondition("bundle startup") {
			@Override
			protected boolean isFulfilled() {
				return bundle.getState() == Bundle.ACTIVE;
			}
		}.waitForCondition();
		return bundle;
	}

}
