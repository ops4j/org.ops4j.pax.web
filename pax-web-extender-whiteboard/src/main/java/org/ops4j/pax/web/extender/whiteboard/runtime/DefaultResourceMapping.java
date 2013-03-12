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

import org.ops4j.pax.web.extender.whiteboard.ResourceMapping;

/**
 * Default implementation of {@link ResourceMapping}.
 * 
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class DefaultResourceMapping implements ResourceMapping {

	/**
	 * Http Context id.
	 */
	private String httpContextId;
	/**
	 * Alias.
	 */
	private String alias;
	/**
	 * Url patterns.
	 */
	private String path;

	/**
	 * @see ResourceMapping#getHttpContextId()
	 */
	public String getHttpContextId() {
		return httpContextId;
	}

	/**
	 * @see ResourceMapping#getAlias()
	 */
	public String getAlias() {
		return alias;
	}

	/**
	 * @see ResourceMapping#getPath()
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Setter.
	 * 
	 * @param httpContextId
	 *            id of the http context this resource belongs to
	 */
	public void setHttpContextId(final String httpContextId) {
		this.httpContextId = httpContextId;
	}

	/**
	 * Setter.
	 * 
	 * @param alias
	 *            alias this resource maps to
	 */
	public void setAlias(final String alias) {
		this.alias = alias;
	}

	/**
	 * Setter.
	 * 
	 * @param path
	 *            local path in the bundle
	 */
	public void setPath(final String path) {
		this.path = path;
	}

	@Override
	public String toString() {
		return new StringBuffer().append(this.getClass().getSimpleName())
				.append("{").append("httpContextId=").append(httpContextId)
				.append(",alias=").append(alias).append(",path=").append(path)
				.append("}").toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((alias == null) ? 0 : alias.hashCode());
		result = prime * result
				+ ((httpContextId == null) ? 0 : httpContextId.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final DefaultResourceMapping other = (DefaultResourceMapping) obj;
		if (alias == null) {
			if (other.alias != null) {
				return false;
			}
		} else if (!alias.equals(other.alias)) {
			return false;
		}
		if (httpContextId == null) {
			if (other.httpContextId != null) {
				return false;
			}
		} else if (!httpContextId.equals(other.httpContextId)) {
			return false;
		}
		if (path == null) {
			if (other.path != null) {
				return false;
			}
		} else if (!path.equals(other.path)) {
			return false;
		}
		return true;
	}

}