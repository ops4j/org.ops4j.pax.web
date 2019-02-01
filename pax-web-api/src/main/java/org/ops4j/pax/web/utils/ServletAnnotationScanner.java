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

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServletAnnotationScanner {

	public Boolean scanned = false;
	public String[] urlPatterns;
	public String servletName;
	public Integer loadOnStartup;
	public Boolean asyncSupported;
	public WebInitParam[] webInitParams;
	public MultipartConfig multiPartConfigAnnotation;
	public WebServlet annotation;

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	public ServletAnnotationScanner(Class<?> clazz) {
		WebServlet servletAnnotation = clazz.getAnnotation(WebServlet.class);

		if (servletAnnotation == null) {
			return;
		}

		scanned = true;

		multiPartConfigAnnotation = clazz.getAnnotation(MultipartConfig.class);

		if (servletAnnotation.urlPatterns().length > 0
				&& servletAnnotation.value().length > 0) {
			log.warn(clazz.getName()
					+ " defines both @WebServlet.value and @WebServlet.urlPatterns");
			return;
		}

		urlPatterns = servletAnnotation.value();
		if (urlPatterns.length == 0) {
			urlPatterns = servletAnnotation.urlPatterns();
		}

		if (urlPatterns.length == 0) {
			log.warn(clazz.getName()
					+ " defines neither @WebServlet.value nor @WebServlet.urlPatterns");
			return;
		}

		servletName = (servletAnnotation.name().equals("") ? clazz
				.getName()
				: servletAnnotation.name());

		webInitParams = servletAnnotation.initParams();

		asyncSupported = servletAnnotation.asyncSupported();
		loadOnStartup = servletAnnotation.loadOnStartup();

	}

}
