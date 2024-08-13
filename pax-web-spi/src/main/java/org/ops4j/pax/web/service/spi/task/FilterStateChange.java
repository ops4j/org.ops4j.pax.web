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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.DispatcherType;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;

public class FilterStateChange extends Change {

	/** Explicitly marked as mapping of {@link TreeMap} to highlight the importance of ordering */
	private final Map<String, TreeMap<FilterModel, List<OsgiContextModel>>> contextFilters;

	/**
	 * Whether the change is caused by dynamic filter registration. In such case, we should NEVER attempt
	 * to start the context after registering such filter (because most probably we're in the process of starting
	 * the context and in particular - we're calling {@link javax.servlet.ServletContainerInitializer#onStartup}).
	 */
	private boolean dynamic = false;

	private boolean useWebOrder = false;

	public FilterStateChange(Map<String, TreeMap<FilterModel, List<OsgiContextModel>>> contextFilters, boolean dynamic) {
		super(OpCode.NONE);
		this.contextFilters = contextFilters;
		this.dynamic = dynamic;

		// fix dispatcher types
		for (TreeMap<FilterModel, List<OsgiContextModel>> map : contextFilters.values()) {
			for (FilterModel fm : map.keySet()) {
				if (!fm.isDynamic()) {
					for (FilterModel.Mapping mapping : fm.getMappingsPerDispatcherTypes()) {
						if (mapping.getDispatcherTypes() == null || mapping.getDispatcherTypes().length == 0) {
							// Servlet 4 Spec: 6.2.5 Filters and the RequestDispatcher
							mapping.setDispatcherTypes(new DispatcherType[] {
									DispatcherType.REQUEST
							});
						}
					}
				}
			}
		}
	}

	public boolean isDynamic() {
		return dynamic;
	}

	public Map<String, TreeMap<FilterModel, List<OsgiContextModel>>> getContextFilters() {
		return contextFilters;
	}

	public boolean useWebOrder() {
		return useWebOrder;
	}

	/**
	 * Set a flag, so when filters are added to the context for particular runtime, they are added
	 * in {@code web.xml} order instead of OSGi Whiteboard order (by rank)
	 * @param useWebOrder
	 */
	public void setUseWebOrder(boolean useWebOrder) {
		this.useWebOrder = useWebOrder;
	}

	@Override
	public void uninstall(List<Change> operations) {
		if (getKind() == OpCode.NONE) {
			Map<String, TreeMap<FilterModel, List<OsgiContextModel>>> newMap = new HashMap<>();
			for (String context : contextFilters.keySet()) {
				newMap.put(context, new TreeMap<>());
			}

			operations.add(new FilterStateChange(newMap, dynamic));
		}
	}

	@Override
	public void accept(BatchVisitor visitor) {
		visitor.visitFilterStateChange(this);
	}

}
