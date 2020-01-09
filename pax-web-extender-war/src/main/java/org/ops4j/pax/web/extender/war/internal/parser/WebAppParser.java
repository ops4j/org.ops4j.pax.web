/*
 * Copyright 2007 Alin Dreghiciu, Guillaume Nodet.
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
package org.ops4j.pax.web.extender.war.internal.parser;

import org.apache.xbean.finder.BundleAnnotationFinder;
import org.ops4j.pax.web.descriptor.gen.AuthConstraintType;
import org.ops4j.pax.web.descriptor.gen.CookieConfigType;
import org.ops4j.pax.web.descriptor.gen.DescriptionType;
import org.ops4j.pax.web.descriptor.gen.DisplayNameType;
import org.ops4j.pax.web.descriptor.gen.EmptyType;
import org.ops4j.pax.web.descriptor.gen.ErrorPageType;
import org.ops4j.pax.web.descriptor.gen.FilterMappingType;
import org.ops4j.pax.web.descriptor.gen.FilterType;
import org.ops4j.pax.web.descriptor.gen.FormLoginConfigType;
import org.ops4j.pax.web.descriptor.gen.JspConfigType;
import org.ops4j.pax.web.descriptor.gen.JspPropertyGroupType;
import org.ops4j.pax.web.descriptor.gen.ListenerType;
import org.ops4j.pax.web.descriptor.gen.LoginConfigType;
import org.ops4j.pax.web.descriptor.gen.MimeMappingType;
import org.ops4j.pax.web.descriptor.gen.MultipartConfigType;
import org.ops4j.pax.web.descriptor.gen.ParamValueType;
import org.ops4j.pax.web.descriptor.gen.PathType;
import org.ops4j.pax.web.descriptor.gen.RoleNameType;
import org.ops4j.pax.web.descriptor.gen.SecurityConstraintType;
import org.ops4j.pax.web.descriptor.gen.SecurityRoleType;
import org.ops4j.pax.web.descriptor.gen.ServletMappingType;
import org.ops4j.pax.web.descriptor.gen.ServletNameType;
import org.ops4j.pax.web.descriptor.gen.ServletType;
import org.ops4j.pax.web.descriptor.gen.SessionConfigType;
import org.ops4j.pax.web.descriptor.gen.TaglibType;
import org.ops4j.pax.web.descriptor.gen.TrackingModeType;
import org.ops4j.pax.web.descriptor.gen.TrueFalseType;
import org.ops4j.pax.web.descriptor.gen.UrlPatternType;
import org.ops4j.pax.web.descriptor.gen.UserDataConstraintType;
import org.ops4j.pax.web.descriptor.gen.WebAppType;
import org.ops4j.pax.web.descriptor.gen.WebResourceCollectionType;
import org.ops4j.pax.web.descriptor.gen.WelcomeFileListType;
import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.model.WebAppConstraintMapping;
import org.ops4j.pax.web.extender.war.internal.model.WebAppCookieConfig;
import org.ops4j.pax.web.extender.war.internal.model.WebAppErrorPage;
import org.ops4j.pax.web.extender.war.internal.model.WebAppFilter;
import org.ops4j.pax.web.extender.war.internal.model.WebAppFilterMapping;
import org.ops4j.pax.web.extender.war.internal.model.WebAppInitParam;
import org.ops4j.pax.web.extender.war.internal.model.WebAppJspConfig;
import org.ops4j.pax.web.extender.war.internal.model.WebAppJspPropertyGroup;
import org.ops4j.pax.web.extender.war.internal.model.WebAppJspServlet;
import org.ops4j.pax.web.extender.war.internal.model.WebAppListener;
import org.ops4j.pax.web.extender.war.internal.model.WebAppLoginConfig;
import org.ops4j.pax.web.extender.war.internal.model.WebAppMimeMapping;
import org.ops4j.pax.web.extender.war.internal.model.WebAppSecurityConstraint;
import org.ops4j.pax.web.extender.war.internal.model.WebAppSecurityRole;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServlet;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServletContainerInitializer;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServletMapping;
import org.ops4j.pax.web.extender.war.internal.model.WebAppTagLib;
import org.ops4j.pax.web.extender.war.internal.util.ManifestUtil;
import org.ops4j.pax.web.service.spi.model.ErrorPageModel;
import org.ops4j.pax.web.utils.ClassPathUtil;
import org.ops4j.spi.SafeServiceLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.servlet.DispatcherType;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.annotation.HandlesTypes;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.sax.SAXSource;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static java.lang.Boolean.TRUE;
import static org.ops4j.util.xml.ElementHelper.getChild;
import static org.ops4j.util.xml.ElementHelper.getChildren;

/**
 * Web xml parser implementation TODO parse and use session-config
 *
 * @author Alin Dreghiciu
 * @author Guillaume Nodet
 * @since 0.3.0, December 27, 2007
 */
@SuppressWarnings("deprecation")
public class WebAppParser {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(WebAppParser.class);

	private ServiceTracker<PackageAdmin, PackageAdmin> packageAdmin;

