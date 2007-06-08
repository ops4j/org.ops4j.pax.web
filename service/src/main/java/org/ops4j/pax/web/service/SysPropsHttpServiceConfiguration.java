/* Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

public class SysPropsHttpServiceConfiguration implements HttpServiceConfiguration {
    private static final Log m_logger = LogFactory.getLog(SysPropsHttpServiceConfiguration.class);

    private final static int DEFAULT_HTTP_PORT = 80;
    private final static int DEFAULT_HTTP_SECURE_PORT = 443;

    private final static String PROPERTY_HTTP_PORT = "org.osgi.service.http.port";
    private final static String PROPERTY_HTTP_SECURE_PORT = "org.osgi.service.http.port.secure";

    private int m_httpPort = DEFAULT_HTTP_PORT;
    private int m_httpSecurePort = DEFAULT_HTTP_SECURE_PORT;

    private boolean m_httpEnabled = true;
    private boolean m_httpSecureEnabled = true;

    public SysPropsHttpServiceConfiguration(BundleContext context) {
	try {
	    if (context.getProperty(PROPERTY_HTTP_PORT) != null) {
		m_httpPort = Integer.parseInt(context.getProperty(PROPERTY_HTTP_PORT));
	    }
	} catch (Exception e) {
	    m_logger.warn("Reading property " + PROPERTY_HTTP_PORT + " has failed");
	}
	try {
	    if (context.getProperty(PROPERTY_HTTP_SECURE_PORT) != null) {
		m_httpSecurePort = Integer.parseInt(context.getProperty(PROPERTY_HTTP_SECURE_PORT));
	    }
	} catch (Exception e) {
	    m_logger.warn("Reading property " + PROPERTY_HTTP_SECURE_PORT + " has failed");
	}
    }

    public int getHttpPort() {
	return m_httpPort;
    }

    public boolean isHttpEnabled() {
	return m_httpEnabled;
    }

    public int getHttpSecurePort() {
	return m_httpSecurePort;
    }

    public boolean isHttpSecureEnabled() {
	return m_httpSecureEnabled;
    }

}
