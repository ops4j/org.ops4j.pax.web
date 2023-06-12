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

import jakarta.servlet.ServletException;

import org.ops4j.pax.web.service.http.NamespaceException;

/**
 * A task that can be passed to {@link ServerModel#run(ModelRegistrationTask)} to ensure running it in
 * single threaded "configuration/registration" event loop.
 *
 * @param <T> task can run a value, which can also be of type {@link Void}.
 */
@FunctionalInterface
public interface ModelRegistrationTask<T> {

	/**
	 * <p>Run a task on behalf of {@link org.ops4j.pax.web.service.WebContainer} implementations, where
	 * we better ensure synchronous manipulation of internal Pax Web model and invocation of
	 * {@link org.ops4j.pax.web.service.spi.ServerController}</p>
	 *
	 * <p>When all such tasks are ensured to be run in single configuration thread, we can make the code much
	 * easier and avoid any locking/synchronization mechanism.</p>
	 *
	 * <p>Task can throw any API exception and will be handled correctly even across thread boundaries.</p>
	 *
	 * @return
	 * @throws ServletException
	 * @throws NamespaceException
	 * @throws IllegalArgumentException
	 * @throws IllegalStateException
	 */
	T run() throws ServletException, NamespaceException, IllegalArgumentException, IllegalStateException;

}
