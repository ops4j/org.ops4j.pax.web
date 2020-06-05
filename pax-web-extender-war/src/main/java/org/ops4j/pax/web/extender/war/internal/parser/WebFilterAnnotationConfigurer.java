/*
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

import java.util.EnumSet;
import java.util.List;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.annotation.WebInitParam;

import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.model.WebAppFilter;
import org.ops4j.pax.web.extender.war.internal.model.WebAppFilterMapping;
import org.ops4j.pax.web.extender.war.internal.model.WebAppInitParam;
import org.ops4j.pax.web.utils.FilterAnnotationScanner;
import org.osgi.framework.Bundle;

/**
 * @author achim
 */
public class WebFilterAnnotationConfigurer extends
		AnnotationConfigurer<WebFilterAnnotationConfigurer> {

	public WebFilterAnnotationConfigurer(Bundle bundle, String className) {
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

		@SuppressWarnings("unchecked")
        FilterAnnotationScanner annotationParam = new FilterAnnotationScanner((Class<? extends Filter>) clazz);

		//WebAppFilter filter = webApp.findFilter(annotationParam.filterName);

		if (webApp.findFilter(annotationParam.filterName) == null) {
			final WebAppFilter filter = new WebAppFilter();
			filter.setFilterName(annotationParam.filterName);
			filter.setFilterClass(className);
			webApp.addFilter(filter);

			// TODO: what about the DisplayName?

			// holder.setDisplayName(filterAnnotation.displayName());
			// metaData.setOrigin(name+".filter.display-name");
			
			annotationParam.webInitParams.forEach((name,value) -> {
			    WebAppInitParam initParam = new WebAppInitParam();
                initParam.setParamName(name);
                initParam.setParamValue(value);
                filter.addInitParam(initParam);
			});

			
//			for (WebInitParam ip : annotationParam.webInitParams) {
//				WebAppInitParam initParam = new WebAppInitParam();
//				initParam.setParamName(ip.name());
//				initParam.setParamValue(ip.value());
//				filter.addInitParam(initParam);
//			}
			
			
			for (String urlPattern : annotationParam.urlPatterns) {
				WebAppFilterMapping mapping = new WebAppFilterMapping();
				mapping.setFilterName(annotationParam.filterName);
				mapping.setUrlPattern(urlPattern);
				webApp.addFilterMapping(mapping);
			}

			for (String servletName : annotationParam.servletNames) {
				WebAppFilterMapping mapping = new WebAppFilterMapping();
				mapping.setFilterName(annotationParam.filterName);
				mapping.setServletName(servletName);
				webApp.addFilterMapping(mapping);
			}

			EnumSet<DispatcherType> dispatcherSet = EnumSet
					.noneOf(DispatcherType.class);
			for (String dispatcherString : annotationParam.dispatcherTypes) {
				dispatcherSet.add(DispatcherType.valueOf(dispatcherString));
			}
			WebAppFilterMapping mapping = new WebAppFilterMapping();
			mapping.setDispatcherTypes(dispatcherSet);
			mapping.setFilterName(annotationParam.filterName);
			webApp.addFilterMapping(mapping);
		} else {
		    final WebAppFilter filter = webApp.findFilter(annotationParam.filterName);
			WebAppInitParam[] initParams = filter.getInitParams();
			// A Filter definition for the same name already exists from web.xml
			// ServletSpec 3.0 p81 if the Filter is already defined and has
			// mappings,
			// they override the annotation. If it already has DispatcherType
			// set, that
			// also overrides the annotation. Init-params are additive, but
			// web.xml overrides
			// init-params of the same name.
			
			annotationParam.webInitParams.forEach((name,value) -> {
			    if (!initParamsContain(initParams, annotationParam.filterName)) {
                    WebAppInitParam initParam = new WebAppInitParam();
                    initParam.setParamName(name);
                    initParam.setParamValue(value);
                    filter.addInitParam(initParam);
                }
			});
			
//			for (WebInitParam ip : annotationParam.webInitParams) {
//				// if (holder.getInitParameter(ip.name()) == null)
//				if (!initParamsContain(initParams, annotationParam.filterName)) {
//					WebAppInitParam initParam = new WebAppInitParam();
//					initParam.setParamName(ip.name());
//					initParam.setParamValue(ip.value());
//					filter.addInitParam(initParam);
//				}
//			}

			List<WebAppFilterMapping> filterMappings = webApp
					.getFilterMappings(annotationParam.filterName);

			boolean mappingExists = false;
			for (WebAppFilterMapping m : filterMappings) {
				if (m.getFilterName().equalsIgnoreCase(
						annotationParam.filterName)) {
					mappingExists = true;
					break;
				}
			}
			// if a descriptor didn't specify at least one mapping, use the
			// mappings from the annotation and the DispatcherTypes
			// from the annotation
			if (!mappingExists) {

				for (String urlPattern : annotationParam.urlPatterns) {
					WebAppFilterMapping mapping = new WebAppFilterMapping();
					mapping.setFilterName(annotationParam.filterName);
					mapping.setUrlPattern(urlPattern);
					webApp.addFilterMapping(mapping);
				}

				for (String servletName : annotationParam.servletNames) {
					WebAppFilterMapping mapping = new WebAppFilterMapping();
					mapping.setFilterName(annotationParam.filterName);
					mapping.setServletName(servletName);
					webApp.addFilterMapping(mapping);
				}

				EnumSet<DispatcherType> dispatcherSet = EnumSet
						.noneOf(DispatcherType.class);
				for (String dispatcherString : annotationParam.dispatcherTypes) {
					dispatcherSet.add(DispatcherType.valueOf(dispatcherString));
				}
				WebAppFilterMapping mapping = new WebAppFilterMapping();
				mapping.setDispatcherTypes(dispatcherSet);
				mapping.setFilterName(annotationParam.filterName);
				webApp.addFilterMapping(mapping);
			}
		}

	}

}
