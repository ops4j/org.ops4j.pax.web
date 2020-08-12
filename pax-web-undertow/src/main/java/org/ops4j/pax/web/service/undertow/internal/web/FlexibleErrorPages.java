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
package org.ops4j.pax.web.service.undertow.internal.web;

import java.util.HashMap;
import java.util.Map;

import io.undertow.servlet.core.ErrorPages;

/**
 * We have to be able to get previous error pages because again, Undertow is not flexible enough.
 */
public class FlexibleErrorPages extends ErrorPages {

	private final Map<Integer, String> errorCodeLocations;
	private final Map<Class<? extends Throwable>, String> exceptionMappings;

	public FlexibleErrorPages() {
		this(new HashMap<>(), new HashMap<>(), null);
	}

	public FlexibleErrorPages(Map<Integer, String> errorCodeLocations, Map<Class<? extends Throwable>, String> exceptionMappings, String defaultErrorPage) {
		super(errorCodeLocations, exceptionMappings, defaultErrorPage);
		this.errorCodeLocations = errorCodeLocations;
		this.exceptionMappings = exceptionMappings;
	}

	public Map<Integer, String> getErrorCodeLocations() {
		return errorCodeLocations;
	}

	public Map<Class<? extends Throwable>, String> getExceptionMappings() {
		return exceptionMappings;
	}

}
