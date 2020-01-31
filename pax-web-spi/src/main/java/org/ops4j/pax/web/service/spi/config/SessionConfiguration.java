/*
 * Copyright 2020 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.spi.config;

public interface SessionConfiguration {

	/**
	 * Returns the time in minutes after which an incative settion times out. If
	 * returned value is null then no time out will be set (in jetty this will
	 * mean that there will be no timeout)
	 *
	 * @return timeout in minutes
	 */
	Integer getSessionTimeout();

	String getSessionCookie();

	String getSessionDomain();

	String getSessionPath();

	String getSessionUrl();

	Boolean getSessionCookieHttpOnly();

	Boolean getSessionCookieSecure();

	Integer getSessionCookieMaxAge();

	String getSessionStoreDirectory();

	Boolean getSessionLazyLoad();

}
