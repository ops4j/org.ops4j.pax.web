/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.war.internal.parser.dom;

import static org.ops4j.util.xml.ElementHelper.getAttribute;
import static org.ops4j.util.xml.ElementHelper.getChild;
import static org.ops4j.util.xml.ElementHelper.getChildren;
import static org.ops4j.util.xml.ElementHelper.getRootElement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.annotation.HandlesTypes;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xbean.finder.BundleAnnotationFinder;
import org.ops4j.pax.web.extender.war.internal.BundleServletScanner;
import org.ops4j.pax.web.extender.war.internal.WebXmlParser;
import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.model.WebAppConstraintMapping;
import org.ops4j.pax.web.extender.war.internal.model.WebAppErrorPage;
import org.ops4j.pax.web.extender.war.internal.model.WebAppFilter;
import org.ops4j.pax.web.extender.war.internal.model.WebAppFilterMapping;
import org.ops4j.pax.web.extender.war.internal.model.WebAppInitParam;
import org.ops4j.pax.web.extender.war.internal.model.WebAppJspServlet;
import org.ops4j.pax.web.extender.war.internal.model.WebAppListener;
import org.ops4j.pax.web.extender.war.internal.model.WebAppLoginConfig;
import org.ops4j.pax.web.extender.war.internal.model.WebAppMimeMapping;
import org.ops4j.pax.web.extender.war.internal.model.WebAppSecurityConstraint;
import org.ops4j.pax.web.extender.war.internal.model.WebAppSecurityRole;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServlet;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServletContainerInitializer;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServletMapping;
import org.ops4j.pax.web.utils.ClassPathUtil;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Web xml parserer implementation using DOM. // TODO parse and use
 * session-config
 * 
 * @author Alin Dreghiciu
 * @since 0.3.0, December 27, 2007
 */
public class DOMWebXmlParser implements WebXmlParser {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(DOMWebXmlParser.class);

	/**
	 * @see WebXmlParser#parse(InputStream)
	 */
	public WebApp parse(final Bundle bundle, final InputStream inputStream) {
		final WebApp webApp = new WebApp(); // changed to final because of inner
											// class.
		try {
			final Element rootElement = getRootElement(inputStream);
			if (rootElement != null) {
				// webApp = new WebApp();
				// web-app attributes
				Integer majorVersion = scanMajorVersion(rootElement);
				Boolean metaDataComplete = Boolean.parseBoolean(getAttribute(
						rootElement, "metadata-complete", "false"));
				webApp.setMetaDataComplete(metaDataComplete);
				LOG.debug("metadata-complete is: {}", metaDataComplete);
				// web-app elements
				webApp.setDisplayName(getTextContent(getChild(rootElement,
						"display-name")));
				parseContextParams(rootElement, webApp);
				parseSessionConfig(rootElement, webApp);
				parseServlets(rootElement, webApp);
				parseFilters(rootElement, webApp);
				parseListeners(rootElement, webApp);
				parseErrorPages(rootElement, webApp);
				parseWelcomeFiles(rootElement, webApp);
				parseMimeMappings(rootElement, webApp);
				parseSecurity(rootElement, webApp);

				servletContainerIntializerScan(bundle, webApp, majorVersion);

				if (!webApp.getMetaDataComplete() && majorVersion != null
						&& majorVersion >= 3) {
					servletAnnotationScan(bundle, webApp);
				}

				tldScan(bundle, webApp);

			} else {
				LOG.warn("The parsed web.xml does not have a root element");
				return null;
			}
		} catch (ParserConfigurationException ignore) {
			LOG.error("Cannot parse web.xml", ignore);
		} catch (IOException ignore) {
			LOG.error("Cannot parse web.xml", ignore);
		} catch (SAXException ignore) {
			LOG.error("Cannot parse web.xml", ignore);
		} catch (ClassNotFoundException ignore) {
			LOG.error("Cannot parse web.xml", ignore);
		} catch (SecurityException ignore) {
			LOG.error("Cannot parse web.xml", ignore);
			// } catch (NoSuchMethodException ignore) {
			// LOG.error("Cannot parse web.xml", ignore);
		} catch (IllegalArgumentException ignore) {
			LOG.error("Cannot parse web.xml", ignore);
		} catch (IllegalAccessException ignore) {
			LOG.error("Cannot parse web.xml", ignore);
			// } catch (InvocationTargetException ignore) {
			// LOG.error("Cannot parse web.xml", ignore);
		} catch (InstantiationException ignore) {
			LOG.error("Cannot parse web.xml", ignore);
		}
		return webApp;
	}

