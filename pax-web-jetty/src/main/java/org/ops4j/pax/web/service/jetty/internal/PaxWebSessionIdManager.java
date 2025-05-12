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

import org.eclipse.jetty.ee8.nested.ContextHandler;
import org.eclipse.jetty.ee8.nested.Request;
import org.eclipse.jetty.ee8.nested.UserIdentityScope;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.session.DefaultSessionIdManager;
import org.eclipse.jetty.session.SessionManager;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaxWebSessionIdManager extends DefaultSessionIdManager {

	public static final Logger LOG = LoggerFactory.getLogger(PaxWebSessionIdManager.class);

	public PaxWebSessionIdManager(Server server) {
		super(server);
	}

	public static String getSessionIdSuffix(Request request) {
		UserIdentityScope uis = request.getUserIdentityScope();
		if (uis instanceof PaxWebServletHolder) {
			PaxWebServletHolder holder = (PaxWebServletHolder) uis;
			OsgiContextModel ocm = holder.getOsgiContextModel();
			// we can't replace '/' to '_' because of how
			// org.eclipse.jetty.server.session.FileSessionDataStore.initializeStore() analyzes the
			// session files.
			if (ocm == null || ocm.getTemporaryLocation() == null) {
				return "";
			}
			return "~" + ocm.getTemporaryLocation().replaceAll("/", "#").replaceAll("_", "#");
		}

		return "";
	}

	@Override
	public String renewSessionId(String oldClusterId, String oldNodeId, org.eclipse.jetty.server.Request request) {
		// copy from org.eclipse.jetty.server.session.DefaultSessionIdManager.renewSessionId()
		// with OsgiContextModel suffix

		String newClusterId = newSessionId(request.hashCode());
		int id1 = oldClusterId.indexOf("~");
		if (id1 > 0) {
			newClusterId += oldClusterId.substring(id1);
		}

		for (SessionManager manager : getSessionManagers()) {
			try {
				manager.renewSessionId(oldClusterId, oldNodeId, newClusterId, getExtendedId(newClusterId, request));
			} catch (Exception e) {
				LOG.warn("Problem renewing session id {} to {}", oldClusterId, newClusterId, e);
			}
		}

		return newClusterId;
	}

	@Override
	public String newSessionId(org.eclipse.jetty.server.Request request, String requestedId, long created) {
		if (request == null) {
			// org.eclipse.jetty.server.session.DefaultSessionIdManager.newSessionId(long) will be called
			// only with "created" as a seed
			return newSessionId(created);
		}

		ContextHandler.CoreContextRequest r = org.eclipse.jetty.server.Request.as(request, ContextHandler.CoreContextRequest.class);
		// request's hashCode + created will be used to get ID, but it'll be altered depending on the context
		String suffix = getSessionIdSuffix(r.getHttpChannel().getRequest());
		if (suffix != null) {
			// it means that we may be accessing existing session for a context, but through a different
			// OsgiContextModel. This means we have to create another session, but with the same JSESSIONID prefix
			if (requestedId == null) {
				return super.newSessionId(request, requestedId, created) + suffix;
			}
			String sid = getId(requestedId);
			return sid + suffix;
		}
		return super.newSessionId(request, requestedId, created);
	}

}
