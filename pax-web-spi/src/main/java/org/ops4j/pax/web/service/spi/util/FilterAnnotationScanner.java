/*
 * Copyright 2014 Achim Nierbeck.
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
package org.ops4j.pax.web.service.spi.util;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.servlet.Filter;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebInitParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to scan for {@link WebFilter} annotation
 */
public class FilterAnnotationScanner {

	private static final Logger LOG = LoggerFactory.getLogger(FilterAnnotationScanner.class);

	public Boolean scanned = false;
	public String[] urlPatterns;
	public String[] servletNames;
	public String filterName;
	public Boolean asyncSupported;
	public Map<String, String> webInitParams;
	public String[] dispatcherTypes;

	public FilterAnnotationScanner(Class<? extends Filter> clazz) {
		WebFilter filterAnnotation = clazz.getAnnotation(WebFilter.class);

		if (filterAnnotation == null) {
			return;
		}

		if (filterAnnotation.urlPatterns().length > 0 && filterAnnotation.value().length > 0) {
			LOG.warn(clazz.getName() + " defines both @WebFilter.value and @WebFilter.urlPatterns");
			return;
		}

		urlPatterns = filterAnnotation.value();
		if (urlPatterns.length == 0) {
			urlPatterns = filterAnnotation.urlPatterns();
		}

		servletNames = filterAnnotation.servletNames();

		if (urlPatterns.length == 0 && servletNames.length == 0) {
			LOG.warn(clazz.getName() + " doesn't define any of @WebFilter.value, @WebFilter.urlPatterns and @WebFilter.servletNames");
			return;
		}

		// It'll default to FQCN anyway in FilterModel
		filterName = (filterAnnotation.filterName().equals("") ? null : filterAnnotation.filterName());

		dispatcherTypes = Arrays.stream(filterAnnotation.dispatcherTypes()).map(Enum::name).toArray(String[]::new);

		WebInitParam[] initParams = filterAnnotation.initParams();
		if (initParams.length > 0) {
			webInitParams = new LinkedHashMap<>();
			for (WebInitParam initParam : initParams) {
				webInitParams.put(initParam.name(), initParam.value());
			}
		}

		asyncSupported = filterAnnotation.asyncSupported();

		scanned = true;
	}

}