	/**
	 * @param rootElement
	 * @return
	 */
	private Integer scanMajorVersion(final Element rootElement) {
		String version = getAttribute(rootElement, "version");
		Integer majorVersion = null;
		if (version != null && !version.isEmpty()
				&& version.length() > 2) {
			LOG.debug("version found in web.xml - {}",version);
			try {
				majorVersion = Integer
						.parseInt(version.split("\\.")[0]);
			} catch (NumberFormatException nfe) {
				// munch do nothing here stay with null therefore
				// annotation scanning is disabled.
			}
		} else if (version != null && !version.isEmpty()
				&& version.length() > 0) {
			try {
				majorVersion = Integer.parseInt(version);
			} catch (NumberFormatException e) {
				// munch do nothing here stay with null....
			}
		}
		return majorVersion;
	}

	public WebApp parseAnnotatedServlets(final Bundle bundle) {
		return parseAnnotatedServlets(bundle, new WebApp());
	}

	public WebApp parseAnnotatedServlets(final Bundle bundle,
			final WebApp webApp) {
		try {
			servletContainerIntializerScan(bundle, webApp, 3);

			servletAnnotationScan(bundle, webApp);

			tldScan(bundle, webApp);
		} catch (UnsupportedEncodingException ignore) {
			LOG.error("Cannot parse annotated classes", ignore);
		} catch (ClassNotFoundException ignore) {
			LOG.error("Cannot parse annotated classes", ignore);
		} catch (InstantiationException ignore) {
			LOG.error("Cannot parse annotated classes", ignore);
		} catch (IllegalAccessException ignore) {
			LOG.error("Cannot parse annotated classes", ignore);
		} catch (IOException ignore) {
			LOG.error("Cannot parse annotated classes", ignore);
		} catch (ParserConfigurationException ignore) {
			LOG.error("Cannot parse annotated classes", ignore);
		} catch (SAXException ignore) {
			LOG.error("Cannot parse annotated classes", ignore);
		}
		return webApp;
	}

	/**
	 * @param bundle
	 * @param webApp
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 */
	private void tldScan(final Bundle bundle, final WebApp webApp)
			throws ParserConfigurationException, IOException, SAXException {
		// special handling for finding JSF Context listeners wrapped in
		// *.tld files
		// FIXME this is not enough to find TLDs from imported bundles or from
		// the bundle classpath
		// Enumeration<?> tldEntries = bundle.findEntries("/", "*.tld", true);
		// while (tldEntries != null && tldEntries.hasMoreElements()) {
		// URL url = tldEntries.nextElement();

		Set<Bundle> bundlesInClassSpace = ClassPathUtil.getBundlesInClassSpace(
				bundle, new HashSet<Bundle>());

		for (Bundle bundleInClassSpace : bundlesInClassSpace) {
			@SuppressWarnings("rawtypes")
			Enumeration e = bundleInClassSpace.findEntries("/", "*.tld", true);
			if (e == null) {
				continue;
			}
			while (e.hasMoreElements()) {
				URL u = (URL) e.nextElement();
				Element rootTld = getRootElement(u.openStream());
				if (rootTld != null) {
					parseListeners(rootTld, webApp);
				}
			}
		}
	}

