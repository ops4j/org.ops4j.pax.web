/*  Copyright 2007 Alin Dreghiciu.
 *
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
package org.ops4j.pax.web.service.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Request;
import org.osgi.service.http.HttpContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * An HttServletRequestWrapper that handles HttpServiceAuthentication attributes.
 *
 * @author Alin Dreghiciu
 * @since December 10, 1007
 */
public class HttpServiceRequestWrapper extends HttpServletRequestWrapper {

    private static final Log LOG = LogFactory.getLog(HttpServiceRequestWrapper.class);

    private Request m_request = null;

    /**
     * Constructs a request object wrapping the given request.
     *
     * @throws IllegalArgumentException if the request is null
     */
    public HttpServiceRequestWrapper(final HttpServletRequest request) {
        super(request);
        // try to cast to a jetty servlet request. It may fail if a filter will change the request when calling handle
        // in the chain
        try {
            m_request = (Request) request;
        }
        catch (ClassCastException ignore) {
            LOG.warn("Could not cast the request to jetty Request, so the http service authentication related features will be disabled. Current request is of class [" + request.getClass().getName() + "]");
        }
    }

    /**
     * Filter the setting of authentication related attributes.
     * If one of HttpContext.AUTHENTICATION_TYPE or HTTPContext.REMOTE_USER set the corresponding values in original
     * request.
     *
     * @see javax.servlet.http.HttpServletRequest#setAttribute(String, Object)
     */
    @Override
    public void setAttribute(final String name, Object value) {
        if (HttpContext.AUTHENTICATION_TYPE.equals(name)) {
            handleAuthenticationType(name);
        } else if (HttpContext.REMOTE_USER.equals(name)) {
            handleRemoteUser(name);
        }
        super.setAttribute(name, value);
    }

    private void handleAuthenticationType(final String authenticationType) {
        if (!isJettyRequestAvailable()) {
            return;
        }
        // be defensive
        if (authenticationType != null) {
            if (!(authenticationType instanceof String)) {
                final String message = "Attribute " + HttpContext.AUTHENTICATION_TYPE + " expected to be a String but was an [" + authenticationType.getClass() + "]";
                LOG.error(message);
                throw new IllegalArgumentException(message);
            }
        }
        m_request.setAuthType((String) authenticationType);
    }

    private void handleRemoteUser(final String remoteUser) {
    }

    private boolean isJettyRequestAvailable() {
        if (m_request == null) {
            LOG.warn("HttpService authentication handling is currently disabled (most probably because the request is not a Jetty request. Setting this attribute has no effect");
        }
        return m_request != null;
    }    

}
