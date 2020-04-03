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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.ServiceModel;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
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

	public Batch(String description) {
		this.description = description;
	}

	public List<Change> getOperations() {
		return operations;
	}

	/**
	 * Add new {@link ServletContextModel}
	 *
	 * @param model
	 * @param servletContextModel
	 */
	public void addServletContextModel(ServerModel model, ServletContextModel servletContextModel) {
		operations.add(new ServletContextModelChange(OpCode.ADD, model, servletContextModel));
	}

	/**
	 * Add new {@link OsgiContextModel}
	 *
	 * @param osgiContextModel
	 */
	public void addOsgiContextModel(OsgiContextModel osgiContextModel) {
		operations.add(new OsgiContextModelChange(OpCode.ADD, null, osgiContextModel));
	}

	/**
	 * Mark {@link OsgiContextModel} as associated with {@link WebContainerContext}
	 *
	 * @param context
	 * @param osgiContextModel
	 */
	public void associateOsgiContextModel(WebContainerContext context, OsgiContextModel osgiContextModel) {
		operations.add(new OsgiContextModelChange(OpCode.ASSOCIATE, context, osgiContextModel));
	}

	/**
	 * Remove {@link ServletModel servlet models} from {@link ServiceModel}
	 * @param serviceModel
	 * @param toUnregister
	 */
	public void removeServletModels(ServiceModel serviceModel, List<ServletModel> toUnregister) {
		operations.add(new ServletModelChange(OpCode.DELETE, serviceModel, toUnregister));
	}

	/**
	 * Add {@link ServletModel} to {@link ServerModel}
	 * @param serverModel
	 * @param model
	 */
	public void addServletModel(ServerModel serverModel, ServletModel model) {
		operations.add(new ServletModelChange(OpCode.ADD, serverModel, model));
	}

	/**
	 * Remove {@link ServletModel} from {@link ServerModel}
	 * @param serverModel
	 * @param model
	 */
	public void removeServletModels(ServerModel serverModel, List<ServletModel> toUnregister) {
		operations.add(new ServletModelChange(OpCode.DELETE, serverModel, toUnregister));
	}

	/**
	 * Add {@link FilterModel} to {@link ServiceModel}
	 * @param serviceModel
	 * @param model
	 */
	public void addFilterModel(ServiceModel serviceModel, FilterModel model) {
		operations.add(new FilterModelChange(OpCode.ADD, serviceModel, model));
	}

	/**
	 * Add {@link FilterModel} to {@link ServerModel}
	 * @param serverModel
	 * @param model
	 */
	public void addFilterModel(ServerModel serverModel, FilterModel model) {
		operations.add(new FilterModelChange(OpCode.ADD, serverModel, model));
	}

	/**
	 * Add {@link ServletModel} to {@link ServerModel} but as <em>disabled</em> model, which can't be registered
	 * because other model is registered for the same URL pattern. Disabled models can later be registered of
	 * existing model with higher ranking is unregistered.
	 *
	 * @param serverModel
	 * @param model
	 */
	public void addDisabledServletModel(ServerModel serverModel, ServletModel model) {
		operations.add(new ServletModelChange(OpCode.ADD, serverModel, model, true));
	}

	/**
	 * Add {@link FilterModel} to {@link FilterModel} but as <em>disabled</em> model
	 *
	 * @param filterModel
	 * @param model
	 */
	public void addDisabledFilterModel(ServerModel serverModel, FilterModel model) {
		operations.add(new FilterModelChange(OpCode.ADD, serverModel, model, true));
	}

	/**
	 * Disable {@link ServletModel} from {@link ServerModel}
	 *
	 * @param serverModel
	 * @param model
	 */
	public void disableServletModel(ServerModel serverModel, ServletModel model) {
		operations.add(new ServletModelChange(OpCode.DISABLE, serverModel, model));
	}

	/**
	 * Enable {@link ServletModel}
	 *
	 * @param serverModel
	 * @param model
	 */
	public void enableServletModel(ServerModel serverModel, ServletModel model) {
		operations.add(new ServletModelChange(OpCode.ENABLE, serverModel, model));
	}

	/**
	 * Disable {@link FilterModel} from {@link ServerModel}
	 *
	 * @param serverModel
	 * @param model
	 */
	public void disableFilterModel(ServerModel serverModel, FilterModel model) {
		operations.add(new FilterModelChange(OpCode.DISABLE, serverModel, model));
	}

	/**
	 * Enable {@link FilterModel}
	 *
	 * @param serverModel
	 * @param model
	 */
	public void enableFilterModel(ServerModel serverModel, FilterModel model) {
		operations.add(new FilterModelChange(OpCode.ENABLE, serverModel, model));
	}

	/**
	 * Batch (inside batch...) method that passes full information about all filters that should be enabled
	 * in a set of contexts. To be handled by Server Controller only
	 *
	 * @param contextFilters
	 */
	public void updateFilters(Map<String, Set<FilterModel>> contextFilters) {
		operations.add(new FilterStateChange(contextFilters));
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

	@Override
	public String toString() {
		return "Batch{\"" + description + "\"}";
	}

}