	/**
	 * @param bundle
	 * @param webApp
	 * @param majorVersion
	 */
	private void servletAnnotationScan(final Bundle bundle, final WebApp webApp) {

		LOG.debug("metadata-complete is either false or not set");

		LOG.debug("scanning for annotated classes");
		BundleAnnotationFinder baf = BundleServletScanner
				.createBundleAnnotationFinder(bundle);
		Set<Class<?>> webServletClasses = new LinkedHashSet<Class<?>>(
				baf.findAnnotatedClasses(WebServlet.class));
		Set<Class<?>> webFilterClasses = new LinkedHashSet<Class<?>>(
				baf.findAnnotatedClasses(WebFilter.class));
		Set<Class<?>> webListenerClasses = new LinkedHashSet<Class<?>>(
				baf.findAnnotatedClasses(WebListener.class));

		for (Class<?> webServletClass : webServletClasses) {
			LOG.debug("found WebServlet annotation on class: {}", webServletClass);
			WebServletAnnotationScanner annonScanner = new WebServletAnnotationScanner(
					bundle, webServletClass.getCanonicalName());
			annonScanner.scan(webApp);
		}
		for (Class<?> webFilterClass : webFilterClasses) {
			LOG.debug("found WebFilter annotation on class: {}", webFilterClass);
			WebFilterAnnotationScanner filterScanner = new WebFilterAnnotationScanner(
					bundle, webFilterClass.getCanonicalName());
			filterScanner.scan(webApp);
		}
		for (Class<?> webListenerClass : webListenerClasses) {
			LOG.debug("found WebListener annotation on class: {}", webListenerClass);
			addWebListener(webApp, webListenerClass.getSimpleName());
		}

		/*
		 * for (; clazzes != null && clazzes.hasMoreElements();) { URL clazzUrl
		 * = (URL) clazzes.nextElement(); Class<?> clazz; String clazzFile =
		 * clazzUrl.getFile(); LOG.debug("Class file found at :" + clazzFile);
		 * if (clazzFile.startsWith("/WEB-INF/classes")) clazzFile =
		 * clazzFile.replaceFirst( "/WEB-INF/classes", ""); else if
		 * (clazzFile.startsWith("/WEB-INF/lib")) clazzFile =
		 * clazzFile.replaceFirst("/WEB-INF/lib", ""); String clazzName =
		 * clazzFile.replaceAll("/", ".") .replaceAll(".class",
		 * "").replaceFirst(".", ""); try { clazz = bundle.loadClass(clazzName);
		 * } catch (ClassNotFoundException e) { LOG.debug("Class {} not found",
		 * clazzName); continue; } if
		 * (clazz.isAnnotationPresent(WebServlet.class)) {
		 * LOG.debug("found WebServlet annotation on class: " + clazz);
		 * WebServletAnnotationScanner annonScanner = new
		 * WebServletAnnotationScanner( bundle, clazz.getCanonicalName());
		 * annonScanner.scan(webApp); } else if
		 * (clazz.isAnnotationPresent(WebFilter.class)) {
		 * LOG.debug("found WebFilter annotation on class: " + clazz);
		 * WebFilterAnnotationScanner filterScanner = new
		 * WebFilterAnnotationScanner( bundle, clazz.getCanonicalName());
		 * filterScanner.scan(webApp); } else if
		 * (clazz.isAnnotationPresent(WebListener.class)) {
		 * LOG.debug("found WebListener annotation on class: " + clazz);
		 * addWebListener(webApp, clazz.getSimpleName()); } }
		 */
		LOG.debug("class scanning done");
	}

