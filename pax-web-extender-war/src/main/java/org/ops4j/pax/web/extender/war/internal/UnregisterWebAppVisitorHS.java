/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.war.internal;

import org.ops4j.pax.web.extender.war.internal.model.BundleWebApplication;
import org.ops4j.pax.web.extender.war.internal.model.WebAppConstraintMapping;
import org.ops4j.pax.web.extender.war.internal.model.WebAppErrorPage;
import org.ops4j.pax.web.extender.war.internal.model.WebAppFilter;
import org.ops4j.pax.web.extender.war.internal.model.WebAppListener;
import org.ops4j.pax.web.extender.war.internal.model.WebAppLoginConfig;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServlet;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A visitor that unregisters a web application. Cannot be reused, it has to be
 * one per visit.
 */
class UnregisterWebAppVisitorHS implements WebAppVisitor {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(UnregisterWebAppVisitorWC.class);
	/**
	 * HttpService to be used for registration.
	 */
	private final HttpService httpService;

	/**
	 * Creates a new unregistration visitor.
	 *
	 * @param httpService http service to be used for unregistration. Cannot be null.
	 * @throws NullArgumentException if http service is null
	 */
	UnregisterWebAppVisitorHS(final HttpService httpService) {
//		NullArgumentException.validateNotNull(httpService, "Http Service");
		this.httpService = httpService;
	}

	/**
	 * Unregisters resources related to web app.
	 *
	 * @see WebAppVisitor#visit(BundleWebApplication)
	 */
	public void visit(final BundleWebApplication webApp) {
		//CHECKSTYLE:OFF
		try {
			httpService.unregister("/");
		} catch (Exception ignore) {
			LOG.warn("Unregistration exception. Skipping.", ignore);
		}
		//CHECKSTYLE:ON
	}

	/**
	 * Unregisters servlet from http context.
	 *
	 * @throws NullArgumentException if servlet is null
	 * @see WebAppVisitor#visit(WebAppServlet)
	 */
	public void visit(final WebAppServlet webAppServlet) {
		//CHECKSTYLE:OFF
//		NullArgumentException.validateNotNull(webAppServlet, "Web app servlet");
		final String[] aliases = webAppServlet.getAliases();
		if (aliases != null && aliases.length > 0) {
			for (String alias : aliases) {
				try {
					httpService.unregister(alias);
				} catch (Exception ignore) {
					LOG.warn("Unregistration exception. Skipping.", ignore);
				}
			}
		} else {
			LOG.warn("Servlet [" + webAppServlet
					+ "] does not have any alias. Skipped.");
		}
		//CHECKSTYLE:OFF
	}

	/**
	 * Does nothing as standard http service does not support filters.
	 *
	 * @see WebAppVisitor#visit(WebAppFilter)
	 */
	public void visit(final WebAppFilter webAppFilter) {
		LOG.info("Pax Web not available. Skipping filter unregistration for ["
				+ webAppFilter + "]");
	}

	/**
	 * Does nothing as standard http service does not support listeners.
	 *
	 * @see WebAppVisitor#visit(WebAppListener)
	 */
	public void visit(final WebAppListener webAppListener) {
		LOG.info("Pax Web not available. Skipping listener unregistration for ["
				+ webAppListener + "]");
	}

	/**
	 * Does nothing as standard http service does not support error pages.
	 *
	 * @see WebAppVisitor#visit(WebAppListener)
	 */
	public void visit(final WebAppErrorPage webAppErrorPage) {
		LOG.info("Pax Web not available. Skipping error page unregistration for ["
				+ webAppErrorPage + "]");
	}

	public void visit(WebAppLoginConfig loginConfig) {
		LOG.info("Pax Web not available. Skipping login config unregistration for ["
				+ loginConfig + "]");
	}

	public void visit(WebAppConstraintMapping constraintMapping) {
		LOG.info("Pax Web not available. Skipping constraint mapping unregistration for ["
				+ constraintMapping + "]");
	}

	public void end() {
	}
}
