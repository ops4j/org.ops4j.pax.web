/* Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.samples.helloworld.wc.internal;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * User Session Listener.
 *
 * @author Anaximandro de Godinho
 */
public class UserSessionListener implements HttpSessionListener {

	/** The user name string, stored in the seesion. */
	private static final String P_USER_NAME = "userName";

	/** One counter, ONLY to see if the listener were fired. */
	private static Integer fireCounter = 0;

	/** Our local session store, by id - synchronized. */
	private static final Map<String, HttpSession> SESSIONS = new Hashtable<>();

	public void sessionCreated(final HttpSessionEvent event) {
		final HttpSession session = event.getSession();

		fireCounter++;
		session.setAttribute(P_USER_NAME, "sessionFired_" + fireCounter);

		final String id = session.getId();
		SESSIONS.put(id, session);
	}

	public void sessionDestroyed(final HttpSessionEvent event) {
		final HttpSession session = event.getSession();
		final String id = session.getId();
		SESSIONS.remove(id);
	}

	public List<String> getUserNames() {
		final List<String> users = new ArrayList<>();

		for (final String id : SESSIONS.keySet()) {
			final HttpSession session = SESSIONS.get(id);
			final String userName = (String) session.getAttribute(P_USER_NAME);
			users.add(userName);
		}

		return users;
	}

}