	public WebAppParser(ServiceTracker<PackageAdmin, PackageAdmin> packageAdmin) {
		this.packageAdmin = packageAdmin;
	}

	public void parse(final Bundle bundle, WebApp webApp) throws Exception {
		// Find root path
		String rootPath = extractRootPath(bundle);
		if (!rootPath.isEmpty()) {
			rootPath = rootPath + "/";
		}
		// Web app version
		Integer majorVersion = 3;
		// Find web xml
		Enumeration<URL> entries = bundle.findEntries(rootPath + "WEB-INF", "web.xml", false);
		URL webXmlURL = (entries != null && entries.hasMoreElements()) ? entries.nextElement() : null;
		if (webXmlURL != null) {
			WebAppType webAppType = parseWebXml(webXmlURL);
			// web-app attributes
			majorVersion = scanMajorVersion(webAppType);
			if (webAppType != null) {
				if (webAppType.isMetadataComplete() != null) {
					webApp.setMetaDataComplete(webAppType.isMetadataComplete());
				}
				LOG.debug("metadata-complete is: {}", webAppType.isMetadataComplete());
			}
			// web-app elements
			parseApp(webAppType, webApp);
		}
		// Scan annotations
		if (!webApp.getMetaDataComplete() && majorVersion != null && majorVersion >= 3) {
			// Scan servlet container-initializer
			servletContainerInitializerScan(bundle, webApp, majorVersion);
			if (TRUE.equals(canSeeClass(bundle, WebServlet.class))) {
				servletAnnotationScan(bundle, webApp);
			}
		}
		// Scan tlds
		tldScan(bundle, webApp);
		// Look for jetty web xml
		URL jettyWebXmlURL = null;
		Enumeration<URL> enums = bundle.findEntries(rootPath + "WEB-INF", "*web*.xml", false);
		while (enums != null && enums.hasMoreElements()) {
			URL url = enums.nextElement();
			if (isJettyWebXml(url)) {
				if (jettyWebXmlURL == null) {
					jettyWebXmlURL = url;
				} else {
					throw new IllegalArgumentException("Found multiple jetty web xml descriptors. Aborting");
				}
			}
		}

		// Look for attached web-fragments
		List<URL> webFragments = scanWebFragments(bundle, webApp);

		webApp.setWebXmlURL(webXmlURL);
		webApp.setJettyWebXmlURL(jettyWebXmlURL);
		webApp.setVirtualHostList(extractVirtualHostList(bundle));
		webApp.setConnectorList(extractConnectorList(bundle));
		webApp.setWebFragments(webFragments);
		webApp.setRootPath(rootPath);
	}

	private void parseApp(WebAppType webAppType, WebApp webApp) {
		for (JAXBElement<?> jaxbElement : webAppType.getModuleNameOrDescriptionAndDisplayName()) {

			Object value = jaxbElement.getValue();
			if (value instanceof ParamValueType) {
				ParamValueType contextParam = (ParamValueType) value;
				parseContextParams(contextParam, webApp);
			} else if (value instanceof FilterType) {
				FilterType filterType = (FilterType) value;
				parseFilters(filterType, webApp);
			} else if (value instanceof FilterMappingType) {
				FilterMappingType filterMapping = (FilterMappingType) value;
				parseFilterMappings(filterMapping, webApp);
			} else if (value instanceof ListenerType) {
				ListenerType listener = (ListenerType) value;
				parseListeners(listener, webApp);
			} else if (value instanceof ServletType) {
				ServletType servlet = (ServletType) value;
				parseServlets(servlet, webApp);
			} else if (value instanceof ServletMappingType) {
				ServletMappingType servletMapping = (ServletMappingType) value;
				parseServletMappings(servletMapping, webApp);
			} else if (value instanceof SessionConfigType) {
				SessionConfigType sessionConfig = (SessionConfigType) value;
				parseSessionConfig(sessionConfig, webApp);
			} else if (value instanceof MimeMappingType) {
				MimeMappingType mimeMapping = (MimeMappingType) value;
				parseMimeMappings(mimeMapping, webApp);
			} else if (value instanceof WelcomeFileListType) {
				WelcomeFileListType welcomeFileList = (WelcomeFileListType) value;
				if (webApp.getWelcomeFiles().length == 0) {
					parseWelcomeFiles(welcomeFileList, webApp);
				} else {
					LOG.error("duplicate <welcome-file-list>");
				}
			} else if (value instanceof ErrorPageType) {
				ErrorPageType errorPage = (ErrorPageType) value;
				parseErrorPages(errorPage, webApp);
			} else if (value instanceof JspConfigType) {
				//TODO: is missing
				JspConfigType jspConfig = (JspConfigType) value;
				if (webApp.getJspConfigDescriptor() == null) {
					parseJspConfig(jspConfig, webApp);
				} else {
					LOG.error("duplicate <jsp-config>");
				}
			} else if (value instanceof SecurityConstraintType) {
				SecurityConstraintType securityConstraint = (SecurityConstraintType) value;
				parseSecurityConstraint(securityConstraint, webApp);
			} else if (value instanceof LoginConfigType) {
				LoginConfigType loginConfig = (LoginConfigType) value;
				if (webApp.getLoginConfigs().length == 0) {
					parseLoginConfig(loginConfig, webApp);
				} else {
					LOG.error("duplicate <login-config>");
				}
			} else if (value instanceof SecurityRoleType) {
				SecurityRoleType securityRole = (SecurityRoleType) value;
				parseSecurityRole(securityRole, webApp);
			} else if (value instanceof DescriptionType || value instanceof DisplayNameType || value instanceof EmptyType ) {
                //Descripton Type  or Display Name Type contains no valueable information for pax web, so just ignore it
                //and make sure there is no warning about it
			} else {
				LOG.debug("unhandled element [{}] of type [{}]", jaxbElement.getName(), value.getClass().getSimpleName());
			}
		}
	}

