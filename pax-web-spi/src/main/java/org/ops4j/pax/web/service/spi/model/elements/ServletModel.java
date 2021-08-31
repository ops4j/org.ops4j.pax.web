/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.spi.model.elements;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;

import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.spi.config.JspConfiguration;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.events.ServletEventData;
import org.ops4j.pax.web.service.spi.util.Path;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWebContainerView;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * Set of parameters describing everything that's required to register a {@link Servlet}.
 */
public class ServletModel extends ElementModel<Servlet, ServletEventData> {

	/** Alias as defined by old {@link org.osgi.service.http.HttpService} registration methods */
	private final String alias;
	private boolean aliasCopiedToPatterns = false;

	/**
	 * <p>URL patterns as specified by:<ul>
	 *     <li>Pax Web specific extensions to {@link org.osgi.service.http.HttpService}</li>
	 *     <li>Whiteboard Service specification</li>
	 *     <li>Servlet API specification</li>
	 * </ul></p>
	 *
	 * <p>Alias is always directly used as one of (possibly the only) <em>exact pattern</em>.</p>
	 */
	private String[] urlPatterns;

	/** Servlet name that defaults to FQCN of the {@link Servlet}. {@code <servlet>/<servlet-name>} */
	private String name;

	/**
	 * Init parameters of the servlet as specified by {@link ServletConfig#getInitParameterNames()} and
	 * {@code <servlet>/<init-param>} elements in {@code web.xml}.
	 */
	private Map<String, String> initParams;

	/** {@code <servlet>/<load-on-startup>} */
	private Integer loadOnStartup;

	/** {@code <servlet>/<async-supported>} */
	private Boolean asyncSupported;

	/** {@code <servlet>/<multipart-config>} */
	private MultipartConfigElement multipartConfigElement;

	/**
	 * Both Http Service and Whiteboard service allows registration of servlets using existing instance.
	 */
	private Servlet servlet;

	/**
	 * Actual class of the servlet, to be instantiated by servlet container itself. {@code <servlet>/<servlet-class>}.
	 * This can only be set when registering Pax Web specific
	 * {@link org.ops4j.pax.web.service.whiteboard.ServletMapping} "direct Whiteboard" service.
	 */
	private final Class<? extends Servlet> servletClass;

	/**
	 * Flag that marks given {@link ServletModel} as "resource servlet" with slightly different processing.
	 */
	private boolean resourceServlet = false;

	/** Flag that marks given {@link ServletModel} as JSP servlet */
	private boolean jspServlet = false;

	/**
	 * If a servlet is based on JSP file, this is the location passed by Jasper servlet to
	 * {@link javax.servlet.ServletContext#getResource(String)}
	 */
	private String jspFile;

	/**
	 * For resource servlets, we have to specify <em>base path</em> which should be the "prefix" to prepend when
	 * accessing resources through {@link org.osgi.service.http.context.ServletContextHelper} (in Whiteboard). But
	 * Pax Web will happily allow to serve properties (without custom context) when basePath is absolute URI.
	 */
	private String basePath = null;

	/**
	 * If the "base" configured by user is valid absolute, non-opaque {@link URL} it can be used as a "base" not
	 * related to any bundle.
	 */
	private URL baseFileUrl = null;

	/**
	 * If we don't know yet if the path provided from Whiteboard is a {@link #basePath} or {@link #baseFileUrl},
	 * we can specify a <em>raw path</em> in order to check it later.
	 */
	private String rawPath = null;

	/**
	 * <p>A servlet may have error declarations specified, then it is used as the error servlet (and if it has own
	 * mappings, it can be used as normal servlet as well).</p>
	 *
	 * <p>If Whiteboard-registered servlet doesn't have URI mappings specified (as OSGi CMPN Whiteboard specification
	 * permits), single exact URI pattern will be added.</p>
	 */
	private String[] errorDeclarations;

	/**
	 * If {@link ServletModel} carries error page information, we'll keep it separately as embedded
	 * {@link ErrorPageModel} - but only if it's valid.
	 */
	private ErrorPageModel errorPageModel;

	/**
	 * Flag used for models registered using {@link javax.servlet.ServletContext#addServlet}
	 */
	private boolean dynamic = false;