	/**
	 * @param bundle
	 * @param webApp
	 * @param majorVersion 
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 * @throws ParserConfigurationException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	private void servletContainerIntializerScan(final Bundle bundle,
			final WebApp webApp, Integer majorVersion) throws IOException,
			ParserConfigurationException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		LOG.debug("scanning for ServletContainerInitializers");

		// This is a special handling due to the fact that the std. SPI
		// mechanism doesn't work out well in a OSGi world.
		Map<ServletContainerInitializer, Class<ServletContainerInitializer>> serviceLoader = null;

		Enumeration<URL> resources = bundle
				.getResources("/META-INF/services/javax.servlet.ServletContainerInitializer");
		while (resources != null && resources.hasMoreElements()) {
			if (serviceLoader == null) {
				serviceLoader = new HashMap<ServletContainerInitializer, Class<ServletContainerInitializer>>();
			}
			URL url = resources.nextElement();

			InputStream in = null;
			BufferedReader r = null;
			ArrayList<String> names = new ArrayList<String>();
			in = url.openStream();
			r = new BufferedReader(new InputStreamReader(in, "utf-8"));
			int lc = 1;
			while (lc >= 0) {
				String ln = r.readLine();
				if (ln == null) {
					lc = -1;
					continue;
				}
				int ci = ln.indexOf('#');
				if (ci >= 0) {
					ln = ln.substring(0, ci);
				}
				ln = ln.trim();
				int n = ln.length();
				if (n != 0) {
					if ((ln.indexOf(' ') >= 0) || (ln.indexOf('\t') >= 0)) {
						r.close();
						throw new ParserConfigurationException(
								"Illegal configuration-file syntax");
					}
					int cp = ln.codePointAt(0);
					if (!Character.isJavaIdentifierStart(cp)) {
						r.close();
						throw new ParserConfigurationException(
								"Illegal provider-class name: " + ln);
					}
					for (int i = Character.charCount(cp); i < n; i += Character
							.charCount(cp)) {
						cp = ln.codePointAt(i);
						if (!Character.isJavaIdentifierPart(cp) && (cp != '.')) {
							r.close();
							throw new ParserConfigurationException(
									"Illegal provider-class name: " + ln);
						}
					}
					if (!names.contains(ln)) {
						names.add(ln);
					}
				}
				lc += 1;

			}

			for (String name : names) {
				@SuppressWarnings("unchecked")
				Class<ServletContainerInitializer> loadClass = (Class<ServletContainerInitializer>) bundle
						.loadClass(name);
				serviceLoader.put(
						(ServletContainerInitializer) loadClass.newInstance(),
						loadClass);
			}
		}

		if (serviceLoader != null) {
			LOG.debug("ServletContainerInitializers found");
			for (Entry<ServletContainerInitializer, Class<ServletContainerInitializer>> service : serviceLoader
					.entrySet()) {
				LOG.debug("ServletContainerInitializer: {}", service.getValue()
						.getName());
				WebAppServletContainerInitializer webAppServletContainerInitializer = new WebAppServletContainerInitializer();
				webAppServletContainerInitializer
						.setServletContainerInitializer((ServletContainerInitializer) service
								.getKey());
				
				if (!webApp.getMetaDataComplete() && majorVersion != null
						&& majorVersion >= 3) {
					@SuppressWarnings("unchecked")
					Class<HandlesTypes> loadClass = (Class<HandlesTypes>) bundle
					.loadClass("javax.servlet.annotation.HandlesTypes");
					HandlesTypes handlesTypes = loadClass.cast(service.getValue()
							.getAnnotation(loadClass));
					LOG.debug("Found HandlesTypes {}", handlesTypes);
					Class<?>[] classes;
					if (handlesTypes != null) {
						// add annotated classes to service
						classes = handlesTypes.value();
						webAppServletContainerInitializer.setClasses(classes);
					}
				}
				webApp.addServletContainerInitializer(webAppServletContainerInitializer);
				
			}
		}
	}

	/**
	 * Parses security-constraint, login-configuration and security-role out of
	 * web.xml
	 * 
	 * @param rootElement
	 *            web.xml root element
	 * @param webApp
	 *            web app for web.xml
	 */
	private static void parseSecurity(final Element rootElement,
			final WebApp webApp) {
		final Element[] securityConstraint = getChildren(rootElement,
				"security-constraint");

		if (securityConstraint != null && securityConstraint.length > 0) {
			try {
				for (Element scElement : securityConstraint) {
					final WebAppSecurityConstraint webSecurityConstraint = new WebAppSecurityConstraint();

					final Element authConstraintElement = getChild(scElement,
							"auth-constraint");
					if (authConstraintElement != null) {
						webSecurityConstraint.setAuthenticate(true);
						final Element[] roleElemnts = getChildren(
								authConstraintElement, "role-name");
						if (roleElemnts != null && roleElemnts.length > 0) {
							for (Element roleElement : roleElemnts) {
								String roleName = getTextContent(roleElement);
								webSecurityConstraint.addRole(roleName);
							}
						}
					}

					final Element userDataConstraintsElement = getChild(
							scElement, "user-data-constraint");
					if (userDataConstraintsElement != null) {
						String guarantee = getTextContent(
								getChild(userDataConstraintsElement,
										"transport-guarantee")).trim()
								.toUpperCase();
						webSecurityConstraint.setDataConstraint(guarantee);
					}

					final Element[] webResourceElements = getChildren(
							scElement, "web-resource-collection");
					if (webResourceElements != null
							&& webResourceElements.length > 0) {
						for (Element webResourceElement : webResourceElements) {
							WebAppConstraintMapping webConstraintMapping = new WebAppConstraintMapping();

							WebAppSecurityConstraint sc = (WebAppSecurityConstraint) webSecurityConstraint
									.clone();

							String constraintName = getTextContent(getChild(
									webResourceElement, "web-resource-name"));
							webConstraintMapping
									.setConstraintName(constraintName);

							Element[] urlPatternElemnts = getChildren(
									webResourceElement, "url-pattern");
							for (Element urlPattern : urlPatternElemnts) {

								String url = getTextContent(urlPattern);

								Element[] httpMethodElements = getChildren(
										urlPattern, "http-method");
								if (httpMethodElements != null
										&& httpMethodElements.length > 0) {
									for (Element httpMethodElement : httpMethodElements) {
										webConstraintMapping
												.setMapping(getTextContent(httpMethodElement));
										webConstraintMapping.setUrl(url);
										webConstraintMapping
												.setSecurityConstraints(sc);
									}
								} else {
									webConstraintMapping.setUrl(url);
									webConstraintMapping
											.setSecurityConstraints(sc);
								}

								webApp.addConstraintMapping(webConstraintMapping);

							}
						}
					}
				}
			} catch (CloneNotSupportedException e) {
				LOG.warn("", e);
			}
		}

		final Element[] securityRoleElements = getChildren(rootElement,
				"security-role");

		if (securityRoleElements != null && securityRoleElements.length > 0) {
			for (Element securityRoleElement : securityRoleElements) {
				final WebAppSecurityRole webSecurityRole = new WebAppSecurityRole();

				Element[] roleElements = getChildren(securityRoleElement,
						"role-name");
				if (roleElements != null && roleElements.length > 0) {
					for (Element roleElement : roleElements) {
						String roleName = getTextContent(roleElement);
						webSecurityRole.addRoleName(roleName);
					}
				}
				webApp.addSecurityRole(webSecurityRole);
			}
		}

		final Element[] loginConfigElements = getChildren(rootElement,
				"login-config");
		if (loginConfigElements != null && loginConfigElements.length > 0) {
			for (Element loginConfigElement : loginConfigElements) {
				final WebAppLoginConfig webLoginConfig = new WebAppLoginConfig();
				webLoginConfig.setAuthMethod(getTextContent(getChild(
						loginConfigElement, "auth-method")));
				String realmName = getTextContent(getChild(loginConfigElement,
						"realm-name"));
				webLoginConfig.setRealmName(realmName == null ? "default"
						: realmName);
				if ("FORM".equalsIgnoreCase(webLoginConfig.getAuthMethod())) { // FORM
																				// authorization
					Element formLoginConfigElement = getChild(
							loginConfigElement, "form-login-config");
					webLoginConfig.setFormLoginPage(getTextContent(getChild(
							formLoginConfigElement, "form-login-page")));
					webLoginConfig.setFormErrorPage(getTextContent(getChild(
							formLoginConfigElement, "form-error-page")));
				}
				webApp.addLoginConfig(webLoginConfig);
			}
		}
	}

