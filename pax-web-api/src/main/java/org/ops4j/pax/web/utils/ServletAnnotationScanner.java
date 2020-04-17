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
package org.ops4j.pax.web.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.Servlet;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to scan for {@link WebServlet} annotation
 */
public class ServletAnnotationScanner {

	private static final Logger LOG = LoggerFactory.getLogger(ServletAnnotationScanner.class);

	public Boolean scanned = false;
	public String[] urlPatterns;
	public String servletName;
	public Integer loadOnStartup;
	public Boolean asyncSupported;
	public Map<String, String> webInitParams;
	public MultipartConfig multiPartConfigAnnotation;

	public ServletAnnotationScanner(Class<? extends Servlet> clazz) {
		WebServlet servletAnnotation = clazz.getAnnotation(WebServlet.class);

		if (servletAnnotation == null) {
			return;
		}

		if (servletAnnotation.urlPatterns().length > 0 && servletAnnotation.value().length > 0) {
			LOG.warn(clazz.getName() + " defines both @WebServlet.value and @WebServlet.urlPatterns");
			return;
		}

		urlPatterns = servletAnnotation.value();
		if (urlPatterns.length == 0) {
			urlPatterns = servletAnnotation.urlPatterns();
		}

		if (urlPatterns.length == 0) {
			LOG.warn(clazz.getName() + " defines neither @WebServlet.value nor @WebServlet.urlPatterns");
			return;
		}

		servletName = (servletAnnotation.name().equals("") ? clazz.getName() : servletAnnotation.name());

		WebInitParam[] initParams = servletAnnotation.initParams();
		if (initParams.length > 0) {
			webInitParams = new LinkedHashMap<>();
			for (WebInitParam initParam : initParams) {
				webInitParams.put(initParam.name(), initParam.value());
			}
		}

		asyncSupported = servletAnnotation.asyncSupported();
		loadOnStartup = servletAnnotation.loadOnStartup();

		multiPartConfigAnnotation = clazz.getAnnotation(MultipartConfig.class);

		scanned = true;
	}

}