	/**
	 * Flag indicating that a servlet may be overriden. It's set to {@code true} for example for
	 * {@code default} servlet from <em>default</em> {@code web.xml}.
	 */
	private boolean overridable = false;

	/**
	 * Constructor used for servlet unregistration
	 * @param alias
	 * @param servletName
	 * @param servlet
	 * @param servletClass
	 * @param reference
	 */
	public ServletModel(String alias, String servletName, Servlet servlet,
			Class<? extends Servlet> servletClass, ServiceReference<Servlet> reference) {
		this.alias = alias;
		this.name = servletName;
		this.servlet = servlet;
		this.servletClass = servletClass;
		this.setElementReference(reference);
	}

	public ServletModel(String alias, Servlet servlet, Dictionary<?,?> initParams, Integer loadOnStartup, Boolean asyncSupported) {
		this(alias, null, null, Utils.toMap(initParams),
				loadOnStartup, asyncSupported, null,
				servlet, null, null, null, false, null);
	}

	public ServletModel(String servletName, String[] urlPatterns, Servlet servlet, Dictionary<String, String> initParams,
			Integer loadOnStartup, Boolean asyncSupported, MultipartConfigElement multiPartConfig) {
		this(null, urlPatterns, servletName, Utils.toMap(initParams),
				loadOnStartup, asyncSupported, multiPartConfig,
				servlet, null, null, null, false, null);
	}

	public ServletModel(String[] urlPatterns, Class<? extends Servlet> servletClass, Dictionary<String, String> initParams,
			Integer loadOnStartup, Boolean asyncSupported, MultipartConfigElement multiPartConfig) {
		this(null, urlPatterns, null, Utils.toMap(initParams),
				loadOnStartup, asyncSupported, multiPartConfig,
				null, servletClass, null, null, false, null);
	}

	@SuppressWarnings("deprecation")
	private ServletModel(String alias, String[] urlPatterns, String name, Map<String, String> initParams,
			Integer loadOnStartup, Boolean asyncSupported, MultipartConfigElement multipartConfigElement,
			Servlet servlet, Class<? extends Servlet> servletClass, ServiceReference<Servlet> reference,
			Supplier<? extends Servlet> supplier, boolean resourceServlet,
			Bundle registeringBundle) {
		this.alias = alias;
		this.urlPatterns = Path.normalizePatterns(urlPatterns);
		this.initParams = initParams == null ? new LinkedHashMap<>() : new LinkedHashMap<>(initParams);
		this.loadOnStartup = loadOnStartup;
		this.asyncSupported = asyncSupported;
		this.multipartConfigElement = multipartConfigElement;
		this.servlet = servlet;
		this.servletClass = servletClass;
		setElementReference(reference);
		setElementSupplier(supplier);
		setRegisteringBundle(registeringBundle);

		this.resourceServlet = resourceServlet;

		if (name == null) {
			// legacy method first
			name = this.initParams.get(PaxWebConstants.INIT_PARAM_SERVLET_NAME);
			this.initParams.remove(PaxWebConstants.INIT_PARAM_SERVLET_NAME);
		}
		if (name == null) {
			// Whiteboard Specification 140.4 Registering Servlets
			Class<? extends Servlet> c = getActualClass();
			if (c != null) {
				name = c.getName();
			}
		}
		if (name == null && !resourceServlet) {
			// no idea how to obtain the class, but this should not happen
			name = UUID.randomUUID().toString();
		}
		this.name = name;

		if ((this.urlPatterns == null || this.urlPatterns.length == 0) && this.alias != null) {
			// Http Service specification 102.4 Mapping HTTP Requests to Servlet and Resource Registrations:
			// [...]
			// 6. If there is no match, the Http Service must attempt to match sub-strings of the requested
			//    URI to registered aliases. The sub-strings of the requested URI are selected by removing
			//    the last "/" and everything to the right of the last "/".
			if ("/".equals(this.alias)) {
				if (resourceServlet) {
					// special case for resource servlet. We don't want "/" to change to "/*"
					this.urlPatterns = new String[] { "/" };
				} else {
					this.urlPatterns = new String[] { "/*" };
				}
			} else {
				this.urlPatterns = new String[] { this.alias + "/*" };
			}
			this.aliasCopiedToPatterns = true;
		}
	}