	private Integer scanMajorVersion(WebAppType webAppType) {
		// String version = getAttribute(rootElement, "version");
		String version = webAppType.getVersion();
		Integer majorVersion = null;
		if (version != null && !version.isEmpty() && version.length() > 2) {
			LOG.debug("version found in web.xml - {}", version);
			try {
				majorVersion = Integer.parseInt(version.split("\\.")[0]);
			} catch (NumberFormatException nfe) {
				// munch do nothing here stay with null therefore
				// annotation scanning is disabled.
			}
		} else if (version != null && !version.isEmpty() && version.length() > 0) {
			try {
				majorVersion = Integer.parseInt(version);
			} catch (NumberFormatException e) {
				// munch do nothing here stay with null....
			}
		}
		return majorVersion;
	}

	private void tldScan(final Bundle bundle, final WebApp webApp) throws Exception {
		// special handling for finding JSF Context listeners wrapped in
		// *.tld files
		// FIXME this is not enough to find TLDs from imported bundles or from
		// the bundle classpath
		// Enumeration<?> tldEntries = bundle.findEntries("/", "*.tld", true);
		// while (tldEntries != null && tldEntries.hasMoreElements()) {
		// URL url = tldEntries.nextElement();

		Set<Bundle> bundlesInClassSpace = ClassPathUtil.getBundlesInClassSpace(bundle, new HashSet<>());

		List<URL> taglibs = new ArrayList<>();
		List<URL> facesConfigs = new ArrayList<>();

		// do not register TLD-defined listeners - they'll be registered by JasperInitializer
//		for (URL u : ClassPathUtil.findResources(bundlesInClassSpace, "/", "*.tld", true)) {
//			try (InputStream is = u.openStream()) {
//				Element rootTld = getRootElement(is);
//				if (rootTld != null) {
//					parseListeners(rootTld, webApp);
//				}
//			}
//		}

		for (URL u : ClassPathUtil.findResources(bundlesInClassSpace, "/META-INF", "*.taglib.xml", false)) {
			LOG.info("found taglib {}", u.toString());
			taglibs.add(u);
		}

		// TODO generalize name pattern according to JSF spec
		for (URL u : ClassPathUtil.findResources(bundlesInClassSpace, "/META-INF", "faces-config.xml", false)) {
			LOG.info("found faces-config.xml {}", u.toString());
			facesConfigs.add(u);
		}

		if (!taglibs.isEmpty()) {
			StringBuilder builder = new StringBuilder();
			for (URL url : taglibs) {
				builder.append(url);
				builder.append(";");
			}
			String paramValue = builder.toString();
			paramValue = paramValue.substring(0, paramValue.length() - 1);

			// semicolon-separated facelet libs
			// TODO merge with any user-defined values
			WebAppInitParam param = new WebAppInitParam();
			param.setParamName("javax.faces.FACELETS_LIBRARIES");
			param.setParamValue(paramValue);
			webApp.addContextParam(param);
		}

		if (!facesConfigs.isEmpty()) {
			StringBuilder builder = new StringBuilder();
			for (URL url : facesConfigs) {
				builder.append(url);
				builder.append(",");
			}
			String paramValue = builder.toString();
			paramValue = paramValue.substring(0, paramValue.length() - 1);

			// comma-separated config files
			// TODO merge with any user-defined values
			WebAppInitParam param = new WebAppInitParam();
			param.setParamName("javax.faces.CONFIG_FILES");
			param.setParamValue(paramValue);
			webApp.addContextParam(param);
		}
	}

	private List<URL> scanWebFragments(final Bundle bundle, final WebApp webApp) throws Exception {
		Set<Bundle> bundlesInClassSpace = ClassPathUtil.getBundlesInClassSpace(bundle, new HashSet<>());

		List<URL> webFragments = new ArrayList<>();
		for (URL webFragmentURL : ClassPathUtil.findResources(bundlesInClassSpace, "/META-INF", "web-fragment.xml", true)) {
			webFragments.add(webFragmentURL);
			WebAppType webAppType = parseWebXml(webFragmentURL);
			parseApp(webAppType, webApp);
		}
		return webFragments;
	}

