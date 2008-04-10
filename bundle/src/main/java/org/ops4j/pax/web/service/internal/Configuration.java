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
package org.ops4j.pax.web.service.internal;

import java.io.File;

public interface Configuration
{

    Integer getHttpPort();

    Boolean isHttpEnabled();

    Integer getHttpSecurePort();

    Boolean isHttpSecureEnabled();

    /**
     * Returns the path to the keystore.
     *
     * @return path to the keystore.
     */
    String getSslKeystore();

    /**
     * Returns the password for keystore integrity check.
     *
     * @return the password for keystore integrity check
     */
    String getSslPassword();

    /**
     * Returns the password for keystore.
     *
     * @return the password for keystore
     */
    String getSslKeyPassword();

    /**
     * Returns the temporary directory, directory that will be set as
     * javax.servlet.context.tempdir.
     *
     * @return the temporary directory
     */
    File getTemporaryDirectory();

    /**
     * Returns the time in minutes after which an incative settion times out.
     * If returned value is null then no time out will be set (in jetty this will mean that there will be no timeout)
     *
     * @return timeout in minutes
     */
    Integer getSessionTimeout();

    /**
     * Returns the addresses to bind to
     * @return addresses
     */
    String[] getListeningAddresses();

}
