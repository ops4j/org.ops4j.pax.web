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
package org.ops4j.pax.web.service.jetty.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.eclipse.jetty.ee8.nested.SessionHandler;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.session.AbstractSessionManager;
import org.eclipse.jetty.session.ManagedSession;
import org.eclipse.jetty.session.SessionManager;

public class PaxWebSessionHandler extends SessionHandler implements InvocationHandler {

	private final AbstractSessionManager original;
	private final SessionManager wrapped;

	public PaxWebSessionHandler() {
		this.original = (AbstractSessionManager) super.getSessionManager();
		this.wrapped = (SessionManager) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { SessionManager.class }, this);
	}

	@Override
	public SessionManager getSessionManager() {
		return wrapped;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getName().equals("getSessionCookie")) {
			return getSessionCookie((ManagedSession) args[0], (Boolean) args[1]);
		}
		if (method.getName().equals("renewSessionId")) {
			renewSessionId((String) args[0], (String) args[1], (String) args[2], (String) args[3]);
			return null;
		}
		return method.invoke(original, args);
	}

	public HttpCookie getSessionCookie(ManagedSession session, boolean requestIsSecure) {
		HttpCookie cookie = original.getSessionCookie(session, requestIsSecure);
		if (cookie != null) {
			String id = getExtendedId(cookie.getValue());
			return HttpCookie.from(cookie.getName(), id, original.getSessionCookieAttributes());
		}
		return cookie;
	}

	public void renewSessionId(String oldId, String oldExtendedId, String newId, String newExtendedId) {
		int id1 = oldId.indexOf("~");
		int id2 = oldExtendedId.indexOf("~");
		int id3 = newId.indexOf("~");
		int id4 = newExtendedId.indexOf("~");
		if (id1 > 0 && id2 > 0 && id3 == -1 && id4 == -1) {
			String s1 = oldId.substring(id1);
			String s2 = oldExtendedId.substring(id2);
			original.renewSessionId(oldId, oldExtendedId, newId + s1, newExtendedId + s2);
		} else {
			original.renewSessionId(oldId, oldExtendedId, newId, newExtendedId);
		}
	}

	public String getExtendedId(String eid) {
		int tilde = eid.indexOf("~");
		if (tilde == -1) {
			return eid;
		}
		// org.eclipse.jetty.server.session.DefaultSessionIdManager.setWorkerName()
		// ensures that there should be no dot in worker name appended to session id
		int dot = eid.lastIndexOf(".");
		if (dot == -1) {
			return eid.substring(0, tilde);
		}
		return eid.substring(0, tilde) + eid.substring(dot);
	}

}
