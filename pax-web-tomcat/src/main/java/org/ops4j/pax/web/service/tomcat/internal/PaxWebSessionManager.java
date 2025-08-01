/*
 * Copyright 2022 OPS4J.
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

import java.io.IOException;

import org.apache.catalina.Session;
import org.apache.catalina.session.StandardManager;

public class PaxWebSessionManager extends StandardManager {

	public PaxWebSessionManager() {
		this.setPathname("SESSIONS.ser");
	}

	@Override
	public Session findSession(String id) throws IOException {
		String sessionIdPrefix = PaxWebSessionIdGenerator.sessionIdPrefix.get();
		if (sessionIdPrefix != null && !id.startsWith(sessionIdPrefix + "~")) {
			id = sessionIdPrefix + "~" + id;
		}
		return super.findSession(id);
	}

	@Override
	public Session createSession(String sessionId) {
		if (sessionId == null && PaxWebSessionIdGenerator.sessionIdPrefix.get() != null
			&& PaxWebSessionIdGenerator.cookieSessionId.get() != null) {
			// it means the client has sent sessionId but it _may_ have a session associated in another OsgiContextModel
			// so we'll create new session with provided Session Id and available prefix indicating an OsgiContextModel
			sessionId = PaxWebSessionIdGenerator.sessionIdPrefix.get() + "~" + PaxWebSessionIdGenerator.cookieSessionId.get();
		}
		return super.createSession(sessionId);
	}

}
