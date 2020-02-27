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
import org.ops4j.pax.web.extender.whiteboard.internal.util.DictionaryUtils;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.whiteboard.JspMapping;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardJspMapping;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;

/**
 * Registers/unregisters {@link JspMapping} with {@link WebContainer}.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class JspWebElement extends WebElement<JspMapping> implements WhiteboardJspMapping {

	private JspMapping jspMapping;

	/**
	 * Constructs a new JspWebElement
	 * @param ref the service-reference behind the registered http-whiteboard-service
	 * @param jspMapping JspMapping containing all necessary information
	 */
	public JspWebElement(ServiceReference<JspMapping> ref, final JspMapping jspMapping) {
		super(ref);
		NullArgumentException.validateNotNull(jspMapping, "JSP mapping");
		this.jspMapping = jspMapping;
	}

	@Override
	public void register(final WebContainer webContainer, final HttpContext httpContext) throws Exception {
//		webContainer.registerJsps(
//					jspMapping.getUrlPatterns(),
//					DictionaryUtils.adapt(jspMapping.getInitParams()),
//					httpContext);
	}

	@Override
	public void unregister(final WebContainer webContainer, final HttpContext httpContext) {
//			webContainer.unregisterJsps(
//					jspMapping.getUrlPatterns(), httpContext);
	}

	@Override
	public String getHttpContextId() {
		return jspMapping.getHttpContextId();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "{mapping=" + jspMapping + "}";
	}

	@Override
	public JspMapping getJspMapping() {
		return jspMapping;
	}
}