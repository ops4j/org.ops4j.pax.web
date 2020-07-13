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
package org.ops4j.pax.web.service.tomcat.internal;

import java.io.IOException;
import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

import static org.ops4j.pax.web.service.tomcat.internal.PaxWebStandardContext.PAXWEB_STANDARD_WRAPPER;

public class PaxWebStandardWrapperValve extends ValveBase {

	/**
	 * {@link org.apache.catalina.core.StandardWrapper} of the servlet for which this valve is used to handle
	 * a request.
	 */
	private final PaxWebStandardWrapper wrapper;

	/**
	 * <em>Real</em> Tomcat's context that's used to obtain default {@link javax.servlet.ServletContext} and
	 * {@link org.ops4j.pax.web.service.spi.model.OsgiContextModel} for filter chain creation.
	 */
	private final PaxWebStandardContext realContext;

	public PaxWebStandardWrapperValve(ValveBase next, PaxWebStandardWrapper wrapper, PaxWebStandardContext realContext) {
		setNext(next);
		setAsyncSupported(wrapper.isAsyncSupported());
		setContainer(wrapper);
		setDomain(wrapper.getDomain());

		this.wrapper = wrapper;
		this.realContext = realContext;
	}

	/**
	 * This {@link org.apache.catalina.Valve#invoke(Request, Response)} does similar work as Jetty's
	 * {@code org.ops4j.pax.web.service.jetty.internal.PaxWebServletHandler#doHandle()}
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws ServletException
	 */
	@Override
	public void invoke(Request request, Response response) throws IOException, ServletException {
		// attributes to be used ONLY by "initial OSGi filter"
		request.setAttribute(PAXWEB_STANDARD_WRAPPER, request.getWrapper());

		getNext().invoke(request, response);
	}

}
