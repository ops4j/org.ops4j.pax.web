/**
 * 
 */
package org.ops4j.pax.web.extender.war.internal.parser.dom;

import java.util.EnumSet;
import java.util.List;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;

import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.model.WebAppFilter;
import org.ops4j.pax.web.extender.war.internal.model.WebAppFilterMapping;
import org.ops4j.pax.web.extender.war.internal.model.WebAppInitParam;
import org.osgi.framework.Bundle;

/**
 * @author achim
 * 
 */
public class WebFilterAnnotationScanner extends
		AnnotationScanner<WebFilterAnnotationScanner> {

	public WebFilterAnnotationScanner(Bundle bundle, String className) {
		super(bundle, className);
	}

	public void scan(WebApp webApp) {
		Class<?> clazz = loadClass();

		if (clazz == null) {
			log.warn("Class %s wasn't loaded", this.className);
			return;
		}

		if (!Filter.class.isAssignableFrom(clazz)) {
			log.warn(clazz.getName()
					+ " is not assignable from javax.servlet.Filter");
			return;
		}

		WebFilter filterAnnotation = (WebFilter) clazz
				.getAnnotation(WebFilter.class);

		if (filterAnnotation.value().length > 0
				&& filterAnnotation.urlPatterns().length > 0) {
			log.warn(clazz.getName()
					+ " defines both @WebFilter.value and @WebFilter.urlPatterns");
			return;
		}

		String name = (filterAnnotation.filterName().equals("") ? clazz
				.getName() : filterAnnotation.filterName());
		String[] urlPatterns = filterAnnotation.value();

		WebAppFilter filter = webApp.findFilter(name);

		if (filter == null) {
			filter = new WebAppFilter();
			filter.setFilterName(name);
			filter.setFilterClass(className);

			// TODO: what about the DisplayName?

			// holder.setDisplayName(filterAnnotation.displayName());
			// metaData.setOrigin(name+".filter.display-name");

			for (WebInitParam ip : filterAnnotation.initParams()) {
				WebAppInitParam initParam = new WebAppInitParam();
				initParam.setParamName(ip.name());
				initParam.setParamValue(ip.value());
				filter.addInitParam(initParam);
			}

			for (String urlPattern : urlPatterns) {
				WebAppFilterMapping mapping = new WebAppFilterMapping();
				mapping.setFilterName(name);
				mapping.setUrlPattern(urlPattern);
				webApp.addFilterMapping(mapping);
			}

			for (String servletName : filterAnnotation.servletNames()) {
				WebAppFilterMapping mapping = new WebAppFilterMapping();
				mapping.setFilterName(name);
				mapping.setServletName(servletName);
				webApp.addFilterMapping(mapping);
			}

			EnumSet<DispatcherType> dispatcherSet = EnumSet
					.noneOf(DispatcherType.class);
			for (DispatcherType d : filterAnnotation.dispatcherTypes()) {
				dispatcherSet.add(d);
			}
			WebAppFilterMapping mapping = new WebAppFilterMapping();
			mapping.setDispatcherTypes(dispatcherSet);
			webApp.addFilterMapping(mapping);
		} else {
			WebAppInitParam[] initParams = filter.getInitParams();
			// A Filter definition for the same name already exists from web.xml
			// ServletSpec 3.0 p81 if the Filter is already defined and has
			// mappings,
			// they override the annotation. If it already has DispatcherType
			// set, that
			// also overrides the annotation. Init-params are additive, but
			// web.xml overrides
			// init-params of the same name.
			for (WebInitParam ip : filterAnnotation.initParams()) {
				// if (holder.getInitParameter(ip.name()) == null)
				if (!initParamsContain(initParams, name)) {
					WebAppInitParam initParam = new WebAppInitParam();
					initParam.setParamName(ip.name());
					initParam.setParamValue(ip.value());
					filter.addInitParam(initParam);
				}
			}

			List<WebAppFilterMapping> filterMappings = webApp
					.getFilterMappings(name);

			boolean mappingExists = false;
			for (WebAppFilterMapping m : filterMappings) {
				if (m.getFilterName().equalsIgnoreCase(name)) {
					mappingExists = true;
					break;
				}
			}
			// if a descriptor didn't specify at least one mapping, use the
			// mappings from the annotation and the DispatcherTypes
			// from the annotation
			if (!mappingExists) {

				for (String urlPattern : urlPatterns) {
					WebAppFilterMapping mapping = new WebAppFilterMapping();
					mapping.setFilterName(name);
					mapping.setUrlPattern(urlPattern);
					webApp.addFilterMapping(mapping);
				}

				for (String servletName : filterAnnotation.servletNames()) {
					WebAppFilterMapping mapping = new WebAppFilterMapping();
					mapping.setFilterName(name);
					mapping.setServletName(servletName);
					webApp.addFilterMapping(mapping);
				}

				EnumSet<DispatcherType> dispatcherSet = EnumSet
						.noneOf(DispatcherType.class);
				for (DispatcherType d : filterAnnotation.dispatcherTypes()) {
					dispatcherSet.add(d);
				}
				WebAppFilterMapping mapping = new WebAppFilterMapping();
				mapping.setDispatcherTypes(dispatcherSet);
				webApp.addFilterMapping(mapping);
			}
		}

	}

}
