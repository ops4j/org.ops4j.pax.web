/*
 * Copyright 2007 Damian Golda.
 * Copyright 2007 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.whiteboard.internal.element;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.whiteboard.JspMapping;
import org.ops4j.pax.web.extender.whiteboard.internal.util.DictionaryUtils;
import org.ops4j.pax.web.extender.whiteboard.internal.util.WebContainerUtils;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * Registers/unregisters {@link JspMapping} with {@link WebContainer}.
 * 
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class JspWebElement implements WebElement {

	/**
	 * Jsp mapping.
	 */
	private JspMapping jspMapping;

	/**
	 * Constructor.
	 * 
	 * @param jspMapping
	 *            JSP mapping; cannot be null
	 */
	public JspWebElement(final JspMapping jspMapping) {
		NullArgumentException.validateNotNull(jspMapping, "JSP mapping");
		this.jspMapping = jspMapping;
	}

	/**
	 * Registers jsps with web container.
	 */
	public void register(final HttpService httpService,
			final HttpContext httpContext) throws Exception {
		if (WebContainerUtils.isWebContainer(httpService)) {
			((WebContainer) httpService).registerJsps(
					jspMapping.getUrlPatterns(),
					DictionaryUtils.adapt(jspMapping.getInitParams()),
					httpContext);
		} else {
			throw new UnsupportedOperationException(
					"Internal error: In use HttpService is not an WebContainer (from Pax Web)");
		}
	}

	/**
	 * Unregisters jsps from web container.
	 */
	public void unregister(final HttpService httpService,
			final HttpContext httpContext) {
		if (WebContainerUtils.isWebContainer(httpService)) {
			((WebContainer) httpService).unregisterJsps(
					jspMapping.getUrlPatterns(), httpContext);
		} else {
			throw new UnsupportedOperationException(
					"Internal error: In use HttpService is not an WebContainer (from Pax Web)");
		}
	}

	public String getHttpContextId() {
		return jspMapping.getHttpContextId();
	}

	@Override
	public String toString() {
		return new StringBuffer().append(this.getClass().getSimpleName())
				.append("{").append("mapping=").append(jspMapping).append("}")
				.toString();
	}

}