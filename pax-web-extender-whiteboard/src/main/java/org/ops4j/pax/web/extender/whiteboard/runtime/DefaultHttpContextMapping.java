/*
 * Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.whiteboard.runtime;

import java.util.Arrays;

import org.ops4j.pax.web.service.whiteboard.HttpContextMapping;
import org.ops4j.pax.web.service.http.HttpContext;

/**
 * Default implementation of {@link HttpContextMapping}.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, March 16, 2008
 */
public class DefaultHttpContextMapping extends AbstractContextMapping implements HttpContextMapping {

	/** Http context itself. */
	private HttpContext httpContext;

	/** Shared Http context */
	private boolean shared = false;

	@Override
	public HttpContext getHttpContext() {
		return httpContext;
	}

	@Override
	public boolean isShared() {
		return false;
	}

	public void setHttpContext(HttpContext httpContext) {
		this.httpContext = httpContext;
	}

	public void setShared(boolean shared) {
		this.shared = shared;
	}

	@Override
	public String toString() {
		return "DefaultHttpContextMapping{"
				+ "httpContext=" + httpContext
				+ ", contextId='" + getContextId() + '\''
				+ ", contextPath='" + getContextPath() + '\''
				+ ", initParameters=" + getInitParameters()
				+ ", virtualHosts=" + Arrays.toString(getVirtualHosts())
				+ ", shared=" + shared
				+ '}';
	}

}
