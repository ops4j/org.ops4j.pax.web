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
package org.ops4j.pax.web.service.jetty.internal;

import org.eclipse.jetty.server.bio.SocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps a jetty SocketConnector in order to catch exceptions on connector
 * opening. If that's the case it will just log the
 */
class SocketConnectorWrapper extends SocketConnector {

	private static final Logger LOG = LoggerFactory
			.getLogger(SocketConnectorWrapper.class);

	@Override
	protected void doStart() throws Exception {
		try {
			super.doStart();
		} catch (Exception e) { 
			LOG.warn("Connection on port " + getPort()
					+ " cannot be open. Reason: " + e.getMessage());
		}

	}
}
