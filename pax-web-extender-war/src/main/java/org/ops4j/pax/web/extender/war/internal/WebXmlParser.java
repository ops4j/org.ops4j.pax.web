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
package org.ops4j.pax.web.extender.war.internal;

import java.io.InputStream;

import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.osgi.framework.Bundle;

/**
 * Web.xml Parser.
 * 
 * @author Alin Dreghiciu
 * @since 0.3.0, December 27, 2007
 */
public interface WebXmlParser {

	/**
	 * Parse the input stream (an web.xml) and returns the corresponding web app
	 * (root element).
	 * 
	 * @param bundle
	 * 
	 * @param inputStream
	 *            input stream over an web.xml
	 * 
	 * @return root web app elemet
	 */
	WebApp parse(Bundle bundle, InputStream inputStream);

}