	@Override
	public Boolean performValidation() throws Exception {
		if (isValid != null) {
			return isValid;
		}
		int sources = 0;
		if (!resourceServlet) {
			sources += (servlet != null ? 1 : 0);
			sources += (servletClass != null ? 1 : 0);
			sources += (getElementReference() != null ? 1 : 0);
			sources += (getElementSupplier() != null ? 1 : 0);
			if (sources == 0) {
				throw new IllegalArgumentException("Servlet Model must specify one of: servlet instance, servlet class,"
						+ " servlet supplier or service reference");
			}
			if (sources != 1) {
				throw new IllegalArgumentException("Servlet Model should specify a servlet uniquely as instance, class,"
						+ " supplier or service reference");
			}
		}

		if (this.alias == null && (this.urlPatterns == null || this.urlPatterns.length == 0)) {
			throw new IllegalArgumentException("Neither alias nor URL patterns array is specified");
		}
		if (this.alias != null && this.urlPatterns != null && this.urlPatterns.length > 0 && !aliasCopiedToPatterns) {
			throw new IllegalArgumentException("Can't specify both alias and URL patterns array");
		}

		if (this.alias != null) {
			if (!this.alias.startsWith("/")) {
				throw new IllegalArgumentException("Alias does not start with slash (/)");
			}
			// "/" must be allowed
			if (alias.length() > 1 && alias.endsWith("/")) {
				throw new IllegalArgumentException("Alias should not end with slash (/)");
			}
			if ("".equals(alias.trim())) {
				throw new IllegalArgumentException("Alias should not be empty");
			}
		}

		if (urlPatterns != null) {
			for (String url : urlPatterns) {
				if ("/".equals(url) || "/*".equals(url)) {
					continue;
				}
				if (url.endsWith("/*")) {
					if (url.substring(0, url.length() - 2).contains("*") || !url.startsWith("/")) {
						throw new IllegalArgumentException("URL Pattern \"" + url + "\" is not a valid path pattern");
					}
				}
				if (url.startsWith("*.")) {
					if (url.substring(2).contains("/") || url.substring(2).contains("*")) {
						throw new IllegalArgumentException("URL Pattern \"" + url + "\" is not a valid extension pattern");
					}
				}
			}
		}

		if (resourceServlet) {
			if (rawPath == null && basePath == null && baseFileUrl == null) {
				throw new IllegalArgumentException("Base path or base directory is required for resource servlets");
			}
			sources = 0;
			sources += (rawPath != null ? 1 : 0);
			sources += (basePath != null ? 1 : 0);
			sources += (baseFileUrl != null ? 1 : 0);
			if (sources != 1) {
				throw new IllegalArgumentException("Only one base (resource base or base directory) is allowed for resource servlets");
			}
		}

		if (errorDeclarations != null && errorDeclarations.length > 0) {
			// a tricky way to validate such declaration - embed full ErrorPageModel
			ErrorPageModel epm = new ErrorPageModel(errorDeclarations);
			epm.setRegisteringBundle(getRegisteringBundle());
			String errorPageExact = null;
			String errorPagePrefix = null;
			String errorPageExtension = null;
			for (String url : urlPatterns) {
				// this loop includes alias and validated url patterns
				if (url.startsWith("/") && !url.endsWith("/*")) {
					// the best mapping
					errorPageExact = url;
					break;
				}
				if (errorPagePrefix == null && url.startsWith("/") && url.endsWith("/*")) {
					// not perfect, but it suffices
					if ("/*".equals(url)) {
						errorPagePrefix = generateRandomErrorPage();
					} else {
						errorPagePrefix = url.substring(0, url.length() - 2) + generateRandomErrorPage();
					}
				}
				if (errorPageExtension == null && url.startsWith("*.")) {
					errorPageExtension = "error" + url.substring(2);
				}
			}
			if (errorPageExact != null) {
				epm.setLocation(errorPageExact);
			} else if (errorPagePrefix != null) {
				epm.setLocation(errorPagePrefix);
			} else if (errorPageExtension != null) {
				epm.setLocation(errorPageExtension);
			}
			if (epm.performValidation()) {
				errorPageModel = epm;
			}
		}

		if (jspFile != null) {
			// we can't verify the existence of this resource at this point
			if (!jspFile.startsWith("/")) {
				// should be relative to context base, but should start with "/"
				// see org.apache.catalina.startup.ContextConfig.convertJsp()
				jspFile = "/" + jspFile;
			}
			// see org.apache.jasper.servlet.JspServlet.init() and
			// org.apache.catalina.startup.ContextConfig.convertJsp()
			initParams.put("jspFile", jspFile);
		}

		return Boolean.TRUE;
	}

