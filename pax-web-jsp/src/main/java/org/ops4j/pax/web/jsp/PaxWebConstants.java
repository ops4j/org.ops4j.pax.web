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

	/**
	 * Scratch directory for JSPs
	 */
	String PROPERTY_JSP_SCRATCH_DIR = PID + ".jsp.scratch.dir";

	String PROPERTY_JSP_CHECK_INTERVAL = PID + ".jsp.check.interval";

	String PROPERTY_JSP_DEBUG_INFO = PID + ".jsp.debug.info";

	String PROPERTY_JSP_DEVELOPMENT = PID + ".jsp.development";

	String PROPERTY_JSP_ENABLE_POOLING = PID + ".jsp.enable.pooling";

	String PROPERTY_JSP_IE_CLASS_ID = PID + ".jsp.ie.classid";

	String PROPERTY_JSP_JAVA_ENCODING = PID + ".jsp.java.encoding";

	String PROPERTY_JSP_KEEP_GENERATED = PID + ".jsp.keep.generated";

	String PROPERTY_JSP_LOG_VERBOSITY_LEVEL = PID + ".jsp.log.verbosity.level";

	String PROPERTY_JSP_MAPPED_FILE = PID + ".jsp.mapped.file";

	String PROPERTY_JSP_TAGPOOL_MAX_SIZE = PID + ".jsp.tagpool.max.size";

	String PROPERTY_JSP_PRECOMPILATION = PID + ".jsp.precompilation";

}
