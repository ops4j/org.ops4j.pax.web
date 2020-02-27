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

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;

import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.util.Path;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * Set of parameters describing everything that's required to register a {@link Servlet}.
 */
public class ServletModel extends ElementModel<Servlet> {

	/** Alias as defined by old {@link org.osgi.service.http.HttpService} registration methods */
	private final String alias;

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
	private final Map<String, String> initParams;

	/** {@code <servlet>/<load-on-startup>} */
	private final Integer loadOnStartup;

	/** {@code <servlet>/<async-supported>} */
	private final Boolean asyncSupported;

	/** {@code <servlet>/<multipart-config>} */
	private final MultipartConfigElement multipartConfigElement;

	/**
	 * Both Http Service and Whiteboard service allows registration of servlets using existing instance.
	 */
	private final Servlet servlet;

	/**
	 * Actual class of the servlet, to be instantiated by servlet container itself. {@code <servlet>/<servlet-class>}.
	 * This can only be set when registering Pax Web specific
	 * {@link org.ops4j.pax.web.service.whiteboard.ServletMapping} "direct Whiteboard" service.
	 */
	private final Class<? extends Servlet> servletClass;

	public ServletModel(String alias, Servlet servlet, Dictionary<?,?> initParams, Integer loadOnStartup, Boolean asyncSupported) {
		this(alias, null, null, Utils.toMap(initParams),
				loadOnStartup, asyncSupported, null,
				servlet, null);
	}

	public ServletModel(String servletName, String[] urlPatterns, Servlet servlet, Dictionary<String, String> initParams,
			Integer loadOnStartup, Boolean asyncSupported, MultipartConfigElement multiPartConfig) {
		this(null, urlPatterns, servletName, Utils.toMap(initParams),
				loadOnStartup, asyncSupported, multiPartConfig,
				servlet, null);
	}

	public ServletModel(String[] urlPatterns, Class<? extends Servlet> servletClass, Dictionary<String, String> initParams,
			Integer loadOnStartup, Boolean asyncSupported, MultipartConfigElement multiPartConfig) {
		this(null, urlPatterns, null, Utils.toMap(initParams),
				loadOnStartup, asyncSupported, multiPartConfig,
				null, servletClass);
	}

