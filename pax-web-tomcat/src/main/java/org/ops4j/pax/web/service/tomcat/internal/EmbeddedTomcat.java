/*
 * Copyright 2020 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.tomcat.internal;

import java.io.File;

import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Catalina;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.digester.Digester;
import org.ops4j.pax.web.annotations.Review;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbeddedTomcat extends Tomcat {
	public static final String SERVER_CONFIG_FILE_NAME = "tomcat-server.xml";

	@Review("These should be read from web.xml taken from org.apache.tomcat:tomcat:VERSION:zip")
	private static final String[] DEFAULT_MIME_MAPPINGS = {"abs", "audio/x-mpeg"};


	private static final Logger LOG = LoggerFactory
			.getLogger(EmbeddedTomcat.class);

	private Integer configurationSessionTimeout;

	private String configurationSessionCookie;

	private Integer configurationSessionCookieMaxAge;

	private Boolean configurationSessionCookieHttpOnly;

	private File configurationDir;

	static EmbeddedTomcat newEmbeddedTomcat(Configuration configuration) {
		EmbeddedTomcat result = new EmbeddedTomcat();
		result.configure(configuration);
		return result;
	}

	public void setServer(Server server) {
	    this.server = server;
		Service[] findServices = server.findServices();
		if (findServices != null && findServices.length > 0 && findServices[0].getContainer() == null) {
			getEngine().setName("Catalina");
		}
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
		Digester digester = new FakeCatalina().createStartDigester();
		digester.push(this);

//		URL tomcatResource = configuration.server().getConfigurationURL();
//		if (tomcatResource == null) {
//			tomcatResource = getClass().getResource("/tomcat-server.xml");
//		}
//
//		configurationDir = configuration.server().getConfigurationDir();
//		File configurationFile = new File(configurationDir,
//				SERVER_CONFIG_FILE_NAME);
//		if (configurationFile.exists()) {
//			try {
//				tomcatResource = configurationFile.toURI().toURL();
//			} catch (MalformedURLException e) {
//				LOG.error("Exception while starting Tomcat:", e);
//				throw new RuntimeException("Exception while starting Tomcat", e);
//			}
//		}
//		if (tomcatResource != null) {
//			ClassLoader loader = Thread.currentThread().getContextClassLoader();
//			try {
//				Thread.currentThread().setContextClassLoader(
//						getClass().getClassLoader());
//				LOG.debug("Configure using resource " + tomcatResource);
//
//				digester.parse(tomcatResource.openStream());
//				long elapsed = start - System.nanoTime();
//				if (LOG.isInfoEnabled()) {
//					LOG.info("configuration processed in {} ms",
//							(elapsed / 1000000));
//				}
//			} catch (IOException | SAXException e) {
//				LOG.error("Exception while starting Tomcat:", e);
//				throw new RuntimeException("Exception while starting Tomcat", e);
//			} finally {
//				Thread.currentThread().setContextClassLoader(loader);
//			}
//		} else {
//		    getServer().setCatalina(new FakeCatalina());
//		    getService().setName("Catalina");
//		    getEngine().setName("Catalina");
//		}

		mergeConfiguration(configuration);

//        initBaseDir(configuration);
	}

	@Review("A bit different than Jetty - here \"context\" is not configured, but this method is called during configure/start")
	private void mergeConfiguration(Configuration configuration) {
		LOG.debug("Start merging configuration");
		Connector httpConnector = null;
		Connector httpSecureConnector = null;
        String[] addresses = configuration.server().getListeningAddresses();
        if (addresses == null || addresses.length == 0) {
            addresses = new String[]{null};
        }

		// Fix for PAXWEB-193
		configurationSessionTimeout = configuration.session().getSessionTimeout();
		configurationSessionCookie = configuration.session().getSessionCookie();
		configurationSessionCookieMaxAge = configuration.session().getSessionCookieMaxAge();
		configurationSessionCookieHttpOnly = configuration
				.session().getSessionCookieHttpOnly();
	}

//	private void initBaseDir(Configuration configuration) {
//		if (System.getProperty(Globals.CATALINA_HOME_PROP) == null) {
//			setBaseDir(configuration.server().getTemporaryDirectory().getAbsolutePath());
//		}
//		initBaseDir();
//	}

	private boolean isJspAvailable() {
//		try {
//			return (org.ops4j.pax.web.jsp.JspServletWrapper.class != null);
//		} catch (NoClassDefFoundError ignore) {
			return false;
//		}
	}

	@Override
	public Host getHost() {
		Engine engine = getEngine();
		if (engine.findChildren().length > 0) {
			return (Host) engine.findChildren()[0];
		}
		Host host = new StandardHost();
		/*
		 * This is almost a copy of super.getHost(), but if (and only if) a new host is created,
		 * we need to set a new Basic Valve, that changes the dispatch logic
		 */
		Valve contextSelect = new ContextSelectionHostValve(host.getPipeline().getBasic(), getService().getMapper());
		host.getPipeline().setBasic(contextSelect);
		host.setName(hostname);
		getEngine().addChild(host);
		return host;
	}
}