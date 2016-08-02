/*
 * Copyright 2014 Achim Nierbeck.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.ops4j.pax.web.utils;

import javax.servlet.DispatcherType;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterAnnotationScanner {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	public Boolean scanned = false;
	public String[] urlPatterns;
	public String filterName;
	public Integer loadOnStartup;
	public Boolean asyncSupported;
	public WebInitParam[] webInitParams;
	public MultipartConfig multiPartConfigAnnotation;
	public WebFilter filterAnnotation;
	public String[] servletNames;

	public DispatcherType[] dispatcherTypes;

	public FilterAnnotationScanner(Class<?> clazz) {
		filterAnnotation = (WebFilter) clazz.getAnnotation(WebFilter.class);

		if (filterAnnotation == null) {
			return;
		}

		scanned = true;

		multiPartConfigAnnotation = (MultipartConfig) clazz.getAnnotation(MultipartConfig.class);

		if (filterAnnotation.urlPatterns().length > 0
				&& filterAnnotation.value().length > 0) {
			log.warn(clazz.getName()
					+ " defines both @WebFilter.value and @WebFilter.urlPatterns");
			return;
		}

		urlPatterns = filterAnnotation.value();
		if (urlPatterns.length == 0) {
			urlPatterns = filterAnnotation.urlPatterns();
		}

		filterName = (filterAnnotation.filterName().equals("") ? clazz
				.getName()
				: filterAnnotation.filterName());


		webInitParams = filterAnnotation.initParams();

		servletNames = filterAnnotation.servletNames();

		dispatcherTypes = filterAnnotation.dispatcherTypes();

		asyncSupported = filterAnnotation.asyncSupported();

	}

}