	private void servletAnnotationScan(final Bundle bundle, final WebApp webApp) throws Exception {

		LOG.debug("metadata-complete is either false or not set");

		LOG.debug("scanning for annotated classes");
		BundleAnnotationFinder baf = new BundleAnnotationFinder(packageAdmin.getService(), bundle);
		Set<Class<?>> webServletClasses = new LinkedHashSet<>(baf.findAnnotatedClasses(WebServlet.class));
		Set<Class<?>> webFilterClasses = new LinkedHashSet<>(baf.findAnnotatedClasses(WebFilter.class));
		Set<Class<?>> webListenerClasses = new LinkedHashSet<>(baf.findAnnotatedClasses(WebListener.class));

		for (Class<?> webServletClass : webServletClasses) {
			LOG.debug("found WebServlet annotation on class: {}", webServletClass);
			WebServletAnnotationConfigurer annonScanner = new WebServletAnnotationConfigurer(bundle,
					webServletClass.getCanonicalName());
			annonScanner.scan(webApp);
		}
		for (Class<?> webFilterClass : webFilterClasses) {
			LOG.debug("found WebFilter annotation on class: {}", webFilterClass);
			WebFilterAnnotationConfigurer filterScanner = new WebFilterAnnotationConfigurer(bundle,
					webFilterClass.getCanonicalName());
			filterScanner.scan(webApp);
		}
		for (Class<?> webListenerClass : webListenerClasses) {
			LOG.debug("found WebListener annotation on class: {}", webListenerClass);
			addWebListener(webApp, webListenerClass.getCanonicalName());
		}

		LOG.debug("class scanning done");
	}

	private void servletContainerInitializerScan(Bundle bundle, WebApp webApp, Integer majorVersion) throws Exception {
		LOG.debug("scanning for ServletContainerInitializer");

		SafeServiceLoader safeServiceLoader = new SafeServiceLoader(bundle.adapt(BundleWiring.class).getClassLoader());
		List<ServletContainerInitializer> containerInitializers = safeServiceLoader
				.load("javax.servlet.ServletContainerInitializer");

		for (ServletContainerInitializer servletContainerInitializer : containerInitializers) {
			WebAppServletContainerInitializer webAppServletContainerInitializer = new WebAppServletContainerInitializer();
			webAppServletContainerInitializer.setServletContainerInitializer(servletContainerInitializer);

			if (!webApp.getMetaDataComplete() && majorVersion != null && majorVersion >= 3) {
				 Class<?>[] classes = getHandledTypes(servletContainerInitializer, bundle);
				 if (classes != null) {
  					 // add annotated classes to service
					 webAppServletContainerInitializer.setClasses(classes);
				 }
			}
			webApp.addServletContainerInitializer(webAppServletContainerInitializer);
		}
	}
	
	private Class<?>[] getHandledTypes(ServletContainerInitializer servletContainerInitializer, Bundle bundle) {
		try {
			@SuppressWarnings("unchecked")
			Class<HandlesTypes> loadClass = (Class<HandlesTypes>) bundle.loadClass("javax.servlet.annotation.HandlesTypes");
			HandlesTypes handlesTypes = loadClass.cast(servletContainerInitializer.getClass().getAnnotation(
					loadClass));
			LOG.debug("Found HandlesTypes {}", handlesTypes);
			return (handlesTypes != null) ? handlesTypes.value() : null;
		} catch (ClassNotFoundException e) {
			LOG.debug("HandlesTypes annotation not present", e);
			return null;
		}
	}

	private static void parseSecurityRole(SecurityRoleType securityRoleType, WebApp webApp) {
		final WebAppSecurityRole webSecurityRole = new WebAppSecurityRole();

		String roleName = securityRoleType.getRoleName().getValue();
		webSecurityRole.addRoleName(roleName);
		webApp.addSecurityRole(webSecurityRole);
	}

	private static void parseLoginConfig(LoginConfigType loginConfig, WebApp webApp) {
		final WebAppLoginConfig webLoginConfig = new WebAppLoginConfig();
		webLoginConfig.setAuthMethod(loginConfig.getAuthMethod().getValue());
		String realmName = null;
		if (loginConfig.getRealmName() != null) {
			realmName = loginConfig.getRealmName().getValue();
		}
		webLoginConfig.setRealmName(realmName == null ? "default" : realmName);
		if ("FORM".equalsIgnoreCase(webLoginConfig.getAuthMethod())) { // FORM
			// authorization
			FormLoginConfigType formLoginConfigElement = loginConfig.getFormLoginConfig();
			if (formLoginConfigElement != null) {
				webLoginConfig.setFormLoginPage(formLoginConfigElement.getFormLoginPage().getValue());
				webLoginConfig.setFormErrorPage(formLoginConfigElement.getFormErrorPage().getValue());
			} else {
				LOG.warn("<login-config> contains <auth-method> FORM but no <form-login-config>");
			}
		}
		webApp.addLoginConfig(webLoginConfig);
	}

