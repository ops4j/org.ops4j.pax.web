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
package org.ops4j.pax.web.samples.whiteboard.ds.extended;

import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.whiteboard.WelcomeFileMapping;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

@Component
public class PaxWebWhiteboardWelcomeFiles implements WelcomeFileMapping {

	@Activate
	public void start() {
		System.out.println("WelcomeFiles registered");
	}

	@Deactivate
	public void stop() {
		System.out.println("WelcomeFiles stopped");
	}

	@Override
	@SuppressWarnings("deprecation")
	public String getContextSelectFilter() {
		// only 3 out of 4 contexts
		return String.format("(|(%s=%s)(%s=%s)(%s=%s))",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "default",
				PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "CustomHttpContext",
				PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "CustomHttpContextMapping"
		);
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