	private String generateRandomErrorPage() {
		return String.format("/error-%s", UUID.randomUUID());
	}

	/**
	 * Returns {@link URL} if and only if the base is proper {@code file:} based absolute url.
	 * @param base
	 * @return
	 */
	public static URL getFileUrlIfAccessible(String base) {
		try {
			URL baseUrl = new URL(base);
			File f = new File(baseUrl.toURI());
			if (!f.isDirectory()) {
				return null;
			}
			return f.toURI().toURL();
		} catch (MalformedURLException | URISyntaxException e) {
			return null;
		}
	}

	@Override
	public void register(WhiteboardWebContainerView view) {
		if (!this.isResourceServlet()) {
			view.registerServlet(this);
		} else {
			view.registerResources(this);
		}
	}

	@Override
	public void unregister(WhiteboardWebContainerView view) {
		if (!this.isResourceServlet()) {
			view.unregisterServlet(this);
		} else {
			view.unregisterResources(this);
		}
	}

	@Override
	public ServletEventData asEventData() {
		ServletEventData data = new ServletEventData(alias, name, urlPatterns, servlet);
		setCommonEventProperties(data);
		data.setResourceServlet(this.resourceServlet);
		data.setJspServlet(this.jspServlet);
		data.setJspFile(this.jspFile);
		if (resourceServlet) {
			if (rawPath != null) {
				data.setPath(this.rawPath);
			} else if (basePath != null) {
				data.setPath(this.basePath);
			} else if (baseFileUrl != null) {
				data.setPath(this.baseFileUrl.toExternalForm());
			}
		}
		return data;
	}

	@Override
	public int compareTo(ElementModel<Servlet, ServletEventData> o) {
		int superCompare = super.compareTo(o);
		if (superCompare == 0 && o instanceof ServletModel) {
			// this happens in non-Whiteboard scenario
			return this.name.compareTo(((ServletModel)o).name);
		}
		return superCompare;
	}

	public String getAlias() {
		return alias;
	}

	public String[] getUrlPatterns() {
		return urlPatterns;
	}

