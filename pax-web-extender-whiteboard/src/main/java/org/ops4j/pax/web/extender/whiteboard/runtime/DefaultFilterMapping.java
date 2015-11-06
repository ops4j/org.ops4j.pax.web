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

import javax.servlet.Filter;

import org.ops4j.pax.web.extender.whiteboard.FilterMapping;

/**
 * Default implementation of {@link FilterMapping}.
 * 
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class DefaultFilterMapping implements FilterMapping {

	/**
	 * Http Context id.
	 */
	private String httpContextId;
	/**
	 * Filter.
	 */
	private Filter filter;
	/**
	 * Url patterns.
	 */
	private String[] urlPatterns;
	/**
	 * Servlets names the filter was registered for.
	 */
	private String[] servletNames;
	/**
	 * Initialization parameters.
	 */
	private Map<String, String> initParams;
	
	/**
	 * Filter supports async calls
	 */
    private Boolean asyncSupported;

	/**
	 * @see FilterMapping#getHttpContextId()
	 */
	public String getHttpContextId() {
		return httpContextId;
	}

	/**
	 * @see FilterMapping#getFilter()
	 */
	public Filter getFilter() {
		return filter;
	}

	/**
	 * @see FilterMapping#getUrlPatterns()
	 */
	public String[] getUrlPatterns() {
		return urlPatterns;
	}

	/**
	 * @see FilterMapping#getServletNames()
	 */
	public String[] getServletNames() {
		return servletNames;
	}

	/**
	 * @see FilterMapping#getInitParams()
	 */
	public Map<String, String> getInitParams() {
		return initParams;
	}

	public Boolean getAsyncSupported() {
        return asyncSupported;
    }
	
	/**
	 * Setter.
	 * 
	 * @param httpContextId
	 *            id of the http context this filter belongs to
	 */
	public void setHttpContextId(final String httpContextId) {
		this.httpContextId = httpContextId;
	}

	/**
	 * Setter.
	 * 
	 * @param filter
	 *            mapped filter
	 */
	public void setFilter(final Filter filter) {
		this.filter = filter;
	}

	/**
	 * Setter.
	 * 
	 * @param urlPatterns
	 *            array of url patterns
	 */
	public void setUrlPatterns(final String... urlPatterns) {
		this.urlPatterns = urlPatterns;
	}

	/**
	 * Setter.
	 * 
	 * @param servletNames
	 *            array of servlet aliases
	 */
	public void setServletNames(final String... servletNames) {
		this.servletNames = servletNames;
	}

	/**
	 * Seter.
	 * 
	 * @param initParams
	 *            map of initialization parameters
	 */
	public void setInitParams(final Map<String, String> initParams) {
		this.initParams = initParams;
	}
	
    public void setAsyncSupported(Boolean asyncSupported) {
        this.asyncSupported = asyncSupported;
    }


    @Override
    public String toString() {
        return "DefaultFilterMapping [httpContextId=" + httpContextId + ", filter=" + filter + ", urlPatterns="
                + Arrays.toString(urlPatterns) + ", servletNames=" + Arrays.toString(servletNames) + ", initParams="
                + initParams + ", asyncSupported=" + asyncSupported + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((asyncSupported == null) ? 0 : asyncSupported.hashCode());
        result = prime * result + ((filter == null) ? 0 : filter.hashCode());
        result = prime * result + ((httpContextId == null) ? 0 : httpContextId.hashCode());
        result = prime * result + ((initParams == null) ? 0 : initParams.hashCode());
        result = prime * result + Arrays.hashCode(servletNames);
        result = prime * result + Arrays.hashCode(urlPatterns);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DefaultFilterMapping other = (DefaultFilterMapping) obj;
        if (asyncSupported == null) {
            if (other.asyncSupported != null)
                return false;
        } else if (!asyncSupported.equals(other.asyncSupported))
            return false;
        if (filter == null) {
            if (other.filter != null)
                return false;
        } else if (!filter.equals(other.filter))
            return false;
        if (httpContextId == null) {
            if (other.httpContextId != null)
                return false;
        } else if (!httpContextId.equals(other.httpContextId))
            return false;
        if (initParams == null) {
            if (other.initParams != null)
                return false;
        } else if (!initParams.equals(other.initParams))
            return false;
        if (!Arrays.equals(servletNames, other.servletNames))
            return false;
        if (!Arrays.equals(urlPatterns, other.urlPatterns))
            return false;
        return true;
    }

}