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

import java.util.List;
import java.util.Map.Entry;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.HttpServlet;

import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.model.WebAppInitParam;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServlet;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServletMapping;
import org.ops4j.pax.web.utils.ServletAnnotationScanner;
import org.osgi.framework.Bundle;

/**
 * @author achim
 */
public class WebServletAnnotationConfigurer extends
		AnnotationConfigurer<WebServletAnnotationConfigurer> {

	public WebServletAnnotationConfigurer(Bundle bundle, String clazz) {
		super(bundle, clazz);
	}

	public void scan(WebApp webApp) {
		Class<?> clazz = loadClass();

		if (clazz == null) {
			log.warn("Class {} wasn't loaded", this.className);
			return;
		}

		if (!HttpServlet.class.isAssignableFrom(clazz)) {
			log.warn(clazz.getName()
					+ " is not assignable from javax.servlet.http.HttpServlet");
			return;
		}

		@SuppressWarnings("unchecked")
        ServletAnnotationScanner annotationParameter = new ServletAnnotationScanner((Class<? extends Servlet>) clazz);

		WebAppServlet webAppServlet = webApp
				.findServlet(annotationParameter.servletName);
		log.debug("Registering Servlet {} with url(s) {}",
				annotationParameter.servletName,
				annotationParameter.urlPatterns);

		if (webAppServlet == null) {
			// Add a new Servlet
			log.debug("Create a new Servlet");
			webAppServlet = new WebAppServlet();
			webAppServlet.setServletName(annotationParameter.servletName);
			webAppServlet.setServletClassName(className);
			webApp.addServlet(webAppServlet);
			webAppServlet.setLoadOnStartup(annotationParameter.loadOnStartup);
			webAppServlet.setAsyncSupported(annotationParameter.asyncSupported);
			// TODO: what about the display Name
		} else {
			//PAXWEB-724
			// could be that we found the servlet due to the classname not the servletName
			// this needs to be corrected. 
			annotationParameter.servletName = webAppServlet.getServletName();
		}

		WebAppInitParam[] initParams = webAppServlet.getInitParams();
		// check if the existing servlet has each init-param from the
		// annotation
		// if not, add it
		for (Entry<String, String> entrySet : annotationParameter.webInitParams.entrySet()) {
			// if (holder.getInitParameter(ip.name()) == null)
			if (!initParamsContain(initParams, entrySet.getKey())) {
				WebAppInitParam initParam = new WebAppInitParam();
				initParam.setParamName(entrySet.getKey());
				initParam.setParamValue(entrySet.getValue());
				webAppServlet.addInitParam(initParam);
			}
		}
		// check the url-patterns, if there annotation has a new one, add it
		List<WebAppServletMapping> mappings = webApp
				.getServletMappings(annotationParameter.servletName);

		log.debug("Found the following mappings {} for servlet: {}", mappings,
				annotationParameter.servletName);

		// ServletSpec 3.0 p81 If a servlet already has url mappings from a
		// descriptor the annotation is ignored
		if (mappings == null || mappings.isEmpty()) {
			log.debug("alter/create mappings");
			for (String urlPattern : annotationParameter.urlPatterns) {
				log.debug("adding mapping for URL {}", urlPattern);
				WebAppServletMapping mapping = new WebAppServletMapping();
				mapping.setServletName(annotationParameter.servletName);
				mapping.setUrlPattern(urlPattern);
				webApp.addServletMapping(mapping);
			}
		}

		if (null != annotationParameter.multiPartConfigAnnotation) {
			MultipartConfigElement multipartConfig = new MultipartConfigElement(annotationParameter.multiPartConfigAnnotation);
			webAppServlet.setMultipartConfig(multipartConfig);
		}


	}

}
