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

	public FilterStateChange(Map<String, TreeSet<FilterModel>> contextFilters) {
		super(OpCode.NONE);
		this.contextFilters = contextFilters;
	}

	public Map<String, TreeSet<FilterModel>> getContextFilters() {
		return contextFilters;
	}

	@Override
	public void accept(BatchVisitor visitor) {
		visitor.visit(this);
	}

}
