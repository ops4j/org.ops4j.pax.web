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
package org.ops4j.pax.web.service.spi.model.events;

import java.net.InetSocketAddress;

/**
 * Event related to entire {@link org.ops4j.pax.web.service.spi.ServerController} state. After Pax Web 8, such
 * event carries more information.
 */
public class ServerEvent {

	public enum State {
		STARTED, STOPPED, CONFIGURED
	}

	private final Address[] addresses;
	private final State state;

	public ServerEvent(State state, Address[] addresses) {
		this.state = state;
		this.addresses = addresses;
	}

	public State getState() {
		return state;
	}

	public Address[] getAddresses() {
		return addresses;
	}

	public static class Address {
		private final InetSocketAddress address;
		private final boolean secure;

		public Address(InetSocketAddress address, boolean secure) {
			this.address = address;
			this.secure = secure;
		}

		public InetSocketAddress getAddress() {
			return address;
		}

		public boolean isSecure() {
			return secure;
		}
	}

}
