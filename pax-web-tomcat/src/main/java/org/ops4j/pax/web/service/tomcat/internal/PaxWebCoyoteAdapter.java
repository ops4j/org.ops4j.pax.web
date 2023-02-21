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

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.CoyoteAdapter;
import org.apache.catalina.connector.Request;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;

/**
 * Special {@link CoyoteAdapter}, which can alter the requested session IDs to match Whiteboard requirements.
 */
public class PaxWebCoyoteAdapter extends CoyoteAdapter {

	public PaxWebCoyoteAdapter(Connector connector) {
		super(connector);
	}

	@Override
	protected void parseSessionCookiesId(Request request) {
		// this method is called before org.apache.catalina.connector.CoyoteAdapter.parseSessionSslId(), but we
		// already have the mapping (to context / wrapper), so we can prepare the thread locals
		Wrapper wrapper = request.getWrapper();
		Context context = null;
		if (wrapper == null) {
			// it will be set in org.ops4j.pax.web.service.tomcat.internal.PaxWebStandardContextValve.invoke()
			// to "wrapperFor404Servlet". Here we need different context to get its OsgiContextModel
			context = request.getContext();
		}

		// Now we know the target servlet (even if it's 404 servlet) and we have to ensure that Session ID
		// generator knows about selected OsgiServletContext/OsgiContextModel
		// because of method signatures (unlike in Jetty) we have to use thread locals here...
		PaxWebStandardWrapper paxWebWrapper = wrapper instanceof PaxWebStandardWrapper
				? (PaxWebStandardWrapper) wrapper : null;
		OsgiContextModel osgiContextModel = paxWebWrapper != null && !paxWebWrapper.is404()
				? paxWebWrapper.getOsgiContextModel()
				: context instanceof PaxWebStandardContext
				? ((PaxWebStandardContext) context).getDefaultOsgiContextModel() : null;
		if (osgiContextModel != null) {
			String sessionIdPrefix = osgiContextModel.getTemporaryLocation().replaceAll("/", "_");
			PaxWebSessionIdGenerator.sessionIdPrefix.set(sessionIdPrefix);
		}
		// only now process the cookies
		super.parseSessionCookiesId(request);

		// this can be null
		PaxWebSessionIdGenerator.cookieSessionId.set(request.getRequestedSessionId());
	}

}
