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
package org.ops4j.pax.web.service.spi.config;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;

public interface JspConfiguration {

	/**
	 * What scratch directory should we use when compiling JSP pages? Default is the work directory for
	 * the current web application.
	 * @param model scratch dir should depend on the {@link OsgiContextModel} where the JSP servlet is installed
	 * @return
	 */
	String getJspScratchDir(OsgiContextModel model);

	/**
	 * Returns root directory for scratch directories for all the web applications.
	 * @return
	 */
	String getGloablJspScratchDir();

}
