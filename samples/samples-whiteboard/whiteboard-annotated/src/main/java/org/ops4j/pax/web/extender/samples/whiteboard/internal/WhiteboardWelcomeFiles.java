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
package org.ops4j.pax.web.extender.samples.whiteboard.internal;

import org.ops4j.pax.web.service.whiteboard.WelcomeFileMapping;
import org.osgi.service.component.annotations.Component;

@Component
public class WhiteboardWelcomeFiles implements WelcomeFileMapping {

	@Override
	public String getContextSelectFilter() {
		// null means "default"
		return null;
	}

	@Override
	public String getContextId() {
		return null;
	}

	@Override
	public boolean isRedirect() {
		return false;
	}

	@Override
	public String[] getWelcomeFiles() {
		return new String[] { "index.html" };
	}

}
