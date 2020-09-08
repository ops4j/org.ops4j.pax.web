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

import java.util.Map;
import java.util.TreeSet;

import org.ops4j.pax.web.service.spi.model.elements.FilterModel;

public class FilterStateChange extends Change {

	/** Explicitly marked as mapping of {@link TreeSet} to highlight the importance of ordering */
	private final Map<String, TreeSet<FilterModel>> contextFilters;

	/**
	 * Whether the change is caused by dynamic filter registration. In such case, we should NEVER attempt
	 * to start the context after registering such filter (because most probably we're in the process of starting
	 * the context and in particular - we're calling {@link javax.servlet.ServletContainerInitializer#onStartup}).
	 */
	private boolean dynamic = false;

	public FilterStateChange(Map<String, TreeSet<FilterModel>> contextFilters, boolean dynamic) {
		super(OpCode.NONE);
		this.contextFilters = contextFilters;
		this.dynamic = dynamic;
	}

	public boolean isDynamic() {
		return dynamic;
	}

	public Map<String, TreeSet<FilterModel>> getContextFilters() {
		return contextFilters;
	}

	@Override
	public void accept(BatchVisitor visitor) {
		visitor.visit(this);
	}

}
