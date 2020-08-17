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

/**
 * <p>Model subpackage representing actual <em>web elements</em>, while <em>infrastructure elements</em>
 * (like contexts) are kept in the package above.</p>
 *
 * <p><em>Web elements</em> may be divided into two groups:<ul>
 *     <li><em>active web elements</em> (the ones that can handle requests): servlets, filters, resources, jsps
 *     and websockets (I guess).</li>
 *     <li><em>passive web elements</em> (the ones that play support role): connectors/virtual hosts, constraint
 *     mappings, context params, error pages, event listeners, jsp configurations, login configs, servlet container
 *     initializers, session cookie configs, session timeouts and welcome files.</li>
 * </ul>
 * This division is simple - only registration of <em>active</em> elements will ensure that the physical context
 * is actually started. This is important when user wants to register configurations and e.g., context listeners.</p>
 *
 * <p>Welcome files may be confusing, but these (themselves) do NOT serve any content - these are used by resources
 * (resource servlets) to adjust their behavior.</p>
 */
package org.ops4j.pax.web.service.spi.model.elements;
