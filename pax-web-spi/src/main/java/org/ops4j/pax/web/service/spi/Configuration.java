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
package org.ops4j.pax.web.service.spi;

import java.io.File;
import java.util.List;

public interface Configuration
{

    Boolean useNIO();

    Integer getHttpPort();
    
    String getHttpConnectorName();

    Boolean isHttpEnabled();

    Integer getHttpSecurePort();
    
    String getHttpSecureConnectorName();

    Boolean isHttpSecureEnabled();

    /**
     * Set the value of the needClientAuth property
     *
     * @return true if we require client certificate authentication
     */
    Boolean isClientAuthNeeded();

    /**
     * Set the value of the _wantClientAuth property. This property is used when opening server sockets.
     *
     * @return true if we want client certificate authentication
     */
    Boolean isClientAuthWanted();

    /**
     * Returns the path to the keystore.
     *
     * @return path to the keystore.
     */
    String getSslKeystore();

    /**
     * Returns the keystore type.
     *
     * @return keystore type.
     */
    String getSslKeystoreType();

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

    String getSessionCookie();

    String getSessionUrl();

    String getWorkerName();

    /**
     * Returns the addresses to bind to
     *
     * @return addresses
     */
    String[] getListeningAddresses();

	/**
	 * Returns the directory containing the external configuration
	 * 
	 * @return
	 */
	File getConfigurationDir();

	String getJspScratchDir();

	Integer getJspCheckInterval();

	Boolean getJspClassDebugInfo();

	Boolean getJspDevelopment();

	Boolean getJspEnablePooling();

	String getJspIeClassId();

	String getJspJavaEncoding();

	Boolean getJspKeepgenerated();

	String getJspLogVerbosityLevel();

	Boolean getJspMappedfile();

	Integer getJspTagpoolMaxSize();

	Boolean isLogNCSAFormatEnabled();
	
    String getLogNCSAFormat();
	
	String getLogNCSARetainDays();

    Boolean isLogNCSAAppend();

    Boolean isLogNCSAExtended();
    
    Boolean isLogNCSADispatch();

    String getLogNCSATimeZone();

	String getLogNCSADirectory();

	Boolean getJspPrecompilation();
	
	List<String> getVirtualHosts();
	
	List<String> getConnectors();

}