	public void setUrlPatterns(String[] urlPatterns) {
		this.urlPatterns = urlPatterns;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, String> getInitParams() {
		return initParams;
	}

	public Integer getLoadOnStartup() {
		return loadOnStartup;
	}

	public void setLoadOnStartup(Integer loadOnStartup) {
		this.loadOnStartup = loadOnStartup;
	}

	public Boolean getAsyncSupported() {
		return asyncSupported;
	}

	public void setAsyncSupported(Boolean asyncSupported) {
		this.asyncSupported = asyncSupported;
	}

	public MultipartConfigElement getMultipartConfigElement() {
		return multipartConfigElement;
	}

	public void setMultipartConfigElement(MultipartConfigElement multipartConfigElement) {
		this.multipartConfigElement = multipartConfigElement;
	}

	public Servlet getServlet() {
		return servlet;
	}

	public void setServlet(Servlet servlet) {
		this.servlet = servlet;
	}

	public Class<? extends Servlet> getServletClass() {
		return servletClass;
	}

	/**
	 * Returns a {@link Class} of the servlet whether it is registered as instance, class or reference.
	 * @return
	 */
	public Class<? extends Servlet> getActualClass() {
		if (this.servletClass != null) {
			return this.servletClass;
		} else if (this.servlet != null) {
			return this.servlet.getClass();
		} else if (this.getElementSupplier() != null) {
			Servlet s = getElementSupplier().get();
			return s.getClass();
		} else if (getElementReference() != null) {
			// I don't want to dereference here - especially if the reference was "prototype" scoped
			// sane default, accepted by Undertow - especially if it has instance factory
			return Servlet.class;
		}

		return null; // even if it can't happen
	}

	public boolean isResourceServlet() {
		return resourceServlet;
	}

	public boolean isJspServlet() {
		return jspServlet;
	}

	public void setJspServlet(boolean jspServlet) {
		this.jspServlet = jspServlet;
	}

	public void setJspFile(String jspFile) {
		this.jspFile = jspFile;
	}

	private void setOverridable(boolean overridable) {
		this.overridable = overridable;
	}

	public boolean isOverridable() {
		return overridable;
	}

	public String getBasePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public void setBaseFileUrl(URL baseFileUrl) {
		this.baseFileUrl = baseFileUrl;
	}

	public URL getBaseFileUrl() {
		return baseFileUrl;
	}

	public String getRawPath() {
		return rawPath;
	}

	public void setRawPath(String rawPath) {
		this.rawPath = rawPath;
	}

	private void setErrorDeclarations(String[] errorDeclarations) {
		// this method is private on purpose - to be invoked only via builder.
		this.errorDeclarations = errorDeclarations;
		if (errorDeclarations != null && (urlPatterns == null || urlPatterns.length == 0) && alias == null) {
			urlPatterns = new String[] { generateRandomErrorPage() };
		}
	}

	public ErrorPageModel getErrorPageModel() {
		return errorPageModel;
	}

	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}

	public boolean isDynamic() {
		return dynamic;
	}

