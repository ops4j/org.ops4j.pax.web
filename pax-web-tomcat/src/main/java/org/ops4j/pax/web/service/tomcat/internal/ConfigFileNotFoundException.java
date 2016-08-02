/*
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
package org.ops4j.pax.web.service.tomcat.internal;

import java.io.File;

/**
 * @author Romaim Gilles
 */
public class ConfigFileNotFoundException extends RuntimeException {
	/**
	 *
	 */
	private static final long serialVersionUID = -5213267690789184307L;

	public ConfigFileNotFoundException(File configFile, Throwable cause) {
		super(String.format("cannot parse the configuration file: %s",
				configFile.getAbsolutePath()), cause);
	}
}
