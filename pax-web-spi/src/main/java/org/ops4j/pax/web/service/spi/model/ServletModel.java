/* Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.spi.model;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Map;
import java.util.Objects;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.spi.util.ConversionUtil;
import org.ops4j.pax.web.service.spi.util.Path;

public class ServletModel extends Model {

	/**
	 * Servlet init param name for specifying a servlet name.
	 */
	public static final String SERVLET_NAME = "servlet-name";

	private final Class<? extends Servlet> servletClass;
	private Servlet servlet;
	private final String alias;
	private final String[] urlPatterns;
	private final Map<String, String> initParams;
	private final String name;
	private final Integer loadOnStartup;
	private final Boolean asyncSupported;

	private MultipartConfigElement multipartConfigElement;

	public ServletModel(final ContextModel contextModel, final Servlet servlet,
						final String alias, final Dictionary<String, ?> initParams,
						final Integer loadOnStartup, final Boolean asyncSupported) {
		this(contextModel, servlet, null,
				new String[]{aliasAsUrlPattern(alias)},
				validateAlias(alias), initParams, loadOnStartup, asyncSupported, null);
	}

	public ServletModel(final ContextModel contextModel, final Servlet servlet,
						final String alias, final Dictionary<String, ?> initParams,
						final Integer loadOnStartup, final Boolean asyncSupported, MultipartConfigElement multiPartConfig) {
		this(contextModel, servlet, null,
				new String[]{aliasAsUrlPattern(alias)},
				validateAlias(alias), initParams, loadOnStartup, asyncSupported, multiPartConfig);
	}

	public ServletModel(final ContextModel contextModel, final Servlet servlet,
						final String servletName, final String[] urlPatterns,
						final String alias, final Dictionary<String, ?> initParams,
						final Integer loadOnStartup, final Boolean asyncSupported, MultipartConfigElement multiPartConfig) {
		this(contextModel, null, servlet, servletName, urlPatterns, alias,
				initParams, loadOnStartup, asyncSupported, multiPartConfig);
	}

	public ServletModel(final ContextModel contextModel,
						final Class<? extends Servlet> servletClass,
						final String servletName, final String[] urlPatterns,
						final String alias, final Dictionary<String, ?> initParams,
						final Integer loadOnStartup, final Boolean asyncSupported, MultipartConfigElement multiPartConfig) {
		this(contextModel, servletClass, null, servletName, urlPatterns, alias,
				initParams, loadOnStartup, asyncSupported, multiPartConfig);
	}

	private ServletModel(final ContextModel contextModel,
						 final Class<? extends Servlet> servletClass, final Servlet servlet,
						 final String servletName, final String[] urlPatterns,
						 final String alias, final Dictionary<String, ?> initParameters,
						 final Integer loadOnStartup, final Boolean asyncSupported, final MultipartConfigElement multiPartConfig) {
		super(contextModel);
		if (servletClass == null) {
			NullArgumentException.validateNotNull(servlet, "Servlet");
		}
		if (servlet == null) {
			NullArgumentException.validateNotNull(servletClass, "ServletClass");
		}
		NullArgumentException.validateNotNull(urlPatterns, "Url patterns");
		if (urlPatterns.length == 0 && !(loadOnStartup >= 0 && loadOnStartup < Integer.MAX_VALUE)) {
			throw new IllegalArgumentException(
					"Registered servlet must have at least one url pattern");
		}
		this.urlPatterns = Path.normalizePatterns(urlPatterns);
		this.alias = alias;
		this.servletClass = servletClass;
		this.servlet = servlet;
		this.initParams = ConversionUtil.convertToMap(initParameters);
		String tmpName = servletName;
		if (tmpName == null) {
			tmpName = initParams.get(SERVLET_NAME);
		}
		if (tmpName == null) {
			tmpName = getId();
		}
		this.name = tmpName;
		this.loadOnStartup = loadOnStartup;
		this.asyncSupported = asyncSupported;
		this.multipartConfigElement = multiPartConfig;
	}

	public String getName() {
		return name;
	}

	public String[] getUrlPatterns() {
		return urlPatterns;
	}

	public String getAlias() {
		return alias;
	}

	public Class<? extends Servlet> getServletClass() {
		return servletClass;
	}

	public Servlet getServlet() {
		return servlet;
	}

	public Map<String, String> getInitParams() {
		return initParams;
	}

	/**
	 * @return the loadOnStartup
	 */
	public Integer getLoadOnStartup() {
		return loadOnStartup;
	}

	/**
	 * @return the asyncSupported
	 */
	public Boolean getAsyncSupported() {
		return asyncSupported;
	}

	/**
	 * Validates that aan alias conforms to OSGi specs requirements. See OSGi R4
	 * Http Service specs for details about alias validation.
	 *
	 * @param alias to validate
	 * @return received alias if validation succeeds
	 * @throws IllegalArgumentException if validation fails
	 */
	private static String validateAlias(final String alias) {
		NullArgumentException.validateNotNull(alias, "Alias");
		if (!alias.startsWith("/")) {
			throw new IllegalArgumentException(
					"Alias does not start with slash (/)");
		}
		// "/" must be allowed
		if (alias.length() > 1 && alias.endsWith("/")) {
			throw new IllegalArgumentException("Alias ends with slash (/)");
		}
		return alias;
	}

	/**
	 * Transforms an alias into a url pattern.
	 *
	 * @param alias to transform
	 * @return url pattern
	 */
	private static String aliasAsUrlPattern(final String alias) {
		String urlPattern = alias;
		if (urlPattern != null && !urlPattern.equals("/")
				&& !urlPattern.contains("*")) {
			if (urlPattern.endsWith("/")) {
				urlPattern = urlPattern + "*";
			} else {
				urlPattern = urlPattern + "/*";
			}
		}
		return urlPattern;
	}

	/*
	 * From web app XSD:
	 * The servlet-name element contains the canonical name of the
	 * servlet. Each servlet name is unique within the web application.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ServletModel that = (ServletModel) o;
		return Objects.equals(getName(), that.getName()) &&
				Objects.equals(getContextModel().getContextName(), that.getContextModel().getContextName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getContextModel().getContextName());
	}

	@Override
	public String toString() {
		return new StringBuilder().append(this.getClass().getSimpleName())
				.append("{").append("id=").append(getId()).append(",name=")
				.append(getName()).append(",urlPatterns=")
				.append(Arrays.toString(urlPatterns)).append(",alias=")
				.append(alias).append(",servlet=").append(servlet)
				.append(",initParams=").append(initParams).append(",context=")
				.append(getContextModel()).append("}").toString();
	}

	public Servlet getServletFromName() throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {

		Class<?> clazz = getContextModel().getClassLoader().loadClass(
				getServletClass().getName());
		servlet = (Servlet) clazz.newInstance();
		return servlet;
	}

	public MultipartConfigElement getMultipartConfig() {
		return multipartConfigElement;
	}

}
