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

import org.ops4j.pax.web.service.internal.AbstractHttpServiceConfiguration;

public class DefaultHttpServiceConfiguration extends AbstractHttpServiceConfiguration
{

    private final static int DEFAULT_HTTP_PORT = 8080;
    private final static int DEFAULT_HTTP_SECURE_PORT = 8443;
    private final static boolean DEFAULT_HTTP_ENABLED = true;
    private final static boolean DEFAULT_HTTP_SECURE_ENABLED = false;

    public DefaultHttpServiceConfiguration()
    {
        m_httpPort = DEFAULT_HTTP_PORT;
        m_httpSecurePort = DEFAULT_HTTP_SECURE_PORT;
        m_httpEnabled = DEFAULT_HTTP_ENABLED;
        m_httpSecureEnabled = DEFAULT_HTTP_SECURE_ENABLED;
    }

}
