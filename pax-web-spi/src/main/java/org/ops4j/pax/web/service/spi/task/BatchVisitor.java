/*
 * Copyright 2020 OPS4J.
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
package org.ops4j.pax.web.service.spi.task;

/**
 * <p>Single interface that aims to replace all other interfaces with methods like {@code registerServlet},
 * {@code unregisterServlet}, ...</p>
 *
 * <p>In Pax Web 8, we've tried to decrease duplication of such interfaces (similar methods were available in
 * interfaces representing {@link org.osgi.service.http.HttpService},
 * {@link org.ops4j.pax.web.service.spi.ServerController}, server abstraction, server state, ... Now, if any class
 * wants to handle actual (un/re)registrations, it just needs to implement the visitor and correctly
 * configured {@link Batch} can accept such visitor.</p>
 */
public interface BatchVisitor {

	/**
	 * Processing the most high-level change - related to
	 * {@link org.ops4j.pax.web.service.spi.model.ServletContextModel}, which creates (or destroys) entire
	 * <em>context</em> (1:1 with "context path").
	 *
	 * @param change
	 */
	void visitServletContextModelChange(ServletContextModelChange change);

	/**
	 * Processing a change related to {@link org.ops4j.pax.web.service.spi.model.OsgiContextModel} - a model
	 * that affects one {@link org.ops4j.pax.web.service.spi.model.ServletContextModel} thought the relation is
	 * not 1:1, but N:1 - many OsgiContextModels may be associated with single ServletContextModel.
	 *
	 * @param change
	 */
	void visitOsgiContextModelChange(OsgiContextModelChange change);

	/**
	 * Processing a change related to {@link org.ops4j.pax.web.service.spi.model.elements.ServletModel}. Related
	 * to registration or unregistration of servlets.
	 * @param change
	 */
	void visitServletModelChange(ServletModelChange change);

	/**
	 * Processing a change related to {@link org.ops4j.pax.web.service.spi.model.elements.FilterModel}. Related
	 * to registration or unregistration of filter.
	 * @param change
	 */
	void visitFilterModelChange(FilterModelChange change);

	/**
	 * Processing full change of registered filters for all the affected contexts. It's necessary for filters,
	 * because registration of single filter may require reorganization (ordering) of currently registered filters.
	 * @param change
	 */
	void visitFilterStateChange(FilterStateChange change);

	/**
	 * Processing a change related to {@link org.ops4j.pax.web.service.spi.model.elements.EventListenerModel}
	 * @param change
	 */
	void visitEventListenerModelChange(EventListenerModelChange change);

	/**
	 * Process a change related to <em>welcome files</em>.
	 * @param change
	 */
	void visitWelcomeFileModelChange(WelcomeFileModelChange change);

	/**
	 * Process a change related to error pages
	 * @param change
	 */
	void visitErrorPageModelChange(ErrorPageModelChange change);

	/**
	 * Process full change of the state of error pages per context
	 * @param change
	 */
	void visitErrorPageStateChange(ErrorPageStateChange change);

	/**
	 * Process a change related to {@link org.ops4j.pax.web.service.spi.model.elements.ContainerInitializerModel}
	 * @param change
	 */
	void visitContainerInitializerModelChange(ContainerInitializerModelChange change);

	/**
	 * Process a change related to {@link org.ops4j.pax.web.service.spi.model.elements.WebSocketModel}
	 * @param change
	 */
	void visitWebSocketModelChange(WebSocketModelChange change);

	/**
	 * Process a change related to transactions for a context (path). There's no need to implement this method
	 * in a {@link BatchVisitor}.
	 * @param change
	 */
	default void visitTransactionStateChange(TransactionStateChange change) {
	}

	/**
	 * Process a change related to metadata of the <em>context</em>. No need to "uninstall" such change.
	 * @param change
	 */
	default void visitContextMetadataModelChange(ContextMetadataModelChange change) {
	}

	/**
	 * Process a change related to full MIME mapping for the context. No add/remove/modify operations - just full set.
	 * @param change
	 */
	default void visitMimeAndLocaleMappingChange(MimeAndLocaleMappingChange change) {
	}

	/**
	 * Process a change related to reset of dynamic registrations (like {@link javax.servlet.ServletContext#addServlet}
	 * @param change
	 */
	void visitClearDynamicRegistrationsChange(ClearDynamicRegistrationsChange change);

	/**
	 * Process a change related to starting a context - action created on the visitor side to schedule start
	 * of the context in different thread/event tick.
	 * @param contextStartChange
	 */
	default void visitContextStartChange(ContextStartChange contextStartChange) {
	}

}
