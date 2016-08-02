/* Copyright 2016 Marc Schlegel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.resources.api.query;

import java.util.Optional;

import org.ops4j.pax.web.resources.api.OsgiResourceLocator;

/**
 * Implementations can be passed to
 * {@link OsgiResourceLocator#findResources(ResourceQueryMatcher)} in order to
 * match against advanced lookup-strategies
 */
public interface ResourceQueryMatcher {

	/**
	 * The {@link OsgiResourceLocator}-implementation will call this method for
	 * every resource it manages.
	 *
	 * @param resourcePath the path of a resource managed by a {@link OsgiResourceLocator}.
	 *                     The argument is never null nor empty.
	 * @return Implementation of {@link ResourceQueryResult} wrapped in an Optional if
	 * path matched the query, otherwise an empty Optional.
	 */
	<R extends ResourceQueryResult> Optional<R> matches(String resourcePath);

}
