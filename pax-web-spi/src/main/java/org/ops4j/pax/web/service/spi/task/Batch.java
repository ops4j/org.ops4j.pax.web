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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.model.ContextMetadataModel;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.model.elements.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.model.elements.WelcomeFileModel;
import org.osgi.service.http.HttpContext;

/**
 * <p>List of tasks to perform on model. Collects primitive operations which can be reverted if something
 * goes wrong, can be delayed if needed or shared between multiple invocations.</p>
 *
 * <p>The idea is that normal, elementary operation, like registration of a servlet may be associated with
 * model-altering operations. If something goes wrong, we'd like to <em>rollback</em> the changes. If everything
 * is fine, we can use given batch to:<ul>
 *     <li>actually apply the operations to global model</li>
 *     <li>pass the operations (e.g., create servlet context, create osgi context, register servlet) to
 *     {@link org.ops4j.pax.web.service.spi.ServerController}</li>
 *     <li>schedule for later invocation when using <em>transactions</em> with
 *     {@link org.ops4j.pax.web.service.WebContainer#begin(HttpContext)}</li>
 * </ul></p>
 *
 * <p>This class is implemented using the Visitor pattern, where the registration operations (servlet registration,
 * error page registration, context configuration, ...) are <em>elements</em> and the classes that should handle
 * registration operations (like global {@link ServerModel} or actual server runtime) implement related
 * <em>vistors</em>.</p>
 */
public class Batch {

	private final List<Change> operations = new LinkedList<>();
	private final String description;
	private String shortDescription;

	public Batch(String description) {
		this.description = description;
	}

	public List<Change> getOperations() {
		return operations;
	}

