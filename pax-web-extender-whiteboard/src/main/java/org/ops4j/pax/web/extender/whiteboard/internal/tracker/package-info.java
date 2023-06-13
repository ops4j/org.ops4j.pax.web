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
 * <p>This package contains trackers (or rather implementations of {@link org.osgi.util.tracker.ServiceTrackerCustomizer})
 * to translate <em>incoming</em> OSGi services into Pax Web internal representations. The internal representation
 * is used to construct web applications out of separate pieces.</p>
 *
 * <p>Remember that e.g., a {@link jakarta.servlet.Servlet} may be registered by user in two ways:<ul>
 *     <li>By registering {@link jakarta.servlet.Servlet} OSGi service with service properties/annotations</li>
 *     <li>By registering {@link jakarta.servlet.http.HttpServlet} OSGi service (which is usually the same as above,
 *     but could be missed if tracking only {@link jakarta.servlet.Servlet servlets}).</li>
 *     <li>By registering {@link org.ops4j.pax.web.service.whiteboard.ServletMapping}, where all the configuration
 *     is available inside the registered service itself.</li>
 * </ul>
 * This means that for each <em>web element</em> we sometimes needs more than one tracker, because of more than
 * one <em>incoming</em> interface for e.g., a <em>servlet</em>.</p>
 *
 * <p>To make things clear, this package contains trackers/customizers for <em>official</em> interfaces (like
 * {@link jakarta.servlet.Servlet}), while {@code legacy} subpackage contains trackers/customizers for Pax Web
 * specific interfaces (like {@link org.ops4j.pax.web.service.whiteboard.FilterMapping}).</p>
 *
 * <p>Each "tracker" should have static method that actually creates an instance of
 * {@link org.osgi.util.tracker.ServiceTracker} with correct customizer.</p>
 */
package org.ops4j.pax.web.extender.whiteboard.internal.tracker;
