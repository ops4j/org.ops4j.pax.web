/* Copyright 2010 Anaximandro de Godinho.
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
 * @author Anaximandro de Godinho (Woody)
 */
public class HelloWorldSessionListener implements HttpSessionListener {
	/**
	 * Local session store, by id - synchronized.
	 */
	private static final Map<String, HttpSession> SESSIONS = new Hashtable<>();

	/**
	 * Counter, good idea if you do need to ensure that this listener were
	 * fired.
	 */
	private static int counter;

	/**
	 * Fires whenever a new session is created.
	 */
	public void sessionCreated(final HttpSessionEvent event) {
		counter++;
		final HttpSession session = event.getSession();
		final String id = session.getId();
		SESSIONS.put(id, session);
	}

	/**
	 * Fires whenever a session is destroyed.
	 */
	public void sessionDestroyed(final HttpSessionEvent event) {
		final HttpSession session = event.getSession();
		final String id = session.getId();
		SESSIONS.remove(id);
		counter--;
	}

	/**
	 * Return a list with all session values for a given attribute name.
	 *
	 * @return a list with all session values for a given attribute name.
	 */
	public static synchronized List<Object> getAttributes(final String name) {
		final List<Object> data = new ArrayList<>();

		for (final String id : SESSIONS.keySet()) {
			final HttpSession session = SESSIONS.get(id);
			try {
				final Object o = session.getAttribute(name);
				data.add(o);
				//CHECKSTYLE:OFF
			} catch (final Exception e) {
				// no data for this object.
			}
			//CHECKSTYLE:ON
		}

		return data;
	}

	/**
	 * Return the current session counter.
	 *
	 * @return the current session counter.
	 */
	public static synchronized int getCounter() {
		return counter;
	}
}