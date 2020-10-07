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

import java.util.Map;

import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.whiteboard.HttpContextMapping;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.HttpContext;

@Component(property = PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID + "=CustomHttpContextMapping")
public class PaxWebWhiteboardHttpContextMapping implements HttpContextMapping {

	public static final String HTTP_CONTEXT_ID = "CustomHttpContextMapping";

	@Activate
	public void start() {
		System.out.println(PaxWebWhiteboardHttpContextMapping.class.getName() + " started");
	}

	@Override
	public boolean isShared() {
		return false;
	}

	@Override
	public String getContextId() {
		return HTTP_CONTEXT_ID;
	}

	@Override
	public String getContextPath() {
		return "/context-mapping";
	}

	@Override
	public Map<String, String> getInitParameters() {
		return null;
	}

	@Override
	public String[] getVirtualHosts() {
		return new String[0];
	}

	@Override
	public HttpContext getHttpContext() {
		return null; //turns into DefaultHttpContext
	}

}
