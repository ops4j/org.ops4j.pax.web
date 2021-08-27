/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.service.spi.model.views;

import org.ops4j.pax.web.service.spi.task.Batch;
import org.ops4j.pax.web.service.views.PaxWebContainerView;
import org.osgi.framework.Bundle;

/**
 * <p>This interface should be used by pax-web-extender-war, so the manipulation of different web elements and
 * contexts happens as if all of them belonged to single <em>web application</em>.</p>
 *
 * <p>Internally, the same methods of Pax Web {@link org.ops4j.pax.web.service.WebContainer} are used, but additionally
 * we can arrange the operations within a kind of <em>transaction</em>. This is desired and expected if all the
 * web elements belong to single WAR (WAB) and also because some kind of validation was already performed when
 * WAB's {@code web.xml} (and possibly web fragments and annotated elements) was parsed. Also all the elements
 * really belong to single {@link javax.servlet.ServletContext} and share single <em>context path</em>.</p>
 *
 * <p>As with traditional Servlet containers, each WAR (in OSGi: WAB) maps 1:1 to unique <em>servlet context</em>
 * identified by <em>context path</em>. In Whiteboard scenario, many bundles may <em>contribute</em> to a single
 * context (path) by registering servlets, filters, etc. In WAR/WAB scenario, we have to limit it a bit:<ul>
 *     <li>When a WAR/WAB is started to be registered, its associated context path <strong>has to</strong> be free.
 *         We can't register another WAR/WAB if there's existing WAR/WAB or Whiteboard context available.</li>
 *     <li>After the deployment starts and before it finishes, the context (while not ready yet) has to be marked
 *         as <em>unavailable</em>, so no Whiteboard service tries to register and web elements there. This is to
 *         keep validation simple - web elements have to be valid only within a scope of single context.</li>
 *     <li>After the deployment finishes, it should be treated as normal Whiteboard/HttpService context, so we can
 *         <em>alter</em> the context by registering (for example) a Whiteboard filter into WAR/WAB context</li>
 *     <li>TODO_WAB: As of 2021-01-05, the open question is: what to do with Whiteboard elements registered into a context
 *               created for a WAR/WAB after the bundle associated with this WAR/WAB is uninstalled/stopped?</li>
 *     <li>If a WAR/WAB's deployment is attempted and associated context (path) is not free, we have to put such
 *         WAR/WAB into a kind of <em>pending</em> state, because the conflicting context (path) may at some point
 *         be unregistered. This is already implemented for Whiteboard/HttpService scenario, so we need to adjust
 *         the mechanism for WARs/WABs as well. However, 128.3.2 "Starting the Web Application Bundle" says simply that
 *         "If the Context Path value is already in use by another Web Application, then the Web Application must not
 *         be deployed, and the deployment fails. [...] If the prior Web Application with the same Context Path is
 *         undeployed later, this Web Application should be considered as a candidate.</li>
 * </ul></p>
 *
 * <p>Before Pax Web 8 there were special {@code begin()} and {@code end()} methods in
 * {@link org.ops4j.pax.web.service.WebContainer} interface which are now <em>extracted</em> into this view.</p>
 */
public interface WebAppWebContainerView extends PaxWebContainerView {

	/**
	 * Before a WAB can be deployed, we have to check if the target context is available. If it's not, the WAB
	 * will be put into "awaiting free context" state.
	 * @param bundle
	 * @param contextPath
	 * @return
	 */
	boolean allocateContext(Bundle bundle, String contextPath);

	/**
	 * <p>After a WAB is undeployed, the context path used by this bundle is again free for registration by Whiteboard
	 * or HttpService.</p>
	 *
	 * <p>TODO_WAB: However the question remains open about what to do with Whiteboard/HttpService web elements registered
	 *          after the WAB was DEPLOYED.</p>
	 * @param bundle
	 * @param contextPath
	 */
	void releaseContext(Bundle bundle, String contextPath);

	void sendBatch(Batch batch);

}