	public String getShortDescription() {
		return shortDescription;
	}

	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}

	/**
	 * Add new {@link ServletContextModel}
	 *
	 * @param model
	 * @param servletContextModel
	 */
	public void addServletContextModel(ServletContextModel servletContextModel) {
		operations.add(new ServletContextModelChange(OpCode.ADD, servletContextModel));
	}

	/**
	 * Add new {@link OsgiContextModel}
	 *
	 * @param osgiContextModel
	 */
	public void addOsgiContextModel(OsgiContextModel osgiContextModel, ServletContextModel servletContextModel) {
		operations.add(new OsgiContextModelChange(OpCode.ADD, null, osgiContextModel, servletContextModel));
	}

	/**
	 * Remove existing {@link OsgiContextModel}
	 *
	 * @param osgiContextModel
	 */
	public void removeOsgiContextModel(OsgiContextModel osgiContextModel) {
		operations.add(new OsgiContextModelChange(OpCode.DELETE, null, osgiContextModel, null));
	}

	/**
	 * Mark {@link OsgiContextModel} as associated with {@link WebContainerContext}
	 *
	 * @param context
	 * @param osgiContextModel
	 */
	public void associateOsgiContextModel(WebContainerContext context, OsgiContextModel osgiContextModel) {
		operations.add(new OsgiContextModelChange(OpCode.ASSOCIATE, context, osgiContextModel, null));
	}

	/**
	 * Mark {@link OsgiContextModel} as (temporarily) disassociated with {@link WebContainerContext}
	 *
	 * @param context
	 * @param osgiContextModel
	 */
	public void disassociateOsgiContextModel(WebContainerContext context, OsgiContextModel osgiContextModel) {
		operations.add(new OsgiContextModelChange(OpCode.DISASSOCIATE, context, osgiContextModel, null));
	}

	/**
	 * Add {@link ServletModel} to {@link ServerModel}
	 * @param model
	 */
	public void addServletModel(ServletModel model, OsgiContextModel ... newModels) {
		operations.add(new ServletModelChange(OpCode.ADD, model, newModels));
	}

	/**
	 * Remove {@link ServletModel} from {@link ServerModel}
	 * @param toUnregister
	 */
	public void removeServletModels(Map<ServletModel, Boolean> toUnregister) {
		operations.add(new ServletModelChange(OpCode.DELETE, toUnregister));
	}

	/**
	 * Add {@link ServletModel} to {@link ServerModel} but as <em>disabled</em> model, which can't be registered
	 * because other model is registered for the same URL pattern. Disabled models can later be registered of
	 * existing model with higher ranking is unregistered.
	 *
	 * @param model
	 */
	public void addDisabledServletModel(ServletModel model) {
		operations.add(new ServletModelChange(OpCode.ADD, model, true));
	}

	/**
	 * Enable {@link ServletModel}
	 *
	 * @param model
	 */
	public void enableServletModel(ServletModel model) {
		operations.add(new ServletModelChange(OpCode.ENABLE, model));
	}

	/**
	 * Disable {@link ServletModel} from {@link ServerModel}
	 *
	 * @param model
	 */
	public void disableServletModel(ServletModel model) {
		operations.add(new ServletModelChange(OpCode.DISABLE, model));
	}

	/**
	 * Add {@link FilterModel} to {@link ServerModel}
	 * @param model
	 */
	public void addFilterModel(FilterModel model, OsgiContextModel ... newModels) {
		operations.add(new FilterModelChange(OpCode.ADD, model, newModels));
	}

	/**
	 * Remove {@link ServletModel} from {@link ServerModel}
	 * @param toUnregister
	 */
	public void removeFilterModels(List<FilterModel> toUnregister) {
		operations.add(new FilterModelChange(OpCode.DELETE, toUnregister));
	}

	/**
	 * Add {@link FilterModel} to {@link ServerModel} but as <em>disabled</em> model
	 *
	 * @param model
	 */
	public void addDisabledFilterModel(FilterModel model) {
		operations.add(new FilterModelChange(OpCode.ADD, model, true));
	}

	/**
	 * Enable {@link FilterModel}
	 *
	 * @param model
	 */
	public void enableFilterModel(FilterModel model) {
		operations.add(new FilterModelChange(OpCode.ENABLE, model));
	}

	/**
	 * Disable {@link FilterModel} from {@link ServerModel}
	 *
	 * @param model
	 */
	public void disableFilterModel(FilterModel model) {
		operations.add(new FilterModelChange(OpCode.DISABLE, model));
	}

	/**
	 * Batch (inside batch...) method that passes full information about all filters that should be enabled
	 * in a set of contexts. To be handled by Server Controller only
	 *
	 * @param contextFilters
	 * @param dynamic should be set to {@code true} when adding a {@link FilterModel} related to
	 *        {@link javax.servlet.ServletContext#addFilter}
	 */
	public void updateFilters(Map<String, TreeMap<FilterModel, List<OsgiContextModel>>> contextFilters, boolean dynamic) {
		operations.add(new FilterStateChange(contextFilters, dynamic));
	}

	/**
	 * Add {@link ErrorPageModel} to {@link ServerModel}
	 * @param model
	 */
	public void addErrorPageModel(ErrorPageModel model, OsgiContextModel ... newModels) {
		operations.add(new ErrorPageModelChange(OpCode.ADD, model, newModels));
	}

	/**
	 * Remove {@link ErrorPageModel} from {@link ServerModel}
	 * @param toUnregister
	 */
	public void removeErrorPageModels(List<ErrorPageModel> toUnregister) {
		operations.add(new ErrorPageModelChange(OpCode.DELETE, toUnregister));
	}

	/**
	 * Add {@link ErrorPageModel} to {@link ServerModel} but as <em>disabled</em> model
	 *
	 * @param model
	 */
	public void addDisabledErrorPageModel(ErrorPageModel model) {
		operations.add(new ErrorPageModelChange(OpCode.ADD, model, true));
	}

	/**
	 * Enable {@link ErrorPageModel}
	 *
	 * @param model
	 */
	public void enableErrorPageModel(ErrorPageModel model) {
		operations.add(new ErrorPageModelChange(OpCode.ENABLE, model));
	}

	/**
	 * Disable {@link ErrorPageModel} in {@link ServerModel}
	 *
	 * @param model
	 */
	public void disableErrorPageModel(ErrorPageModel model) {
		operations.add(new ErrorPageModelChange(OpCode.DISABLE, model));
	}

	/**
	 * Batch (inside batch...) method that passes full information about all error page models
	 * that should be enabled in a set of contexts. To be handled by Server Controller only
	 *
	 * @param contextErrorPageModels
	 */
	public void updateErrorPages(Map<String, TreeMap<ErrorPageModel, List<OsgiContextModel>>> contextErrorPageModels) {
		operations.add(new ErrorPageStateChange(contextErrorPageModels));
	}

	/**
	 * Add new {@link EventListenerModel}
	 * @param model
	 */
	public void addEventListenerModel(EventListenerModel model,
				OsgiContextModel ... newModels) {
		operations.add(new EventListenerModelChange(OpCode.ADD, model, newModels));
	}

	/**
	 * Remove existing {@link EventListenerModel}
	 * @param models
	 */
	public void removeEventListenerModels(List<EventListenerModel> models) {
		operations.add(new EventListenerModelChange(OpCode.DELETE, models));
	}

	/**
	 * Add new {@link ContainerInitializerModel}
	 * @param model
	 */
	public void addContainerInitializerModel(ContainerInitializerModel model,
			OsgiContextModel ... newModels) {
		operations.add(new ContainerInitializerModelChange(OpCode.ADD, model, newModels));
	}

	/**
	 * Remove existing {@link ContainerInitializerModel}
	 * @param models
	 */
	public void removeContainerInitializerModels(List<ContainerInitializerModel> models) {
		operations.add(new ContainerInitializerModelChange(OpCode.DELETE, models));
	}

	/**
	 * Add new {@link WelcomeFileModel}
	 * @param serverModel
	 * @param model
	 */
	public void addWelcomeFileModel(WelcomeFileModel model, OsgiContextModel ... newModels) {
		operations.add(new WelcomeFileModelChange(OpCode.ADD, model, newModels));
	}

	/**
	 * Remove {@link WelcomeFileModel}
	 * @param serverModel
	 * @param model
	 */
	public void removeWelcomeFileModel(WelcomeFileModel model) {
		operations.add(new WelcomeFileModelChange(OpCode.DELETE, model));
	}

	/**
	 * Mark a {@link Batch} as <em>transactional</em>, so accepting visitors can be aware that more changes are
	 * coming. For example, a WAB/WAR configures a set of servlet/filter/... registrations and it's wise to start
	 * the context only at the end of the batch.
	 *
	 * @param contextPath transactions are always (for now?) related to single context path.
	 */
	public void beginTransaction(String contextPath) {
		operations.add(new TransactionStateChange(OpCode.ASSOCIATE, contextPath));
	}

	/**
	 * Complete the transaction, so for example a {@link org.ops4j.pax.web.service.spi.ServerController} knows to
	 * finally start the context.
	 *
	 * @param contextPath
	 */
	public void commitTransaction(String contextPath) {
		operations.add(new TransactionStateChange(OpCode.DISASSOCIATE, contextPath));
	}

	/**
	 * Configure some meta information related to a context.
	 * @param meta
	 * @param ocm
	 */
	public void configureMetadata(ContextMetadataModel meta, OsgiContextModel ocm) {
		operations.add(new ContextMetadataModelChange(OpCode.ADD, meta, ocm));
	}

	/**
	 * Configure extension - MIME mapping.
	 * @param mimeMapping
	 * @param localeEncodingMapping
	 * @param ocm
	 */
	public void configureMimeAndEncodingMappings(Map<String, String> mimeMapping, Map<String, String> localeEncodingMapping, OsgiContextModel ocm) {
		operations.add(new MimeAndLocaleMappingChange(OpCode.ADD, mimeMapping, localeEncodingMapping, ocm));
	}

	/**
	 * Assuming everything is ok, this method simply invokes all the collected operations which will:<ul>
	 *     <li>alter global {@link ServerModel} sequentially.</li>
	 *     <li>alter actual server runtime</li>
	 *     <li>...</li>
	 * </ul>
	 */
	public void accept(BatchVisitor visitor) {
		for (Change op : operations) {
			op.accept(visitor);
		}
	}

	/**
	 * A {@link Batch} can create the <em>uninstallation batch</em> consisting of all the operations reversed.
	 * @return
	 */
	public Batch uninstall(String description) {
		Batch b = new Batch(description);

		List<Change> reversed = new ArrayList<>(operations);
		Collections.reverse(reversed);
		for (Change c : reversed) {
			c.uninstall(b.getOperations());
		}

		return b;
	}

	@Override
	public String toString() {
		return "Batch{\"" + description + "\"}";
	}

}
