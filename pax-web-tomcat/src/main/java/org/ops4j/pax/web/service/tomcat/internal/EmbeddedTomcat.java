package org.ops4j.pax.web.service.tomcat.internal;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;

import org.apache.catalina.AccessLog;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Executor;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.ContainerBase;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Catalina;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.coyote.http11.Http11Protocol;
import org.apache.tomcat.util.digester.Digester;
import org.ops4j.pax.web.service.spi.Configuration;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class EmbeddedTomcat extends Tomcat {
	public static final String SERVER_CONFIG_FILE_NAME = "tomcat-server.xml";

	private static final Logger LOG = LoggerFactory
			.getLogger(EmbeddedTomcat.class);

	private File configurationDirectory;

	private Integer configurationSessionTimeout;

	private String configurationSessionCookie;

	private String configurationSessionUrl;

	private Boolean configurationSessionCookieHttpOnly;

	private String configurationWorkerName;

	private EmbeddedTomcat() {
	}

	static EmbeddedTomcat newEmbeddedTomcat(Configuration configuration) {
		EmbeddedTomcat result = new EmbeddedTomcat();
		result.getServer().setCatalina(new FakeCatalina());
		result.getService().setName("Catalina");
		result.getEngine().setName("Catalina");
		result.configure(configuration);
		return result;
	}

	public void setServer(Server server) {
		Service[] findServices = server.findServices();
		for (Service service : findServices) {
			Service existingService = getServer()
					.findService(service.getName());
			if (existingService != null) {
				for (Connector connector : service.findConnectors()) {
					existingService.addConnector(connector);
				}
				for (Executor executor : service.findExecutors()) {
					existingService.addExecutor(executor);
				}
				for (LifecycleListener lifecycleListener : service
						.findLifecycleListeners()) {
					existingService.addLifecycleListener(lifecycleListener);
				}
				existingService.getContainer().setRealm(
						service.getContainer().getRealm());
				existingService.getContainer().setBackgroundProcessorDelay(
						service.getContainer().getBackgroundProcessorDelay());
				existingService.getContainer().setCluster(
						service.getContainer().getCluster());
				existingService.getContainer().setResources(
						service.getContainer().getResources());
			} else {
				getServer().addService(service);
			}
		}
		this.setHostname(server.getAddress());

		this.setPort(server.getPort());
	}

	private static class FakeCatalina extends Catalina {
		@Override
		protected Digester createStartDigester() {
			Digester digester = super.createStartDigester();
			digester.setClassLoader(getClass().getClassLoader());
			return digester;
		}
	}

	void configure(Configuration configuration) {
		long start = System.nanoTime();
		initBaseDir(configuration);
		Digester digester = new FakeCatalina().createStartDigester();
		// digester.setClassLoader(classLoader); //TODO see if we need to work
		// on class loader
		digester.push(this);

		URL tomcatResource = configuration.getConfigurationURL();
		if (tomcatResource == null) {
			tomcatResource = getClass().getResource("/tomcat-server.xml");
		}

		File configurationFile = new File(configuration.getConfigurationDir(),
				SERVER_CONFIG_FILE_NAME);
		if (configurationFile.exists()) {
			try {
				tomcatResource = configurationFile.toURI().toURL();
			} catch (MalformedURLException e) {
				LOG.error("Exception while starting Tomcat:", e);
				throw new RuntimeException("Exception while starting Tomcat", e);
			}
		}
		if (tomcatResource != null) {
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(
						getClass().getClassLoader());
				LOG.debug("Configure using resource " + tomcatResource);

				digester.parse(tomcatResource.openStream());
				long elapsed = start - System.nanoTime();
				if (LOG.isInfoEnabled()) {
					LOG.info("configuration processed in {} ms",
							(elapsed / 1000000));
				}
			} catch (IOException e) {
				LOG.error("Exception while starting Tomcat:", e);
				throw new RuntimeException("Exception while starting Tomcat", e);
			} catch (SAXException e) {
				LOG.error("Exception while starting Tomcat:", e);
				throw new RuntimeException("Exception while starting Tomcat", e);
			} finally {
				Thread.currentThread().setContextClassLoader(loader);
			}
		}

		mergeConfiguration(configuration);
		
	}

	private void mergeConfiguration(Configuration configuration) {
		LOG.debug("Start merging configuration");
		Connector httpConnector = null;
		Connector httpSecureConnector = null;
		String[] addresses = configuration.getListeningAddresses();
		if (addresses == null || addresses.length == 0) {
			addresses = new String[] { null };
		}
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("javax.servlet.context.tempdir",
				configuration.getTemporaryDirectory());

		// Fix for PAXWEB-193
		configurationDirectory = configuration.getConfigurationDir(); 
		configurationSessionTimeout = configuration.getSessionTimeout();
		configurationSessionCookie = configuration.getSessionCookie();
		configurationSessionUrl = configuration.getSessionUrl();
		configurationSessionCookieHttpOnly = configuration
				.getSessionCookieHttpOnly();
		configurationWorkerName = configuration.getWorkerName();

		for (int i = 0; i < addresses.length; i++) {
			LOG.debug("Loop {} of {}", i, addresses.length);
			// configuring hosts
			String address = addresses[i];
			LOG.debug("configuring host with address: {}", address);

			Host host = null;

			if (i == 0) {
				host = getHost();
				LOG.debug("retrieved existing host: {}", host);
			} else {
				host = new StandardHost();
				LOG.debug("created a new StandardHost: {}", host);
			}
			host.setName(addresses[i]);
			host.setAutoDeploy(false);
			LOG.debug("re-configured host to {}", host);
			if (i == 0) {
				getEngine().setDefaultHost(address);
			}

			if (i > 0) {
				getEngine().addChild(host);
			}
		}

		// NCSA Logger --> AccessLogValve
		if (configuration.isLogNCSAFormatEnabled()) {
			AccessLog ncsaLogger = new AccessLogValve();
			boolean modifiedValve = false;
			for (Valve valve : getHost().getPipeline().getValves()) {
				if (valve instanceof AccessLogValve) {
					modifiedValve = true;
					ncsaLogger = (AccessLog) valve;
				}
			}

			((AccessLogValve) ncsaLogger).setPattern("common");
			((AccessLogValve) ncsaLogger).setDirectory(configuration
					.getLogNCSADirectory());
			// ((AccessLogValve)
			// ncsaLogger).setPrefix(configuration.getLogNCSA);
			((AccessLogValve) ncsaLogger).setSuffix(".log"); // ncsaLogge

			if (!modifiedValve) {
				getHost().getPipeline().addValve((Valve) ncsaLogger);
			}
		}

		// for( String address : addresses ) {
		Integer httpPort = configuration.getHttpPort();
		Boolean useNIO = configuration.useNIO();
		Integer httpSecurePort = configuration.getHttpSecurePort();

		if (configuration.isHttpEnabled()) {
			LOG.debug("HttpEnabled");
			Connector[] connectors = getService().findConnectors();
			boolean masterConnectorFound = false; // Flag is set if the same
													// connector has been found
													// through xml config and
													// properties
			if (connectors != null && connectors.length > 0) {
				// Combine the configurations if they do match
				Connector backupConnector = null;

				for (Connector connector : connectors) {
					if ((connector instanceof Connector)
							&& !connector.getSecure()) {

						if ((httpPort == connector.getPort())
								&& "HTTP/1.1".equalsIgnoreCase(connector
										.getProtocol())) {
							if (httpConnector == null) { // CHECKSTYLE:SKIP
								httpConnector = connector;
							}
							configureConnector(configuration, httpPort, useNIO,
									connector);
							masterConnectorFound = true;
							LOG.debug("master connector found, will alter it");
						} else {
							if (backupConnector == null) {
								backupConnector = connector;
								LOG.debug("backup connector found");
							}
						}
					}
				}

				if (httpConnector == null && backupConnector != null) {
					LOG.debug("No master connector found will use backup one");
					httpConnector = backupConnector;
				}
			}

			if (!masterConnectorFound) {
				LOG.debug("No Master connector found create a new one");
				connector = new Connector("HTTP/1.1");
				LOG.debug("Reconfiguring master connector");
				configureConnector(configuration, httpPort, useNIO, connector);
				if (httpConnector == null) {
					httpConnector = connector;
				}
				service.addConnector(connector);
			}
		} else {
			// remove maybe already configured connectors through server.xml,
			// the config-property/config-admin service is master configuration
			LOG.debug("Http is disabled any existing http connector will be removed");
			Connector[] connectors = getService().findConnectors();
			if (connectors != null) {
				for (Connector connector : connectors) {
					if ((connector instanceof Connector)
							&& !connector.getSecure()) {
						LOG.debug("Removing connector {}", connector);
						getService().removeConnector(connector);
					}
				}
			}
		}
		if (configuration.isHttpSecureEnabled()) {
			final String sslPassword = configuration.getSslPassword();
			final String sslKeyPassword = configuration.getSslKeyPassword();

			Connector[] connectors = getService().findConnectors();
			boolean masterSSLConnectorFound = false;
			if (connectors != null && connectors.length > 0) {
				// Combine the configurations if they do match
				Connector backupConnector = null;

				for (Connector connector : connectors) {
					if (connector.getSecure()) {
						Connector sslCon = connector;
						// String[] split = connector.getName().split(":");
						if (httpSecurePort == connector.getPort()) {
							httpSecureConnector = sslCon;
							masterSSLConnectorFound = true;
							configureSSLConnector(configuration, useNIO,
									httpSecurePort, sslCon);
						} else {
							// default behaviour
							if (backupConnector == null) {
								backupConnector = connector;
							}
						}
					}
				}
				if (httpSecureConnector == null && backupConnector != null) {
					httpSecureConnector = backupConnector;
				}
			}

			if (!masterSSLConnectorFound) {
				// no combination of jetty.xml and config-admin/properties
				// needed
				if (sslPassword != null && sslKeyPassword != null) {
					Connector secureConnector = new Connector("HTTPS/1.1");
					configureSSLConnector(configuration, useNIO,
							httpSecurePort, secureConnector);
					// secureConnector.
					if (httpSecureConnector == null) {
						httpSecureConnector = secureConnector;
					}
					getService().addConnector(httpSecureConnector);
				} else {
					LOG.warn("SSL password and SSL keystore password must be set in order to enable SSL.");
					LOG.warn("SSL connector will not be started");
				}
			}
		} else {
			// remove maybe already configured connectors through jetty.xml, the
			// config-property/config-admin service is master configuration
			Connector[] connectors = getService().findConnectors();
			if (connectors != null) {
				for (Connector connector : connectors) {
					if (connector.getSecure()) {
						getService().removeConnector(connector);
					}
				}
			}
		}
		// }
	}

	/**
	 * @param configuration
	 * @param useNIO
	 * @param httpSecurePort
	 * @param secureConnector
	 */
	private void configureSSLConnector(Configuration configuration,
			Boolean useNIO, Integer httpSecurePort, Connector secureConnector) {
		secureConnector.setPort(httpSecurePort);
		secureConnector.setSecure(true);
		secureConnector.setScheme("https");
		secureConnector.setProperty("SSLEnabled", "true");

		secureConnector.setProperty("keystoreFile",
				configuration.getSslKeystore());
		secureConnector.setProperty("keystorePass",
				configuration.getSslKeyPassword());
		secureConnector.setProperty("clientAuth", "false");
		secureConnector.setProperty("sslProtocol", "TLS");

		// configuration.getSslKeystoreType();
		// configuration.getSslPassword();

		// keystoreFile="${user.home}/.keystore" keystorePass="changeit"
		// clientAuth="false" sslProtocol="TLS"

		if (useNIO) {
			secureConnector.setProtocolHandlerClassName(Http11NioProtocol.class
					.getName());
		} else {
			secureConnector.setProtocolHandlerClassName(Http11Protocol.class
					.getName());
		}
	}

	/**
	 * @param configuration
	 * @param httpPort
	 * @param useNIO
	 * @param connector
	 */
	private void configureConnector(Configuration configuration,
			Integer httpPort, Boolean useNIO, Connector connector) {
		LOG.debug("Configuring connector {}", connector);
		connector.setScheme("http");
		connector.setPort(httpPort);
		if (configuration.isHttpSecureEnabled()) {
			connector.setRedirectPort(configuration.getHttpSecurePort());
		}
		if (useNIO) {
			connector.setProtocolHandlerClassName(Http11NioProtocol.class
					.getName());
		} else {
			connector.setProtocolHandlerClassName(Http11Protocol.class
					.getName());
		}
		// connector
		LOG.debug("configuration done: {}", connector);
	}

	private void initBaseDir(Configuration configuration) {
		setBaseDir(configuration.getTemporaryDirectory().getAbsolutePath());
		// TODO do we put the canonical insteadof?
		// super.initBaseDir();
		// TODO do it if it is required
	}

	String getBasedir() {
		return basedir;
	}

	Context findContext(String contextName) {
		return (Context) findContainer(contextName);
	}

	Container findContainer(String contextName) {
		return getHost().findChild(contextName);
	}

	public Context addContext(
			Map<String, String> contextParams,
			Map<String, Object> contextAttributes,
			String contextName,
			HttpContext httpContext,
			AccessControlContext accessControllerContext,
			Map<ServletContainerInitializer, Set<Class<?>>> containerInitializers,
			URL jettyWebXmlURL, List<String> virtualHosts,
			List<String> connectors, String basedir) {
		silence(host, "/" + contextName);
		Context ctx = new HttpServiceContext(getHost(), accessControllerContext);
		ctx.setName(contextName);
		ctx.setPath("/" + contextName);
		ctx.setDocBase(basedir);
		ctx.addLifecycleListener(new FixContextListener());

		// Add Session config
		ctx.setSessionCookieName(configurationSessionCookie);
		// configurationSessionCookieHttpOnly
		ctx.setUseHttpOnly(configurationSessionCookieHttpOnly);
		// configurationSessionTimeout
		ctx.setSessionTimeout(configurationSessionTimeout);
		// configurationWorkerName //TODO: missing

		// new OSGi methods
		((HttpServiceContext) ctx).setHttpContext(httpContext);
		// TODO: what about the AccessControlContext?
		// TODO: the virtual host section below
		// TODO: what about the VirtualHosts?
		// TODO: what about the tomcat-web.xml config?
		// TODO: connectors are needed for virtual host?
		if (containerInitializers != null) {
			for (Entry<ServletContainerInitializer, Set<Class<?>>> entry : containerInitializers
					.entrySet()) {
				ctx.addServletContainerInitializer(entry.getKey(),
						entry.getValue());
			}
		}

		if (host == null) {
			((ContainerBase) getHost()).setStartChildren(false);
			getHost().addChild(ctx);
		} else {
			((ContainerBase) host).setStartChildren(false);
			host.addChild(ctx);
		}

		// Custom Service Valve for checking authentication stuff ...
		ctx.getPipeline().addValve(new ServiceValve(httpContext));
		// Custom OSGi Security
		ctx.getPipeline().addValve(new OSGiAuthenticatorValve(httpContext));

		// try {
		// ctx.stop();
		// } catch (LifecycleException e) {
		// LOG.error("context couldn't be started", e);
		// // e.printStackTrace();
		// }
		return ctx;
	}

	private void silence(Host host, String ctx) {
		String base = "org.apache.catalina.core.ContainerBase.[default].[";
		if (host == null) {
			base += getHost().getName();
		} else {
			base += host.getName();
		}
		base += "].[";
		base += ctx;
		base += "]";
		LOG.warn(base);
	}

}