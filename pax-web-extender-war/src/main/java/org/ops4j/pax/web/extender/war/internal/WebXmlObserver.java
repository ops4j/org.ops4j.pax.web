/*
 * Copyright 2007 Alin Dreghiciu, Achim Nierbeck.
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
package org.ops4j.pax.web.extender.war.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.lang.PreConditionException;
import org.ops4j.pax.swissbox.extender.BundleObserver;
import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.util.ManifestUtil;
import org.ops4j.pax.web.extender.war.internal.util.Path;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Register/unregister web applications once a bundle containing a
 * "WEB-INF/web.xml" gets started or stopped.
 * 
 * @author Alin Dreghiciu
 * @author Achim Nierbeck
 * @author Hiram Chirino <hiram@hiramchirino.com>
 * @author Marc Klinger - mklinger[at]nightlabs[dot]de
 * @since 0.3.0, Decemver 27, 2007
 */
class WebXmlObserver extends WebObserver<URL> {
	/**
	 * Logger.
	 */
	static final Logger LOG = LoggerFactory.getLogger(WebXmlObserver.class);
	/**
	 * web.xml parser to be used.
	 */
	private final WebXmlParser parser;

	/**
	 * Creates a new web.xml observer.
	 * 
	 * @param parser
	 *            parser for web.xml
	 * @param publisher
	 *            web app publisher
	 * @param bundleContext
	 * 
	 * @throws NullArgumentException
	 *             if parser or publisher is null
	 */
	WebXmlObserver(final WebXmlParser parser, final WebAppPublisher publisher,
			final WebEventDispatcher eventDispatcher,
			DefaultWebAppDependencyManager dependencyManager,
			BundleContext bundleContext) {
		super(publisher, eventDispatcher, dependencyManager, bundleContext);
		NullArgumentException.validateNotNull(parser, "Web.xml Parser");

		this.parser = parser;

	}

