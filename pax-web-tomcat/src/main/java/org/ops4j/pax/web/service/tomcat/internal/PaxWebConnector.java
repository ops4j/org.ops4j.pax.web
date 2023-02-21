/*
 * Copyright 2023 OPS4J.
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
package org.ops4j.pax.web.service.tomcat.internal;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;

/**
 * Special {@link Connector} where we can override the mechanism of setting session ID from incoming data at proper
 * stage due to Whiteboard session/context separation requirement.
 */
public class PaxWebConnector extends Connector {

	public PaxWebConnector(String protocol) {
		super(protocol);
	}

	@Override
	protected void initInternal() throws LifecycleException {
		// org.apache.catalina.connector.Connector.protocolHandler is already set
		// but org.apache.catalina.connector.Connector.adapter will be created when calling super.initInternal()
		// and passed to the protocolHandler
		PaxWebHttp11Nio2Protocol protocol = (PaxWebHttp11Nio2Protocol) getProtocolHandler();
		protocol.setConnector(this);

		// now, when protocolHandler will get the CoyoteAdapter, we'll swap it
		super.initInternal();
	}

}