	/**
	 * Special configuration just before registration of the {@link ServletModel} if it's a JSP servlet
	 * @param config
	 */
	public void configureJspServlet(JspConfiguration config) {
		// important configuration of JSP servlet - passed using init parameters
		// see org.apache.jasper.EmbeddedServletOptions.EmbeddedServletOptions() and
		// https://tomcat.apache.org/tomcat-9.0-doc/jasper-howto.html#Configuration

		if (config != null) {
			/*
			 * "+" - set explicitly to opinionated value in Pax Web
			 * "-" - not handled yet in Pax Web
			 * "~" - not handled in Pax Web, because Jasper's default is fine
			 * "!" - not handled in Pax Web, because it's not relevant
			 *
			 * ~ checkInterval - If development is false and checkInterval is greater than zero, background compiles are enabled. checkInterval is the time in seconds between checks to see if a JSP page (and its dependent files) needs to be recompiled. Default 0 seconds.
			 * ~ compilerClassName
			 * ~ compilerSourceVM - What JDK version are the source files compatible with? (Default value: 1.7)
			 * ~ compilerTargetVM - What JDK version are the generated files compatible with? (Default value: 1.7)
			 * ~ displaySourceFragment - Should a source fragment be included in exception messages? true or false, default true.
			 * ~ dumpSmap - Should the SMAP info for JSR45 debugging be dumped to a file? true or false, default false. false if suppressSmap is true.
			 * ~ enablePooling - Determines whether tag handler pooling is enabled. This is a compilation option. It will not alter the behaviour of JSPs that have already been compiled. true or false, default true.
			 * ~ engineOptionsClass - Allows specifying the Options class used to configure Jasper. If not present, the default EmbeddedServletOptions will be used. This option is ignored if running under a SecurityManager.
			 * ~ errorOnUseBeanInvalidClassAttribute - Should Jasper issue an error when the value of the class attribute in an useBean action is not a valid bean class? true or false, default true.
			 * ~ genStringAsCharArray - Should text strings be generated as char arrays, to improve performance in some cases? Default false.
			 * ~ ieClassId - The class-id value to be sent to Internet Explorer when using <jsp:plugin> tags. Default clsid:8AD9C840-044E-11D1-B3E9-00805F499D93.
			 * ~ javaEncoding - Java file encoding to use for generating java source files. Default UTF8.
			 * ~ jspIdleTimeout - The amount of time in seconds a JSP can be idle before it is unloaded. A value of zero or less indicates never unload. Default -1
			 * ~ keepgenerated - Should we keep the generated Java source code for each page instead of deleting it? true or false, default true.
			 * ~ mappedfile - Should we generate static content with one print statement per input line, to ease debugging? true or false, default true.
			 * ~ modificationTestInterval - Causes a JSP (and its dependent files) to not be checked for modification during the specified time interval (in seconds) from the last time the JSP was checked for modification. A value of 0 will cause the JSP to be checked on every access. Used in development mode only. Default is 4 seconds.
			 * ~ quoteAttributeEL - When EL is used in an attribute value on a JSP page, should the rules for quoting of attributes described in JSP.1.6 be applied to the expression? true or false, default true.
			 * ~ recompileOnFail - If a JSP compilation fails should the modificationTestInterval be ignored and the next access trigger a re-compilation attempt? Used in development mode only and is disabled by default as compilation may be expensive and could lead to excessive resource usage.
			 * ~ strictQuoteEscaping - When scriptlet expressions are used for attribute values, should the rules in JSP.1.6 for the escaping of quote characters be strictly applied? true or false, default true.
			 * ~ trimSpaces - Should template text that consists entirely of whitespace be removed? true or false, default false.
			 * ~ xpoweredBy - Determines whether X-Powered-By response header is added by generated servlet. true or false, default false.
			 * ! classpath - Defines the class path to be used to compile the generated servlets. This parameter only has an effect if the ServletContext attribute org.apache.jasper.Constants.SERVLET_CLASSPATH is not set. This attribute is always set when Jasper is used within Tomcat. By default the classpath is created dynamically based on the current web application.
			 * ! compiler - Which compiler Ant should use to compile JSP pages. The valid values for this are the same as for the compiler attribute of Ant's javac task. If the value is not set, then the default Eclipse JDT Java compiler will be used instead of using Ant. There is no default value. If this attribute is set then setenv.[sh|bat] should be used to add ant.jar, ant-launcher.jar and tools.jar to the CLASSPATH environment variable.
			 * ! fork - Have Ant fork JSP page compiles so they are performed in a separate JVM from Tomcat? true or false, default true.
			 * ! maxLoadedJsps - The maximum number of JSPs that will be loaded for a web application. If more than this number of JSPs are loaded, the least recently used JSPs will be unloaded so that the number of JSPs loaded at any one time does not exceed this limit. A value of zero or less indicates no limit. Default -1
			 * + classdebuginfo - Should the class file be compiled with debugging information? true or false, default true.
			 * + development - Is Jasper used in development mode? If true, the frequency at which JSPs are checked for modification may be specified via the modificationTestInterval parameter.true or false, default true.
			 * + scratchdir - What scratch directory should we use when compiling JSP pages? Default is the work directory for the current web application. This option is ignored if running under a SecurityManager.
			 * + suppressSmap - Should the generation of SMAP info for JSR45 debugging be suppressed? true or false, default false.
			 */
			if (initParams.get("scratchdir") == null) {
				// I know that there (in theory) may be more, but let's take the first one, as I don't expect
				// more contexts when working with JSPs - JSP servlet can easily be registered with HttpService. With
				// Whiteboard we still can register such servlet but with much hassle
				OsgiContextModel ocm = getContextModels().get(0);
				initParams.put("scratchdir", config.getJspScratchDir(ocm));
			}
			initParams.putIfAbsent("development", "false"); // to prevent checking for lastModified on bundle resources
			initParams.putIfAbsent("suppressSmap", "true");
			initParams.putIfAbsent("classdebuginfo", "false");
		}
	}

	@Override
	public String toString() {
		return "ServletModel{id=" + getId()
				+ ",name='" + name + "'"
				+ (alias == null ? "" : ",alias='" + alias + "'")
				+ (urlPatterns == null ? "" : ",urlPatterns=" + Arrays.toString(urlPatterns))
				+ (servlet == null ? "" : ",servlet=" + servlet)
				+ (servletClass == null ? "" : ",servletClass=" + servletClass)
				+ ",contexts=" + getContextModelsInfo()
				+ "}";
	}

	public static class Builder {

