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

import java.io.IOException;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ops4j.pax.web.service.PaxWebConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.HttpContext;

@Component(property = {
		PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID + "=" + PaxWebWhiteboardHttpContext.CONTEXT_ID,
		PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH + "=/custom-http-context"
})
@SuppressWarnings("deprecation")
public class PaxWebWhiteboardHttpContext implements HttpContext {

	public static final String CONTEXT_ID = "CustomHttpContext";

	private final Bundle bundle;

	@Activate
	public PaxWebWhiteboardHttpContext(BundleContext context) {
		this.bundle = context.getBundle();
	}

	@Override
	public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
		return true;
	}

	@Override
	public URL getResource(String name) {
		return bundle.getEntry(name);
	}

	@Override
	public String getMimeType(String name) {
		return null;
	}

}
