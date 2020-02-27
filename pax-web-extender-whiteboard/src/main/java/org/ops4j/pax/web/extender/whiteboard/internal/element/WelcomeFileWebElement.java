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
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.whiteboard.WelcomeFileMapping;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWelcomeFile;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;

/**
 * Registers/unregisters {@link WelcomeFileMapping} with {@link WebContainer}.
 *
 * @author dsklyut
 * @since 0.7.0
 */
public class WelcomeFileWebElement extends WebElement<WelcomeFileMapping> implements WhiteboardWelcomeFile {

	private final WelcomeFileMapping welcomeFileMapping;

	/**
	 * Constructs a new WelcomeFileWebElement
	 * @param ref the service-reference behind the registered http-whiteboard-service
	 * @param welcomeFileMapping WelcomeFileMapping containing all necessary information
	 */
	public WelcomeFileWebElement(ServiceReference<WelcomeFileMapping> ref, WelcomeFileMapping welcomeFileMapping) {
		super(ref);
		NullArgumentException.validateNotNull(welcomeFileMapping, "Welcome file mapping");
		this.welcomeFileMapping = welcomeFileMapping;
	}

	@Override
	public void register(WebContainer webContainer, HttpContext httpContext)
			throws Exception {
//		webContainer.registerWelcomeFiles(
//				welcomeFileMapping.getWelcomeFiles(),
//				welcomeFileMapping.isRedirect(), httpContext);
	}

	@Override
	public void unregister(WebContainer webContainer, HttpContext httpContext) {
//		webContainer.unregisterWelcomeFiles(welcomeFileMapping.getWelcomeFiles(), httpContext);
	}

	@Override
	public String getHttpContextId() {
		return welcomeFileMapping.getHttpContextId();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "{mapping=" + welcomeFileMapping +	"}";
	}

	@Override
	public WelcomeFileMapping getWelcomeFilemapping() {
		return welcomeFileMapping;
	}
}
