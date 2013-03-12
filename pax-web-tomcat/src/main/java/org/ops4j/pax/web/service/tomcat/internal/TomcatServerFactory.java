/*
 * Copyright 2012 Romain Gilles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.ops4j.pax.web.service.tomcat.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessControlContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Catalina;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.coyote.http11.Http11Protocol;
import org.apache.tomcat.util.digester.Digester;
import org.ops4j.pax.web.service.spi.Configuration;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @author Romain Gilles
 */
public class TomcatServerFactory implements ServerFactory {
	private static final Logger LOG = LoggerFactory
			.getLogger(TomcatServerFactory.class);

	public TomcatServerFactory() {
	}

	@Override
	public ServerWrapper newServer(Configuration configuration) {
		return TomcatServerWrapper.getInstance(EmbeddedTomcat
				.newEmbeddedTomcat(configuration));
	}
}

class EmbeddedTomcat extends Tomcat {
	private static final Logger LOG = LoggerFactory
			.getLogger(EmbeddedTomcat.class);

	public static final String SERVER_CONFIG_FILE_NAME = "tomcat-server.xml";

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
		result.configure(configuration);
		return result;
	}

	private static class FakeCatalina extends Catalina {
		@Override
		protected Digester createStartDigester() {
			return super.createStartDigester();
		}
	}

	void configure(Configuration configuration) {
		long start = System.nanoTime();
		initBaseDir(configuration);
		Digester digester = new FakeCatalina().createStartDigester();
		// digester.setClassLoader(classLoader); //TODO see if we need to work
		// on class loader
		digester.push(this);
		File configurationFile = new File(configuration.getConfigurationDir(),
				SERVER_CONFIG_FILE_NAME);
		if (configurationFile.exists()) {
			InputStream configurationStream = null;
			try {
				configurationStream = new FileInputStream(configurationFile);
				digester.parse(configurationStream);
				long elapsed = start - System.nanoTime();
				if (LOG.isInfoEnabled()) {
					LOG.info("configuration processed in {} ms",
							(elapsed / 1000000));
				}
			} catch (FileNotFoundException e) {
				throw new ConfigFileNotFoundException(configurationFile, e);
			} catch (IOException e) {
				throw new ConfigFileParsingException(configurationFile, e);
			} catch (SAXException e) {
				throw new ConfigFileParsingException(configurationFile, e);
			} finally {
				// TODO close the file org.eclipse.virgo.util.io.IOUtils
				if (configurationStream != null) {
					try {
						configurationStream.close();
					} catch (IOException e) {
						LOG.debug(
								"cannot close the configuration file '{}' properly",
								configurationFile, e);
					}
				}
			}
		}

		// TODO For the moment we do nothing with the defaults context.xml,
		// web.xml. They are used when you want to deploy web app

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

		// TODO: those configs need to be configured somehow by
		// "systemProperties"?
		configurationDirectory = configuration.getConfigurationDir(); // Fix for
																		// PAXWEB-193

		configurationSessionTimeout = configuration.getSessionTimeout();
		configurationSessionCookie = configuration.getSessionCookie();
		configurationSessionUrl = configuration.getSessionUrl();
		configurationSessionCookieHttpOnly = configuration
				.getSessionCookieHttpOnly();
		configurationWorkerName = configuration.getWorkerName();

		// m_jettyServer.configureContext( attributes,
		// configuration.getSessionTimeout(), configuration
		// .getSessionCookie(), configuration.getSessionUrl(),
		// configuration.getSessionCookieHttpOnly(),
		// configuration.getWorkerName());

		/*
		 * <Host name="localhost" appBase="webapps" unpackWARs="true"
		 * autoDeploy="true"> <Valve
		 * className="org.apache.catalina.valves.AccessLogValve"
		 * directory="logs" prefix="localhost_access_log." suffix=".txt"
		 * pattern="%h %l %u %t &quot;%r&quot; %s %b" /> </Host>
		 */
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
			if (i == 0)
				getEngine().setDefaultHost(address);
			// TODO Configure NCSA RequestLogHandler

			/*
			 * <Valve className="org.apache.catalina.valves.AccessLogValve"
			 * directory="logs" prefix="localhost_access_log." suffix=".txt"
			 * pattern="%h %l %u %t &quot;%r&quot; %s %b" />
			 */
			/*
			 * if (configuration.isLogNCSAFormatEnabled()) { //
			 * m_jettyServer.configureRequestLog
			 * (configuration.getLogNCSAFormat(),
			 * configuration.getLogNCSARetainDays(), //
			 * configuration.isLogNCSAAppend
			 * (),configuration.isLogNCSAExtended(),
			 * configuration.isLogNCSADispatch(), //
			 * configuration.getLogNCSATimeZone
			 * (),configuration.getLogNCSADirectory());
			 * 
			 * String directory = configuration.getLogNCSADirectory();
			 * 
			 * if (directory == null || directory.isEmpty()) directory =
			 * "./logs/"; File file = new File(directory); if (!file.exists()) {
			 * file.mkdirs(); try { file.createNewFile(); } catch (IOException
			 * e) { LOG.error("can't create NCSARequestLog", e); } }
			 * 
			 * if (!directory.endsWith("/")) directory += "/";
			 * 
			 * AccessLog ncsaLogger = new AccessLogValve(); ((AccessLogValve)
			 * ncsaLogger).setPattern(configuration.getLogNCSAFormat());
			 * ((AccessLogValve) ncsaLogger).setDirectory(directory);
			 * ((AccessLogValve)
			 * ncsaLogger).setPrefix(configuration.getLogNCSAFormat());
			 * ((AccessLogValve) ncsaLogger).setSuffix(".txt"); // ncsaLogge
			 * 
			 * // ((Host)host). //TODO: how to attach to host? }
			 */
			if (i > 0)
				getEngine().addChild(host);

		}

		// for( String address : addresses )
		// {
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
						// String[] split = connector.getName().split(":");
						// if (httpPort == Integer.valueOf(split[1]).intValue()
						// && address.equalsIgnoreCase(split[0])) {

						// String connectorHost = connector.getHost();

						if ((httpPort == connector.getPort())
								&& "HTTP/1.1".equalsIgnoreCase(connector
										.getProtocol())) {// && ((connectorHost
															// == null &&
															// connectorHost ==
															// address)
							// || (connectorHost != null &&
							// address.equalsIgnoreCase(connector.getHost()))))
							// {
							// the same connection as configured through
							// property/config-admin already is configured
							// through jetty.xml
							// therefore just use it as the one if not already
							// done so.
							if (httpConnector == null)
								httpConnector = connector;
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
				// connector = new
				// Connector("org.apache.coyote.http11.Http11Protocol");
				LOG.debug("Reconfiguring master connector");
				configureConnector(configuration, httpPort, useNIO, connector);
				// final Connector connector = m_jettyFactory.createConnector(
				// configuration.getHttpConnectorName(), httpPort, address,
				// useNIO);
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
							if (backupConnector == null)
								backupConnector = connector;
						}
					}
				}
				if (httpSecureConnector == null && backupConnector != null)
					httpSecureConnector = backupConnector;
			}

			if (!masterSSLConnectorFound) {
				// no combination of jetty.xml and config-admin/properties
				// needed
				if (sslPassword != null && sslKeyPassword != null) {
					// final Connector secureConnector =
					// m_jettyFactory.createSecureConnector(
					// configuration.getHttpSecureConnectorName(),
					// httpSecurePort, configuration.getSslKeystore(),
					// sslPassword, sslKeyPassword,
					// address,
					// configuration.getSslKeystoreType(),
					// configuration.isClientAuthNeeded(),
					// configuration.isClientAuthWanted()
					// );
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

		if (useNIO)
			secureConnector.setProtocolHandlerClassName(Http11NioProtocol.class
					.getName());
		else
			secureConnector.setProtocolHandlerClassName(Http11Protocol.class
					.getName());
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
		if (configuration.isHttpSecureEnabled())
			connector.setRedirectPort(configuration.getHttpSecurePort());
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
		Context ctx = new HttpServiceContext();
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
			getHost().addChild(ctx);
		} else {
			host.addChild(ctx);
		}
		try {
			ctx.stop();
		} catch (LifecycleException e) {
			LOG.error("context couldn't be started", e);
			// e.printStackTrace();
		}
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