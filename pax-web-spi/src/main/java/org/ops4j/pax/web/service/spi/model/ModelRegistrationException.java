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
package org.ops4j.pax.web.service.spi.model;

import javax.servlet.ServletException;

import org.osgi.service.http.NamespaceException;

/**
 * Special {@link RuntimeException} to handle checked exceptions thrown from {@link ModelRegistrationTask}
 * that can be run in {@link java.util.concurrent.CompletableFuture}.
 */
public class ModelRegistrationException extends RuntimeException {

	private ServletException servletException;
	private NamespaceException namespaceException;

	public ModelRegistrationException() {
	}

	public ModelRegistrationException(String message) {
		super(message);
	}

	public ModelRegistrationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ModelRegistrationException(ServletException cause) {
		super(cause);
		this.servletException = cause;
	}

	public ModelRegistrationException(NamespaceException cause) {
		super(cause);
		this.namespaceException = cause;
	}

	public ModelRegistrationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * Method called from the code that runs {@link ModelRegistrationTask} using
	 * {@link java.util.concurrent.CompletableFuture} when {@link java.util.concurrent.ExecutionException} is caught.
	 *
	 * @throws ServletException
	 * @throws NamespaceException
	 */
	public void throwTheCause() throws ServletException, NamespaceException {
		if (servletException != null) {
			throw servletException;
		}
		if (namespaceException != null) {
			throw namespaceException;
		}
		throw this;
	}

}