	@SuppressWarnings("deprecation")
	private ServletModel(String alias, String[] urlPatterns, String name, Map<String, String> initParams,
			Integer loadOnStartup, Boolean asyncSupported, MultipartConfigElement multipartConfigElement,
			Servlet servlet, Class<? extends Servlet> servletClass) {
		this.alias = alias;
		this.urlPatterns = Path.normalizePatterns(urlPatterns);
		this.name = name;
		this.initParams = initParams == null ? Collections.emptyMap() : initParams;
		this.loadOnStartup = loadOnStartup;
		this.asyncSupported = asyncSupported;
		this.multipartConfigElement = multipartConfigElement;
		this.servlet = servlet;
		this.servletClass = servletClass;

		int sources = 0;
		sources += (servlet != null ? 1 : 0);
		sources += (servletClass != null ? 1 : 0);
		sources += (getElementReference() != null ? 1 : 0);
		if (sources == 0) {
			throw new IllegalArgumentException("Servlet Model must specify one of: servlet instance, servlet class"
					+ " or service reference");
		}
		if (sources != 1) {
			throw new IllegalArgumentException("Servlet Model should specify a servlet uniquely as instance, class"
					+ " or service reference");
		}

		if (this.name == null) {
			// legacy method first
			this.name = this.initParams.get(PaxWebConstants.INIT_PARAM_SERVLET_NAME);
		}
		if (this.name == null) {
			// Whiteboard Specification 140.4 Registering Servlets
			Class<? extends Servlet> c = null;
			if (this.servletClass != null) {
				c = this.servletClass;
			} else if (this.servlet != null) {
				c = this.servlet.getClass();
			}
			if (c != null) {
				this.name = c.getName();
			} else if (getElementReference() != null) {
				Object objectClass = getElementReference().getProperty(Constants.OBJECTCLASS);
				if (objectClass instanceof String) {
					this.name = (String) objectClass;
				} else if (objectClass instanceof String[] && ((String[]) objectClass).length > 0) {
					this.name = ((String[]) objectClass)[0];
				}
			}
		}
		if (this.name == null) {
			// no idea how to obtain the class, but this should not happen
			this.name = UUID.randomUUID().toString();
		}

		if (this.alias == null && this.urlPatterns == null) {
			throw new IllegalArgumentException("Neither alias nor URL patterns is specified");
		}
		if (this.alias != null && this.urlPatterns != null && this.urlPatterns.length > 0) {
			throw new IllegalArgumentException("Can't specify both alias and URL patterns");
		}

		if (this.alias != null) {
			if (!this.alias.startsWith("/")) {
				throw new IllegalArgumentException("Alias does not start with slash (/)");
			}
			// "/" must be allowed
			if (alias.length() > 1 && alias.endsWith("/")) {
				throw new IllegalArgumentException("Alias should not end with slash (/)");
			}
		}

		if (this.urlPatterns == null) {
			// Http Service specification 102.4 Mapping HTTP Requests to Servlet and Resource Registrations:
			// [...]
			// 6. If there is no match, the Http Service must attempt to match sub-strings of the requested
			//    URI to registered aliases. The sub-strings of the requested URI are selected by removing
			//    the last "/" and everything to the right of the last "/".
			this.urlPatterns = new String[] { this.alias + "/*" };
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
				+ ",contexts=" + getContextModels()
				+ "}";
	}

//	/*
//	 * From web app XSD:
//	 * The servlet-name element contains the canonical name of the
//	 * servlet. Each servlet name is unique within the web application.
//	 */
//	@Override
//	public boolean equals(Object o) {
//		if (this == o) {
//		    return true;
//		}
//		if (o == null || getClass() != o.getClass()) {
//		    return false;
//		}
//		ServletModel that = (ServletModel) o;
//		return Objects.equals(getName(), that.getName()) &&
//				Objects.equals(getContextModel().getContextName(), that.getContextModel().getContextName());
//	}
//
//	@Override
//	public int hashCode() {
//		return Objects.hash(getName(), getContextModel().getContextName());
//	}

	public String getAlias() {
		return alias;
	}

	public String[] getUrlPatterns() {
		return urlPatterns;
	}

	public String getName() {
		return name;
	}

	public Map<String, String> getInitParams() {
		return initParams;
	}

	public Integer getLoadOnStartup() {
		return loadOnStartup;
	}

	public Boolean getAsyncSupported() {
		return asyncSupported;
	}

	public MultipartConfigElement getMultipartConfigElement() {
		return multipartConfigElement;
	}

	public Servlet getServlet() {
		return servlet;
	}

	public Class<? extends Servlet> getServletClass() {
		return servletClass;
	}

	public static class Builder {

		private String alias;
		private String[] urlPatterns;
		private String servletName;
		private Map<String, String> initParams;
		private Integer loadOnStartup;
		private Boolean asyncSupported;
		private MultipartConfigElement multipartConfigElement;
		private Servlet servlet;
		private Class<? extends Servlet> servletClass;
		private final List<OsgiContextModel> list = new LinkedList<>();
		private Bundle bundle;

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

		public Builder withOsgiContextModel(OsgiContextModel osgiContextModel) {
			this.list.add(osgiContextModel);
			return this;
		}

		public Builder withRegisteringBundle(Bundle bundle) {
			this.bundle = bundle;
			return this;
		}

		public ServletModel build() {
			ServletModel model = new ServletModel(alias, urlPatterns, servletName, initParams, loadOnStartup, asyncSupported, multipartConfigElement, servlet, servletClass);
			list.forEach(model::addContextModel);
			model.setRegisteringBundle(this.bundle);
			return model;
		}
	}

}
