/*
 * Copyright 2022 OPS4J.
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
package org.ops4j.pax.web.service.jetty.internal;

public class PriorityValue<T> {

	private final T value;
	private final Integer priority;

	PriorityValue(T value, Integer priority) {
		this.value = value;
		this.priority = priority;
	}

	public T getValue() {
		return value;
	}

	public Integer getPriority() {
		return priority;
	}

}
