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
package org.ops4j.pax.web.service.whiteboard;

import org.osgi.service.http.context.ServletContextHelper;

/**
 * <p><em>ServletContextHelper mapping</em> collects all the information required to register a
 * {@link ServletContextHelperMapping} to allow referencing it later.</p>
 *
 * <p>Registering a {@link ServletContextHelper} can be done in two ways:<li>
 *     <li>registering a service with this interface - all the information is included in service itself
 *     (<em>explicit whiteboard approach</em>)</li>
 *     <li>registering a {@link ServletContextHelper} service, while required properties (mapping,
 *     name, parameters) are specified using service registration properties/annotations
 *     (OSGi CMPN Whiteboard approach)</li>
 * </li></p>
 *
 * <p>This interface doesn't extend {@link ContextRelated}, because it represents the <em>context</em> itself.</p>
 */
public interface ServletContextHelperMapping extends ContextMapping {

	/**
	 * <p>Get actual context help being registered. If specified, this is the way to provide the <em>behavioral</em>
	 * aspects of the <em>context</em> (e.g., {@link ServletContextHelper#handleSecurity}). If not specified, default
	 * {@link ServletContextHelper} will be created according to {@link ContextMapping#getContextId()}.</p>
	 *
	 * @return
	 */
	ServletContextHelper getServletContextHelper();

}