	private static void parseSecurityConstraint(SecurityConstraintType securityConstraint, WebApp webApp) {
		try {
			final WebAppSecurityConstraint webSecurityConstraint = new WebAppSecurityConstraint();

			final AuthConstraintType authConstraintElement = securityConstraint.getAuthConstraint();
			if (authConstraintElement != null) {
				webSecurityConstraint.setAuthenticate(true);
				for (RoleNameType roleElement : authConstraintElement.getRoleName()) {
					webSecurityConstraint.addRole(roleElement.getValue());
				}
			}

			final UserDataConstraintType userDataConstraintsElement = securityConstraint.getUserDataConstraint();
			if (userDataConstraintsElement != null) {
				String guarantee = userDataConstraintsElement.getTransportGuarantee().getValue().toUpperCase();
				webSecurityConstraint.setDataConstraint(guarantee);
			}

			for (WebResourceCollectionType webResourceElement : securityConstraint.getWebResourceCollection()) {

				WebAppSecurityConstraint sc = (WebAppSecurityConstraint) webSecurityConstraint.clone();

				String constraintName = webResourceElement.getWebResourceName().getValue();
				int count = webApp.getConstraintMappings().length;
				for (UrlPatternType urlPatternType : webResourceElement.getUrlPattern()) {
					String url = urlPatternType.getValue();
					List<String> httpMethodElements = webResourceElement.getHttpMethod();
					if (httpMethodElements != null && !httpMethodElements.isEmpty()) {
						for (String httpMethodElement : httpMethodElements) {

							WebAppConstraintMapping webConstraintMapping = new WebAppConstraintMapping();

							webConstraintMapping.setConstraintName(constraintName + "-" + count);
							webConstraintMapping.setMapping(httpMethodElement);
							webConstraintMapping.setUrl(url);
							webConstraintMapping.setSecurityConstraints(sc);

							webApp.addConstraintMapping(webConstraintMapping);
							count++;
						}
					} else {
						WebAppConstraintMapping webConstraintMapping = new WebAppConstraintMapping();
						webConstraintMapping.setConstraintName(constraintName + "-" + count);
						webConstraintMapping.setUrl(url);
						webConstraintMapping.setSecurityConstraints(sc);

						webApp.addConstraintMapping(webConstraintMapping);
						count++;
					}
				}
			}
		} catch (CloneNotSupportedException e) {
			LOG.warn("", e);
		}
	}

	/**
	 * Parses context params out of web.xml.
	 *
	 * @param contextParam contextParam element from web.xml
	 * @param webApp       model for web.xml
	 */
	private static void parseContextParams(final ParamValueType contextParam, final WebApp webApp) {
		final WebAppInitParam initParam = new WebAppInitParam();
		initParam.setParamName(contextParam.getParamName().getValue());
		initParam.setParamValue(contextParam.getParamValue().getValue());
		webApp.addContextParam(initParam);
	}

	/**
	 * Parses session config out of web.xml.
	 *
	 * @param sessionConfigType session-configType element from web.xml
	 * @param webApp            model for web.xml
	 */
	private static void parseSessionConfig(final SessionConfigType sessionConfigType, final WebApp webApp) {
		// Fix for PAXWEB-201
		if (sessionConfigType.getSessionTimeout() != null) {
			webApp.setSessionTimeout(sessionConfigType.getSessionTimeout().getValue().toString());
		}
		if (sessionConfigType.getCookieConfig() != null) {
			CookieConfigType cookieConfig = sessionConfigType.getCookieConfig();
			WebAppCookieConfig sessionCookieConfig = new WebAppCookieConfig();
			if (cookieConfig.getDomain() != null) {
				sessionCookieConfig.setDomain(cookieConfig.getDomain().getValue());
			}
			if (cookieConfig.getHttpOnly() != null) {
				sessionCookieConfig.setHttpOnly(cookieConfig.getHttpOnly().isValue());
			}
			if (cookieConfig.getMaxAge() != null) {
				sessionCookieConfig.setMaxAge(cookieConfig.getMaxAge().getValue().intValue());
			}
			if (cookieConfig.getName() != null) {
				sessionCookieConfig.setName(cookieConfig.getName().getValue());
			}
			if (cookieConfig.getPath() != null) {
				sessionCookieConfig.setPath(cookieConfig.getPath().getValue());
			}
			if (cookieConfig.getSecure() != null) {
				sessionCookieConfig.setSecure(cookieConfig.getSecure().isValue());
			}

			webApp.setSessionCookieConfig(sessionCookieConfig);
		}
		if (sessionConfigType.getTrackingMode() != null) {
			List<TrackingModeType> trackingMode = sessionConfigType.getTrackingMode();
			for (TrackingModeType trackingModeType : trackingMode) {
				String value = trackingModeType.getValue();
				webApp.addSessionTrackingMode(value);
			}
		}
	}