		private String alias;
		private String[] urlPatterns;
		private String servletName;
		private Map<String, String> initParams;
		private Integer loadOnStartup;
		private Boolean asyncSupported;
		private MultipartConfigElement multipartConfigElement;
		private String[] errorDeclarations;
		private Servlet servlet;
		private Class<? extends Servlet> servletClass;
		private ServiceReference<Servlet> reference;
		private Supplier<? extends Servlet> supplier;
		private final List<OsgiContextModel> list = new LinkedList<>();
		private Bundle bundle;
		private int rank;
		private long serviceId;
		private boolean resourceServlet = false;
		private boolean jspServlet = false;
		private String jspFile;
		// only for resource servlet
		private String resourcePath;
		// it means the definition may be overriden later (e.g., in SCI through ServletContext.addServlet())
		private boolean overridable;

		public Builder() {
		}

		public Builder(String servletName) {
			this.servletName = servletName;
		}

		public Builder withAlias(String alias) {
			this.alias = alias;
			return this;
		}

		public Builder withUrlPatterns(String[] urlPatterns) {
			this.urlPatterns = urlPatterns;
			return this;
		}

		public Builder withServletName(String servletName) {
			this.servletName = servletName;
			return this;
		}

		public Builder withInitParams(Map<String, String> initParams) {
			this.initParams = initParams;
			return this;
		}

		public Builder withLoadOnStartup(Integer loadOnStartup) {
			this.loadOnStartup = loadOnStartup;
			return this;
		}

		public Builder withAsyncSupported(Boolean asyncSupported) {
			this.asyncSupported = asyncSupported;
			return this;
		}

		public Builder withMultipartConfigElement(MultipartConfigElement multipartConfigElement) {
			this.multipartConfigElement = multipartConfigElement;
			return this;
		}

		public Builder withServlet(Servlet servlet) {
			this.servlet = servlet;
			return this;
		}

		public Builder withServletClass(Class<? extends Servlet> servletClass) {
			this.servletClass = servletClass;
			return this;
		}

		public Builder withServletReference(ServiceReference<Servlet> reference) {
			this.reference = reference;
			return this;
		}

		public Builder withServletReference(Bundle bundle, ServiceReference<Servlet> reference) {
			this.bundle = bundle;
			this.reference = reference;
			return this;
		}

		public Builder withServletSupplier(Supplier<? extends Servlet> supplier) {
			this.supplier = supplier;
			return this;
		}

		public Builder withOsgiContextModel(OsgiContextModel osgiContextModel) {
			this.list.add(osgiContextModel);
			return this;
		}

		public Builder withOsgiContextModels(final Collection<OsgiContextModel> osgiContextModels) {
			this.list.addAll(osgiContextModels);
			return this;
		}

		public Builder withRegisteringBundle(Bundle bundle) {
			this.bundle = bundle;
			return this;
		}

		public Builder withServiceRankAndId(int rank, long id) {
			this.rank = rank;
			this.serviceId = id;
			return this;
		}

		public Builder resourceServlet(boolean resourceServlet) {
			this.resourceServlet = resourceServlet;
			return this;
		}

		public Builder jspServlet(boolean jspServlet) {
			this.jspServlet = jspServlet;
			return this;
		}

		public Builder withRawPath(String path) {
			this.resourcePath = path;
			return this;
		}

		public Builder withErrorDeclarations(String[] errorDeclarations) {
			this.errorDeclarations = errorDeclarations;
			return this;
		}

		public Builder withServletJspFile(String jspFile) {
			this.jspFile = jspFile;
			this.jspServlet = true;
			return this;
		}

		public Builder setOverridable(boolean overridable) {
			this.overridable = overridable;
			return this;
		}

		public ServletModel build() {
			ServletModel model = new ServletModel(alias, urlPatterns, servletName, initParams,
					loadOnStartup, asyncSupported, multipartConfigElement, servlet, servletClass, reference,
					supplier, resourceServlet, bundle);
			list.forEach(model::addContextModel);
			model.setServiceRank(this.rank);
			model.setServiceId(this.serviceId);
			model.setRawPath(resourcePath);
			model.setErrorDeclarations(errorDeclarations);
			model.setJspServlet(jspServlet);
			model.setJspFile(jspFile);
			model.setOverridable(overridable);
			return model;
		}
	}

}
