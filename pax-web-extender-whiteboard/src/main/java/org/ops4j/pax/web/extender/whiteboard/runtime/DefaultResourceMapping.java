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

import org.ops4j.pax.web.service.whiteboard.ResourceMapping;

/**
 * Default implementation of {@link ResourceMapping}.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class DefaultResourceMapping extends AbstractContextRelated implements ResourceMapping {

	private String[] urlPatterns = new String[0];
	private String alias;
	private String path;

	@Override
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	@Override
	public String[] getUrlPatterns() {
		return urlPatterns;
	}

	public void setUrlPatterns(String[] urlPatterns) {
		this.urlPatterns = urlPatterns;
	}

	@Override
	public String toString() {
		return "DefaultResourceMapping{"
				+ ", urlPatterns=" + Arrays.toString(getUrlPatterns())
				+ ", alias='" + getAlias() + '\''
				+ ", contextSelectFilter='" + contextSelectFilter + '\''
				+ ", contextId='" + contextId + '\''
				+ '}';
	}

}