	/**
	 * Parses servlets and servlet mappings out of web.xml.
	 *
	 * @param servletType servletType element from web.xml
	 * @param webApp      model for web.xml
	 */
	private static void parseServlets(final ServletType servletType, final WebApp webApp) {
		final WebAppServlet servlet = new WebAppServlet();
		servlet.setServletName(servletType.getServletName().getValue());
		if (servletType.getServletClass() != null) {
			servlet.setServletClassName(servletType.getServletClass().getValue());
			webApp.addServlet(servlet);
		} else {
			String jspFile = servletType.getJspFile().getValue();
			if (jspFile != null) {
				WebAppJspServlet jspServlet = new WebAppJspServlet();
				jspServlet.setServletName(servletType.getServletName().getValue());
				jspServlet.setJspPath(jspFile);
				webApp.addServlet(jspServlet);
			}
		}
		servlet.setLoadOnStartup(servletType.getLoadOnStartup());
		if (servletType.getAsyncSupported() != null) {
			servlet.setAsyncSupported(servletType.getAsyncSupported().isValue());
		}

		MultipartConfigType multipartConfig = servletType.getMultipartConfig();
		if (multipartConfig != null) {
			String location;
			if (multipartConfig.getLocation() == null) {
				location = null;
			} else {
				location = multipartConfig.getLocation().getValue();
			}
			long maxFileSize;
			if (multipartConfig.getMaxFileSize() == null) {
				maxFileSize = -1L;
			} else {
				maxFileSize = multipartConfig.getMaxFileSize();
			}
			long maxRequestSize;
			if (multipartConfig.getMaxRequestSize() == null) {
				maxRequestSize = -1L;
			} else {
				maxRequestSize = multipartConfig.getMaxRequestSize();
			}
			int fileSizeThreshold;
			if (multipartConfig.getFileSizeThreshold() == null) {
				fileSizeThreshold = 0;
			} else {
				fileSizeThreshold = multipartConfig.getFileSizeThreshold().intValue();
			}
			MultipartConfigElement multipartConfigElement = new MultipartConfigElement(location, maxFileSize, maxRequestSize,
					fileSizeThreshold);
			servlet.setMultipartConfig(multipartConfigElement);
		}

		List<ParamValueType> servletInitParams = servletType.getInitParam();
		for (ParamValueType initParamElement : servletInitParams) {
			final WebAppInitParam initParam = new WebAppInitParam();
			initParam.setParamName(initParamElement.getParamName().getValue());
			initParam.setParamValue(initParamElement.getParamValue().getValue());
			servlet.addInitParam(initParam);
		}

	}

	private static void parseServletMappings(ServletMappingType servletMappingType, WebApp webApp) {
		// starting with servlet 2.5 url-pattern can be specified more times
		// for the earlier version only one entry will be returned
		final String servletName = servletMappingType.getServletName().getValue();
		List<UrlPatternType> urlPattern = servletMappingType.getUrlPattern();
		for (UrlPatternType urlPatternElement : urlPattern) {
			final WebAppServletMapping servletMapping = new WebAppServletMapping();
			servletMapping.setServletName(servletName);
			servletMapping.setUrlPattern(urlPatternElement.getValue());
			webApp.addServletMapping(servletMapping);
		}
	}

	/**
	 * Parses filters and filter mappings out of web.xml.
	 *
	 * @param filterType filterType element from web.xml
	 * @param webApp     model for web.xml
	 */
	private static void parseFilters(final FilterType filterType, final WebApp webApp) {
		final WebAppFilter filter = new WebAppFilter();
		if (filterType.getFilterName() != null) {
			filter.setFilterName(filterType.getFilterName().getValue());
		}
		if (filterType.getFilterClass() != null) {
			filter.setFilterClass(filterType.getFilterClass().getValue());
		}

		if (filterType.getAsyncSupported() != null) {
			filter.setAsyncSupported(filterType.getAsyncSupported().isValue());
		}

		webApp.addFilter(filter);
		List<ParamValueType> initParams = filterType.getInitParam();
		if (initParams != null && initParams.size() > 0) {
			for (ParamValueType initParamElement : initParams) {
				final WebAppInitParam initParam = new WebAppInitParam();
				initParam.setParamName(initParamElement.getParamName().getValue());
				initParam.setParamValue(initParamElement.getParamValue().getValue());
				filter.addInitParam(initParam);
			}
		}

		List<DescriptionType> description = filterType.getDescription();
		for (DescriptionType descriptionType : description) {
			filter.addDispatcherType(DispatcherType.valueOf(descriptionType.getValue()));
		}
	}

