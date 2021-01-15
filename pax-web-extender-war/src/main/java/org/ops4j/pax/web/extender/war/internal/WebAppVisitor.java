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

/**
 * Visitor to web app elements.
 *
 * @author Alin Dreghiciu, Achim
 * @since 0.3.0, December 27, 2007
 */
public interface WebAppVisitor {

	/**
	 * Called once per web application.
	 *
	 * @param webApp visited web application
	 */
	void visit(BundleWebApplication webApp);

	/**
	 * Called once for each servlet.
	 *
	 * @param webAppServlet visited servlet
	 */
	void visit(WebAppServlet webAppServlet);

	/**
	 * Called once for each filter.
	 *
	 * @param webAppFilter visited filter
	 */
	void visit(WebAppFilter webAppFilter);

	/**
	 * Called once for each listener.
	 *
	 * @param webAppListener visited listener
	 */
	void visit(WebAppListener webAppListener);

	/**
	 * Called once for each error page.
	 *
	 * @param webAppErrorPage visited error page
	 */
	void visit(WebAppErrorPage webAppErrorPage);

	/**
	 * Called once for each login config element.
	 *
	 * @param loginConfig visited login config
	 */
	void visit(WebAppLoginConfig loginConfig);

	/**
	 * Called once for each constraint mapping element.
	 *
	 * @param constraintMapping visited constraint mapping
	 */
	void visit(WebAppConstraintMapping constraintMapping);

	/**
	 * Called when the web app visit is terminated
	 */
	void end();

}
