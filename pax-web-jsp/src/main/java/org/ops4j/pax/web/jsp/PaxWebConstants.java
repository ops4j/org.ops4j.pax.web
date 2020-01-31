/* Copyright 2008 Achim Nierbeck.
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
package org.ops4j.pax.web.jsp;

/**
 * <p>Different constants used across Pax Web.</p>
 * <p>Constants names use the following prefixes:<ul>
 *     <li>{@code WEB_CFG_} - for system or context property names</li>
 *     <li>{@code PROPERTY_JSP_} - for property names found in {@code org.ops4j.pax.web} PID</li>
 *     <li>{@code SERVICE_PROPERTY_} - for names of OSGi service properties</li>
 * </ul></p>
 */
public interface PaxWebConstants {

	/**
	 * The Managed Service PID for web configuration.
	 */
	String PID = "org.ops4j.pax.web";

}
