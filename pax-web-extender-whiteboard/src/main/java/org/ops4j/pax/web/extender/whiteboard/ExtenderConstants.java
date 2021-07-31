/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.whiteboard;

/**
 * Defines standard names for pax web extender related registration properties.
 *
 * @author Alin Dreghiciu
 * @since 0.2.0, December 20, 2007
 */
// CHECKSTYLE:OFF
public interface ExtenderConstants {


	/**
	 * The http context virtual hosts property key.
	 */
	String PROPERTY_HTTP_VIRTUAL_HOSTS = "httpContext.virtualhosts";
	/**
	 * The http context connectors property key.
	 */
	String PROPERTY_HTTP_CONNECTORS = "httpContext.connectors";

}