	private static void parseFilterMappings(FilterMappingType filterMapping, final WebApp webApp) {
		// starting with servlet 2.5 url-pattern / servlet-names can be
		// specified more times
		// for the earlier version only one entry will be returned
		final String filterName = filterMapping.getFilterName().getValue();
		List<Object> urlPatternOrServletName = filterMapping.getUrlPatternOrServletName();
		for (Object object : urlPatternOrServletName) {
			if (object instanceof UrlPatternType) {
				UrlPatternType urlPatternType = (UrlPatternType) object;
				final WebAppFilterMapping webAppFilterMapping = new WebAppFilterMapping();
				webAppFilterMapping.setFilterName(filterName);
				webAppFilterMapping.setUrlPattern(urlPatternType.getValue());
				webApp.addFilterMapping(webAppFilterMapping);
			} else if (object instanceof ServletNameType) {
				ServletNameType servletNameType = (ServletNameType) object;
				final WebAppFilterMapping webAppFilterMapping = new WebAppFilterMapping();
				webAppFilterMapping.setFilterName(filterName);
				webAppFilterMapping.setServletName(servletNameType.getValue());
				webApp.addFilterMapping(webAppFilterMapping);
			}
		}
		List<org.ops4j.pax.web.descriptor.gen.DispatcherType> dispatcher = filterMapping.getDispatcher();
		for (org.ops4j.pax.web.descriptor.gen.DispatcherType dispatcherType : dispatcher) {
			final WebAppFilterMapping webAppFilterMapping = new WebAppFilterMapping();
			webAppFilterMapping.setFilterName(filterName);
			DispatcherType displatcher = DispatcherType.valueOf(dispatcherType.getValue());
			EnumSet<DispatcherType> dispatcherSet = EnumSet.noneOf(DispatcherType.class);
			dispatcherSet.add(displatcher);
			webAppFilterMapping.setDispatcherTypes(dispatcherSet);
			webApp.addFilterMapping(webAppFilterMapping);
		}
	}

	/**
	 * Parses listeners out of web.xml.
	 *
	 * @param listenerType listenerType element from web.xml
	 * @param webApp       model for web.xml
	 */
	private static void parseListeners(final ListenerType listenerType, final WebApp webApp) {
		addWebListener(webApp, listenerType.getListenerClass().getValue());
	}

	/**
	 * Parses listeners out of web.xml.
	 *
	 * @param rootElement web.xml root element
	 * @param webApp      web app for web.xml
	 */
	private static void parseListeners(final Element rootElement,
									   final WebApp webApp) {
		final Element[] elements = getChildren(rootElement, "listener");
		Arrays.stream(elements).forEach(element ->
				addWebListener(webApp, getTextContent(getChild(element, "listener-class"))));
	}

	/**
	 * Parses error pages out of web.xml.
	 *
	 * @param errorPageType errorPageType element from web.xml
	 * @param webApp        model for web.xml
	 */
	private static void parseErrorPages(final ErrorPageType errorPageType, final WebApp webApp) {
		final WebAppErrorPage errorPage = new WebAppErrorPage();
		if (errorPageType.getErrorCode() != null) {
			errorPage.setErrorCode(errorPageType.getErrorCode().getValue().toString());
		}
		if (errorPageType.getExceptionType() != null) {
			errorPage.setExceptionType(errorPageType.getExceptionType().getValue());
		}
		if (errorPageType.getLocation() != null) {
			errorPage.setLocation(errorPageType.getLocation().getValue());
		}
		if (errorPage.getErrorCode() == null && errorPage.getExceptionType() == null) {
			errorPage.setExceptionType(ErrorPageModel.ERROR_PAGE);
		}
		webApp.addErrorPage(errorPage);
	}

	/**
	 * Parses welcome files out of web.xml.
	 *
	 * @param welcomeFileList welcomeFileList element from web.xml
	 * @param webApp     	  model for web.xml
	 */
	private static void parseWelcomeFiles(final WelcomeFileListType welcomeFileList, final WebApp webApp) {
		if (welcomeFileList != null && welcomeFileList.getWelcomeFile() != null
				&& !welcomeFileList.getWelcomeFile().isEmpty()) {
			welcomeFileList.getWelcomeFile().forEach(webApp::addWelcomeFile);
		}
	}

	/**
	 * Parses mime mappings out of web.xml.
	 *
	 * @param mimeMappingType mimeMappingType element from web.xml
	 * @param webApp     	  model for web.xml
	 */
	private static void parseMimeMappings(final MimeMappingType mimeMappingType, final WebApp webApp) {
		final WebAppMimeMapping mimeMapping = new WebAppMimeMapping();
		mimeMapping.setExtension(mimeMappingType.getExtension().getValue());
		mimeMapping.setMimeType(mimeMappingType.getMimeType().getValue());
		webApp.addMimeMapping(mimeMapping);
	}

