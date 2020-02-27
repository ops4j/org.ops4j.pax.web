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

import java.util.Map;

import org.ops4j.pax.web.service.whiteboard.HttpContextMapping;
import org.osgi.service.http.HttpContext;

/**
 * Default implementation of {@link HttpContextMapping}.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, March 16, 2008
 */
public class DefaultHttpContextMapping implements HttpContextMapping {

	/**
	 * Context id.
	 */
	private String httpContextId;
	/**
	 * Context path.
	 */
	private String path;
	/**
	 * Context parameters.
	 */
	private Map<String, String> parameters;
	/**
	 * Http context.
	 */
	private HttpContext httpContext;

	/**
	 * Shared Http context, flag showing if this
	 */
	private Boolean sharedHttpContext = false;

	/**
	 * @see HttpContextMapping#getContextId()
	 */
	public String getContextId() {
		return httpContextId;
	}

	/**
	 * @see HttpContextMapping#getContextPath()
	 */
	public String getContextPath() {
		return path;
	}

	/**
	 * @see HttpContextMapping#getParameters()
	 */
	public Map<String, String> getInitParameters() {
		return parameters;
	}

	/**
	 * @see HttpContextMapping#getHttpContext()
	 */
	public HttpContext getHttpContext() {
		return httpContext;
	}

	/**
	 * Setter.
	 *
	 * @param httpContextId context id; can be null
	 */
	public void setHttpContextId(final String httpContextId) {
		this.httpContextId = httpContextId;
	}

	/**
	 * Setter.
	 *
	 * @param path context path; can be null
	 */
	public void setPath(final String path) {
		this.path = path;
	}

	/**
	 * Setter.
	 *
	 * @param parameters context parameters; can be null
	 */
	public void setParameters(final Map<String, String> parameters) {
		this.parameters = parameters;
	}

	/**
	 * Setter.
	 *
	 * @param httpContext http context; can be null case when a default http context
	 *                    will be used
	 */
	public void setHttpContext(HttpContext httpContext) {
		this.httpContext = httpContext;
	}

	/**
	 * Setter.
	 *
	 * @param sharedContext shared http context; is false by default, needs to be actively
	 *                      set to true.
	 */
	public void setHttpContextShared(Boolean sharedContext) {
		this.sharedHttpContext = sharedContext;
	}

	public boolean getHttpContextShared() {
		return this.sharedHttpContext;
	}

    @Override
    public String toString() {
        return "DefaultHttpContextMapping{" +
                "httpContextId='" + httpContextId + '\'' +
                ", path='" + path + '\'' +
                ", parameters=" + parameters +
                ", httpContext=" + httpContext +
                ", sharedHttpContext=" + sharedHttpContext +
                '}';
    }


}