	/**
	 * Parse the web.xml and publish the corresponding web app. The received
	 * list is expected to contain one URL of an web.xml (only first is used.
	 * The web.xml will be parsed and resulting web application structure will
	 * be registered with the http service.
	 * 
	 * @throws NullArgumentException
	 *             if bundle or list of web xmls is null
	 * @throws PreConditionException
	 *             if the list of web xmls is empty or more then one xml
	 * @see BundleObserver#addingEntries(Bundle,List)
	 */
	public void addingEntries(final Bundle bundle, final List<URL> entries) {
		NullArgumentException.validateNotNull(bundle, "Bundle");
		NullArgumentException.validateNotNull(entries, "List of *.xml's");
		if (bundle.getState() != Bundle.ACTIVE) {
			throw new PreConditionException("Bundle is not in ACTIVE state, ignore it!");
		}

		ClassLoader previous = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(
				getClass().getClassLoader());

		// Context name is also needed for some of the pre-condition checks,
		// therefore it is retrieved here.
		String contextName = ManifestUtil.extractContextName(bundle);
		LOG.info("Using [{}] as web application context name", contextName);

		List<String> virtualHostList = extractVirtualHostList(bundle);
		LOG.info("[{}] virtual hosts defined in bundle header", virtualHostList.size());

		List<String> connectorList = extractConnectorList(bundle);
		LOG.info("[{}] connectors defined in bundle header", connectorList.size());

		// try-catch only to inform framework and listeners of an event.
		try {
			// PreConditionException.validateLesserThan( entries.size(), 3,
			// "Number of xml's" );
			PreConditionException.validateEqualTo("WEB-INF"
					.compareToIgnoreCase(Path.getDirectParent(entries.get(0))),
					0, "Direct parent of web.xml");
		} catch (PreConditionException pce) {
			LOG.error(pce.getMessage(), pce);
			eventDispatcher.webEvent(new WebEvent(WebEvent.FAILED, "/"
					+ contextName, bundle, bundleContext.getBundle(), pce));
			throw pce;
		}

		if (webApps.containsKey(bundle.getBundleId())) {
			LOG.debug("Already found a web application in bundle {}", bundle.getBundleId());
			return;
		}

		URL webXmlURL = null; // = entries.get( 0 );
		URL jettyWebXmlURL = null;

		for (URL url : entries) {
			if (isJettyWebXml(url)) {
				// it's the jetty-web.xml
				jettyWebXmlURL = url;
			} else if (isWebXml(url)) {
				// it's the web.xml
				webXmlURL = url;
			} else {
				// just another one
			}
		}
		if (webXmlURL == null) {
			PreConditionException pce = new PreConditionException(
					"no web.xml configured in web-application");
			LOG.error(pce.getMessage(), pce);
			eventDispatcher.webEvent(new WebEvent(WebEvent.FAILED, "/"
					+ contextName, bundle, bundleContext.getBundle(), pce));
			throw pce;
		}

		LOG.debug("Parsing a web application from [{}]", webXmlURL);

		String rootPath = extractRootPath(bundle);
		LOG.info("Using [{}] as web application root path", rootPath);

		InputStream is = null;
		try {
			is = webXmlURL.openStream();
			final WebApp webApp = parser.parse(bundle, is);
			if (webApp != null) {
				LOG.debug("Parsed web app [{}]", webApp);

				webApp.setWebXmlURL(webXmlURL);
				webApp.setJettyWebXmlURL(jettyWebXmlURL);
				webApp.setVirtualHostList(virtualHostList);
				webApp.setConnectorList(connectorList);
				webApp.setBundle(bundle);
				webApp.setContextName(contextName);
				webApp.setRootPath(rootPath);
				webApp.setDeploymentState(WebApp.UNDEPLOYED_STATE);

				webApps.put(bundle.getBundleId(), webApp);

				if (ManifestUtil.getHeader(bundle, "Pax-ManagedBeans") == null) {
					dependencyManager.addWebApp(webApp);
				}

				// The Webapp-Deploy header controls if the app is deployed on
				// startup.
				if ("true"
						.equals(opt(
								ManifestUtil.getHeader(bundle, "Webapp-Deploy"),
								"true"))) {
					deploy(webApp);
				} else {
					eventDispatcher.webEvent(new WebEvent(WebEvent.UNDEPLOYED,
							"/" + webApp.getContextName(), webApp.getBundle(),
							bundleContext.getBundle()));
				}

			}
		} catch (Exception ignore) { // CHECKSTYLE:SKIP
			LOG.error("Could not parse web.xml", ignore);
			eventDispatcher.webEvent(new WebEvent(WebEvent.FAILED, "/"
					+ contextName, bundle, bundleContext.getBundle(), ignore));
		} finally {
			Thread.currentThread().setContextClassLoader(previous);
			if (is != null) {
				try {
					is.close();
				} catch (IOException ignore) { // CHECKSTYLE:SKIP
					// just ignore
				}
			}
		}
	}

	private boolean isJettyWebXml(URL url) {
		String path = url.getPath();
		path = path.substring(path.lastIndexOf('/') + 1);
		boolean match = path.matches("jetty[0-9]?-web\\.xml");
		if (match) {
			return match;
		}
		match = path.matches("web-jetty\\.xml");
		return match;
	}

	private boolean isWebXml(URL url) {
		String path = url.getPath();
		path = path.substring(path.lastIndexOf('/') + 1);
		return path.matches("web\\.xml");
	}

	/**
	 * Unregisters registered web app once that the bundle that contains the
	 * web.xml gets stopped. The list of web.xml's is expected to contain only
	 * one entry (only first will be used).
	 * 
	 * @throws NullArgumentException
	 *             if bundle or list of web xmls is null
	 * @throws PreConditionException
	 *             if the list of web xmls is empty or more then one xml
	 * @see BundleObserver#removingEntries(Bundle,List)
	 */
	public void removingEntries(final Bundle bundle, final List<URL> entries) {
		WebApp webApp = webApps.remove(bundle.getBundleId());
		if (webApp != null
				&& webApp.getDeploymentState() != WebApp.UNDEPLOYED_STATE) {
			undeploy(webApp);
		}
	}
}