	private void parseJspConfig(JspConfigType jspConfig, WebApp webApp) {
		List<JspPropertyGroupType> jspPropertyGroup = jspConfig.getJspPropertyGroup();
		List<TaglibType> taglib = jspConfig.getTaglib();
		WebAppJspConfig webAppJspConfig = new WebAppJspConfig();

		for (JspPropertyGroupType jspPropertyGroupType : jspPropertyGroup) {
			WebAppJspPropertyGroup webAppJspGroup = new WebAppJspPropertyGroup();

			TrueFalseType elIgnored = jspPropertyGroupType.getElIgnored();
			TrueFalseType scriptingInvalid = jspPropertyGroupType.getScriptingInvalid();
			TrueFalseType isXml = jspPropertyGroupType.getIsXml();
			for (DisplayNameType displayNameType : jspPropertyGroupType.getDisplayName()) {
				webAppJspGroup.addDisplayName(displayNameType.getValue());
			}

			for (UrlPatternType urlPatternType : jspPropertyGroupType.getUrlPattern()) {
				webAppJspGroup.addUrlPattern(urlPatternType.getValue());
			}

			for (PathType includeCoda : jspPropertyGroupType.getIncludeCoda()) {
				webAppJspGroup.addIncludeCode(includeCoda.getValue());
			}

			for (PathType includePrelude : jspPropertyGroupType.getIncludePrelude()) {
				webAppJspGroup.addIncludePrelude(includePrelude.getValue());
			}

			if (elIgnored != null) {
				webAppJspGroup.addElIgnored(elIgnored.isValue());
			}
			if (scriptingInvalid != null) {
				webAppJspGroup.addScrptingInvalid(scriptingInvalid.isValue());
			}
			if (isXml != null) {
				webAppJspGroup.addIsXml(isXml.isValue());
			}

			webAppJspConfig.addJspPropertyGroup(webAppJspGroup);
		}

		for (TaglibType taglibType : taglib) {
			WebAppTagLib webAppTagLib = new WebAppTagLib();
			String tagLibLocation = taglibType.getTaglibLocation().getValue();
			String tagLibUri = taglibType.getTaglibUri().getValue();
			webAppTagLib.addTagLibLocation(tagLibLocation);
			webAppTagLib.addTagLibUri(tagLibUri);
			webAppJspConfig.addTagLibConfig(webAppTagLib);
		}

		webApp.setJspConfigDescriptor(webAppJspConfig);
	}

	/**
	 * Returns the text content of an element or null if the element is null.
	 *
	 * @param element the same element form which the context should be retrieved
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

	private static void addWebListener(final WebApp webApp, String clazz) {
		final WebAppListener listener = new WebAppListener();
		listener.setListenerClass(clazz);
		webApp.addListener(listener);
	}

	private static String extractRootPath(final Bundle bundle) {
		String rootPath = ManifestUtil.getHeader(bundle, "Webapp-Root");
		if (rootPath == null) {
			rootPath = "";
		}
		rootPath = stripPrefix(rootPath, "/");
		rootPath = stripSuffix(rootPath, "/");
		rootPath = rootPath.trim();
		return rootPath;
	}

	private static String stripPrefix(String value, String prefix) {
		if (value.startsWith(prefix)) {
			return value.substring(prefix.length());
		}
		return value;
	}

	private static String stripSuffix(String value, String suffix) {
		if (value.endsWith(suffix)) {
			return value.substring(0, value.length() - suffix.length());
		}
		return value;
	}

	private static List<String> extractVirtualHostList(final Bundle bundle) {
		List<String> virtualHostList = new LinkedList<>();
		String virtualHostListAsString = ManifestUtil.getHeader(bundle, "Web-VirtualHosts");
		if ((virtualHostListAsString != null) && (virtualHostListAsString.length() > 0)) {
			String[] virtualHostArray = virtualHostListAsString.split(",");
			for (String virtualHost : virtualHostArray) {
				virtualHostList.add(virtualHost.trim());
			}
		}
		return virtualHostList;
	}

	private static List<String> extractConnectorList(final Bundle bundle) {
		List<String> connectorList = new LinkedList<>();
		String connectorListAsString = ManifestUtil.getHeader(bundle, "Web-Connectors");
		if ((connectorListAsString != null) && (connectorListAsString.length() > 0)) {
			String[] virtualHostArray = connectorListAsString.split(",");
			for (String virtualHost : virtualHostArray) {
				connectorList.add(virtualHost.trim());
			}
		}
		return connectorList;
	}

	public static Boolean canSeeClass(Bundle bundle, Class<?> clazz) {
		try {
			return bundle.loadClass(clazz.getName()) == clazz;
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	private static boolean isJettyWebXml(URL url) {
		String path = url.getPath();
		path = path.substring(path.lastIndexOf('/') + 1);
		boolean match = path.matches("jetty[0-9]?-web\\.xml");
		if (match) {
			return true;
		}
		match = path.matches("web-jetty\\.xml");
		return match;
	}

	protected WebAppType parseWebXml(URL url) {
		try {
			XMLReader reader = XMLReaderFactory.createXMLReader();

			// Use filter to override the namespace in the document.
			// On JDK 7, JAXB fails to parse the document if the namespace does
			// not match
			// the one indicated by the generated JAXB model classes.
			// For some reason, the JAXB version in JDK 8 is more lenient and
			// does
			// not require this filter.
			NamespaceFilter inFilter = new NamespaceFilter("http://xmlns.jcp.org/xml/ns/javaee");
			inFilter.setParent(reader);

			JAXBContext context = JAXBContext.newInstance(WebAppType.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			SAXSource source = new SAXSource(inFilter, new InputSource(url.openStream()));

			return unmarshaller.unmarshal(source, WebAppType.class).getValue();
		} catch (JAXBException | IOException | SAXException exc) {
			LOG.error("error parsing web.xml", exc);
		}
		return null;
	}
}
