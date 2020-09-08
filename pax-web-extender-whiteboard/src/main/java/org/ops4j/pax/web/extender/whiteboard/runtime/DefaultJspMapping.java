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
import java.util.Map;

import org.ops4j.pax.web.service.whiteboard.JspMapping;

/**
 * Default implementation of {@link JspMapping}.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, March 15, 2008
 */
public class DefaultJspMapping extends AbstractContextRelated implements JspMapping {

	/** Url patterns. */
	private String[] urlPatterns;

	/** Initialization parameters. */
	private Map<String, String> initParameters;

	@Override
	public Map<String, String> getInitParameters() {
		return initParameters;
	}

	public void setInitParameters(Map<String, String> initParameters) {
		this.initParameters = initParameters;
	}

	@Override
	public String[] getUrlPatterns() {
		return urlPatterns;
	}

	public void setUrlPatterns(String ... urlPatterns) {
		this.urlPatterns = urlPatterns;
	}

	@Override
	public String toString() {
		return "DefaultJspMapping{urlPatterns=" + Arrays.deepToString(urlPatterns)
				+ ",initParams=" + initParameters + "}";
	}

}
