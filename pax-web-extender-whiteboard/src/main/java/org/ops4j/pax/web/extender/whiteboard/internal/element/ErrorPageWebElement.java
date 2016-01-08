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
 package org.ops4j.pax.web.extender.whiteboard.internal.element;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.whiteboard.ErrorPageMapping;
import org.ops4j.pax.web.extender.whiteboard.internal.util.WebContainerUtils;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * Registers/unregisters
 * {@link org.ops4j.pax.web.extender.whiteboard.ErrorPageMapping} with
 * {@link WebContainer}.
 * 
 * @author dsklyut
 * @since 0.7.0
 */
public class ErrorPageWebElement implements WebElement {

	private final ErrorPageMapping errorPageMapping;

	/**
	 * Constructor.
	 * 
	 * @param errorPageMapping
	 *            error page mapping; cannot be null
	 */
	public ErrorPageWebElement(final ErrorPageMapping errorPageMapping) {
		NullArgumentException.validateNotNull(errorPageMapping,
				"error page errorPageMapping");
		this.errorPageMapping = errorPageMapping;
	}

	/**
	 * registers error page
	 * 
	 * @param httpService
	 * @param httpContext
	 */
	public void register(HttpService httpService, HttpContext httpContext)
			throws Exception {
		if (WebContainerUtils.isWebContainer(httpService)) {
			((WebContainer) httpService).registerErrorPage(
					errorPageMapping.getError(),
					errorPageMapping.getLocation(), httpContext);
		} else {
			throw new UnsupportedOperationException(
					"Internal error: In use HttpService is not an WebContainer (from Pax Web)");
		}
	}

	/**
	 * unregisters error page
	 * 
	 * @param httpService
	 * @param httpContext
	 */
	public void unregister(HttpService httpService, HttpContext httpContext) {
		if (WebContainerUtils.isWebContainer(httpService)) {
			((WebContainer) httpService).unregisterErrorPage(
					errorPageMapping.getError(), httpContext);
		}
	}

	public String getHttpContextId() {
		return errorPageMapping.getHttpContextId();
	}

	@Override
	public String toString() {
		return new StringBuilder().append(this.getClass().getSimpleName())
				.append("{").append("mapping=").append(errorPageMapping)
				.append("}").toString();
	}
}