	/**
	 * Parses context params out of web.xml.
	 * 
	 * @param rootElement
	 *            web.xml root element
	 * @param webApp
	 *            web app for web.xml
	 */
	private static void parseContextParams(final Element rootElement,
			final WebApp webApp) {
		final Element[] elements = getChildren(rootElement, "context-param");
		if (elements != null && elements.length > 0) {
			for (Element element : elements) {
				final WebAppInitParam initParam = new WebAppInitParam();
				initParam.setParamName(getTextContent(getChild(element,
						"param-name")));
				initParam.setParamValue(getTextContent(getChild(element,
						"param-value")));
				webApp.addContextParam(initParam);
			}
		}
	}

	/**
	 * Parses session config out of web.xml.
	 * 
	 * @param rootElement
	 *            web.xml root element
	 * @param webApp
	 *            web app for web.xml
	 */
	private static void parseSessionConfig(final Element rootElement,
			final WebApp webApp) {
		final Element scElement = getChild(rootElement, "session-config");
		if (scElement != null) {
			final Element stElement = getChild(scElement, "session-timeout"); // Fix
																				// for
																				// PAXWEB-201
			if (stElement != null) {
				webApp.setSessionTimeout(getTextContent(stElement));
			}
		}
	}

	/**
	 * Parses servlets and servlet mappings out of web.xml.
	 * 
	 * @param rootElement
	 *            web.xml root element
	 * @param webApp
	 *            web app for web.xml
	 */
	private static void parseServlets(final Element rootElement,
			final WebApp webApp) {
		final Element[] elements = getChildren(rootElement, "servlet");
		if (elements != null && elements.length > 0) {
			for (Element element : elements) {
				final WebAppServlet servlet = new WebAppServlet();
				servlet.setServletName(getTextContent(getChild(element,
						"servlet-name")));
				String servletClass = getTextContent(getChild(element,
						"servlet-class"));
				if (servletClass != null) {
					servlet.setServletClassName(servletClass);
					webApp.addServlet(servlet);
				} else {
					String jspFile = getTextContent(getChild(element,
							"jsp-file"));
					if (jspFile != null) {
						WebAppJspServlet jspServlet = new WebAppJspServlet();
						jspServlet.setServletName(getTextContent(getChild(
								element, "servlet-name")));
						jspServlet.setJspPath(jspFile);
						webApp.addServlet(jspServlet);
					}
				}
				servlet.setLoadOnStartup(getTextContent(getChild(element,
						"load-on-startup")));
				servlet.setAsyncSupported(getTextContent(getChild(element,
						"async-supported")));

				final Element[] initParamElements = getChildren(element,
						"init-param");
				if (initParamElements != null && initParamElements.length > 0) {
					for (Element initParamElement : initParamElements) {
						final WebAppInitParam initParam = new WebAppInitParam();
						initParam.setParamName(getTextContent(getChild(
								initParamElement, "param-name")));
						initParam.setParamValue(getTextContent(getChild(
								initParamElement, "param-value")));
						servlet.addInitParam(initParam);
					}
				}
			}
		}
		final Element[] mappingElements = getChildren(rootElement,
				"servlet-mapping");
		if (mappingElements != null && mappingElements.length > 0) {
			for (Element mappingElement : mappingElements) {
				// starting with servlet 2.5 url-patern can be specified more
				// times
				// for the earlier version only one entry will be returned
				final String servletName = getTextContent(getChild(
						mappingElement, "servlet-name"));
				final Element[] urlPatternsElements = getChildren(
						mappingElement, "url-pattern");
				if (urlPatternsElements != null
						&& urlPatternsElements.length > 0) {
					for (Element urlPatternElement : urlPatternsElements) {
						final WebAppServletMapping servletMapping = new WebAppServletMapping();
						servletMapping.setServletName(servletName);
						servletMapping
								.setUrlPattern(getTextContent(urlPatternElement));
						webApp.addServletMapping(servletMapping);
					}
				}
			}
		}
	}

