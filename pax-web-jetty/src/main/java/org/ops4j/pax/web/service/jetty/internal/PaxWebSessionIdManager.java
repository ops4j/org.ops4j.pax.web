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

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;

public class PaxWebSessionIdManager extends DefaultSessionIdManager {

	public PaxWebSessionIdManager(Server server) {
		super(server);
	}

	public static String getSessionIdSuffix(HttpServletRequest request) {
		if (request instanceof Request) {
			Request r = (Request) request;
			UserIdentity.Scope uis = r.getUserIdentityScope();
			if (uis instanceof PaxWebServletHolder) {
				PaxWebServletHolder holder = (PaxWebServletHolder) uis;
				OsgiContextModel ocm = holder.getOsgiContextModel();
				// we can't replace '/' to '_' because of how
				// org.eclipse.jetty.server.session.FileSessionDataStore.initializeStore() analyzes the
				// session files.
				return "~" + ocm.getTemporaryLocation().replaceAll("/", "#").replaceAll("_", "#");
			}
		}
		return null;
	}

	@Override
	public String renewSessionId(String oldClusterId, String oldNodeId, HttpServletRequest request) {
		// copy from org.eclipse.jetty.server.session.DefaultSessionIdManager.renewSessionId()
		// with OsgiContextModel suffix

		String newClusterId = newSessionId(request.hashCode());
		int id1 = oldClusterId.indexOf("~");
		if (id1 > 0) {
			newClusterId += oldClusterId.substring(id1);
		}

		for (SessionHandler manager : getSessionHandlers()) {
			manager.renewSessionId(oldClusterId, oldNodeId, newClusterId, getExtendedId(newClusterId, request));
		}

		return newClusterId;
	}

	@Override
	public String newSessionId(HttpServletRequest request, long created) {
		if (request == null) {
			// org.eclipse.jetty.server.session.DefaultSessionIdManager.newSessionId(long) will be called
			// only with "created" as a seed
			return newSessionId(created);
		}

		// request's hashCode + created will be used to get ID, but it'll be altered depending on the context
		String suffix = getSessionIdSuffix(request);
		if (suffix != null) {
			// it means that we may be accessing existing session for a context, but through a different
			// OsgiContextModel. This means we have to create another session, but with the same JSESSIONID prefix
			String rsid = request.getRequestedSessionId();
			if (rsid == null) {
				return super.newSessionId(request, created) + suffix;
			}
			String sid = getId(rsid);
			return sid + suffix;
		}
		return super.newSessionId(request, created);
	}

}
