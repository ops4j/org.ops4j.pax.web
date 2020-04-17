/*
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
package org.ops4j.pax.web.service.tomcat.internal;

import java.io.IOException;
import javax.servlet.ServletException;

import org.apache.catalina.Contained;
import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.mapper.Mapper;
import org.apache.catalina.mapper.MappingData;
import org.apache.catalina.valves.ValveBase;

public class ContextSelectionHostValve extends ValveBase {

	Valve standardHostValve;
	Mapper mapper;

	public ContextSelectionHostValve(Valve standardHostValve, Mapper mapper) {
		super(true);
		this.standardHostValve = standardHostValve;
		this.mapper = mapper;
	}

	@Override
	public void invoke(Request request, Response response) throws IOException, ServletException {
		/*
		 * The OSGi HTTP service (and the whiteboard extender) will create one
		 * Context with an empty context path per HttpContext. Tomcat will
		 * interpret this as different context versions.
		 * 
		 * At this point all these Contexts will be in the contexts array in the
		 * mappingData attribute of the request. The context and the wrapper
		 * element in the mappingData are set to the last element of the
		 * contexts array (which is the last version for Tomcat).
		 * 
		 * This code will select the first context element that has a wrapper
		 * for the request URI and set the context and wrapper elements in the
		 * mapping data accordingly. Afterwards the standard host valve is
		 * executed with the modified request.
		 */
		MappingData md = request.getMappingData();
		if (md.contexts != null && md.contexts.length > 1 && md.wrapper == null) {
			for (int i = 0; md.wrapper == null && i < md.contexts.length; i++) {
				md.context = md.contexts[i];
				mapper.map(md.context, request.getDecodedRequestURIMB(), md);
			}
		}
		standardHostValve.invoke(request, response);
	}

	@Override
	public void setContainer(Container container) {
		super.setContainer(container);
		if (standardHostValve instanceof Contained) {
			((Contained) standardHostValve).setContainer(container);
		}
	}

	@Override
	protected void startInternal() throws LifecycleException {
		super.startInternal();
		if (standardHostValve instanceof Lifecycle) {
			((Lifecycle) standardHostValve).start();
		}
	}

	@Override
	protected void stopInternal() throws LifecycleException {
		super.stopInternal();
		if (standardHostValve instanceof Lifecycle) {
			((Lifecycle) standardHostValve).stop();
		}
	}
}