	/**
	 * Parses filters and filter mappings out of web.xml.
	 * 
	 * @param rootElement
	 *            web.xml root element
	 * @param webApp
	 *            web app for web.xml
	 */
	private static void parseFilters(final Element rootElement,
			final WebApp webApp) {
		final Element[] elements = getChildren(rootElement, "filter");
		if (elements != null && elements.length > 0) {
			for (Element element : elements) {
				final WebAppFilter filter = new WebAppFilter();
				filter.setFilterName(getTextContent(getChild(element,
						"filter-name")));
				filter.setFilterClass(getTextContent(getChild(element,
						"filter-class")));
				webApp.addFilter(filter);
				final Element[] initParamElements = getChildren(element,
						"init-param");
				if (initParamElements != null && initParamElements.length > 0) {
					for (Element initParamElement : initParamElements) {
						final WebAppInitParam initParam = new WebAppInitParam();
						initParam.setParamName(getTextContent(getChild(
								initParamElement, "param-name")));
						initParam.setParamValue(getTextContent(getChild(
								initParamElement, "param-value")));
						filter.addInitParam(initParam);
					}
				}
			}
		}
		final Element[] mappingElements = getChildren(rootElement,
				"filter-mapping");
		if (mappingElements != null && mappingElements.length > 0) {
			for (Element mappingElement : mappingElements) {
				// starting with servlet 2.5 url-patern / servlet-names can be
				// specified more times
				// for the earlier version only one entry will be returned
				final String filterName = getTextContent(getChild(
						mappingElement, "filter-name"));
				final Element[] urlPatternsElements = getChildren(
						mappingElement, "url-pattern");
				if (urlPatternsElements != null
						&& urlPatternsElements.length > 0) {
					for (Element urlPatternElement : urlPatternsElements) {
						final WebAppFilterMapping filterMapping = new WebAppFilterMapping();
						filterMapping.setFilterName(filterName);
						filterMapping
								.setUrlPattern(getTextContent(urlPatternElement));
						webApp.addFilterMapping(filterMapping);
					}
				}
				final Element[] servletNamesElements = getChildren(
						mappingElement, "servlet-name");
				if (servletNamesElements != null
						&& servletNamesElements.length > 0) {
					for (Element servletNameElement : servletNamesElements) {
						final WebAppFilterMapping filterMapping = new WebAppFilterMapping();
						filterMapping.setFilterName(filterName);
						filterMapping
								.setServletName(getTextContent(servletNameElement));
						webApp.addFilterMapping(filterMapping);
					}
				}
			}
		}
	}

	/**
	 * Parses listsners out of web.xml.
	 * 
	 * @param rootElement
	 *            web.xml root element
	 * @param webApp
	 *            web app for web.xml
	 */
	private static void parseListeners(final Element rootElement,
			final WebApp webApp) {
		final Element[] elements = getChildren(rootElement, "listener");
		if (elements != null && elements.length > 0) {
			for (Element element : elements) {
				addWebListener(webApp,
						getTextContent(getChild(element, "listener-class")));
			}
		}
	}

	/**
	 * Parses error pages out of web.xml.
	 * 
	 * @param rootElement
	 *            web.xml root element
	 * @param webApp
	 *            web app for web.xml
	 */
	private static void parseErrorPages(final Element rootElement,
			final WebApp webApp) {
		final Element[] elements = getChildren(rootElement, "error-page");
		if (elements != null && elements.length > 0) {
			for (Element element : elements) {
				final WebAppErrorPage errorPage = new WebAppErrorPage();
				errorPage.setErrorCode(getTextContent(getChild(element,
						"error-code")));
				errorPage.setExceptionType(getTextContent(getChild(element,
						"exception-type")));
				errorPage.setLocation(getTextContent(getChild(element,
						"location")));
				webApp.addErrorPage(errorPage);
			}
		}
	}

	/**
	 * Parses welcome files out of web.xml.
	 * 
	 * @param rootElement
	 *            web.xml root element
	 * @param webApp
	 *            web app for web.xml
	 */
	private static void parseWelcomeFiles(final Element rootElement,
			final WebApp webApp) {
		final Element listElement = getChild(rootElement, "welcome-file-list");
		if (listElement != null) {
			final Element[] elements = getChildren(listElement, "welcome-file");
			if (elements != null && elements.length > 0) {
				for (Element element : elements) {
					webApp.addWelcomeFile(getTextContent(element));
				}
			}
		}
	}

	/**
	 * Parses mime mappings out of web.xml.
	 * 
	 * @param rootElement
	 *            web.xml root element
	 * @param webApp
	 *            web app for web.xml
	 */
	private static void parseMimeMappings(final Element rootElement,
			final WebApp webApp) {
		final Element[] elements = getChildren(rootElement, "mime-mapping");
		if (elements != null && elements.length > 0) {
			for (Element element : elements) {
				final WebAppMimeMapping mimeMapping = new WebAppMimeMapping();
				mimeMapping.setExtension(getTextContent(getChild(element,
						"extension")));
				mimeMapping.setMimeType(getTextContent(getChild(element,
						"mime-type")));
				webApp.addMimeMapping(mimeMapping);
			}
		}
	}

	/**
	 * Returns the text content of an element or null if the element is null.
	 * 
	 * @param element
	 *            the som elemet form which the contet should be retrieved
	 * 
	 * @return text content of element
	 */
	private static String getTextContent(final Element element) {
		if (element != null) {
			String content = element.getTextContent();
			if (content != null) {
				content = content.trim();
			}
			return content;
		}
		return null;
	}

	/**
	 * @param webApp
	 * @param clazz
	 */
	private static void addWebListener(final WebApp webApp, String clazz) {
		final WebAppListener listener = new WebAppListener();
		listener.setListenerClass(clazz);
		webApp.addListener(listener);
	}

}
