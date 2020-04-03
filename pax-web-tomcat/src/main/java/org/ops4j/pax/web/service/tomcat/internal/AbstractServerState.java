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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.Servlet;

import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.LifeCycle;
import org.ops4j.pax.web.service.spi.model.elements.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.SecurityConstraintMappingModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.model.elements.WelcomeFileModel;
import org.osgi.service.http.HttpContext;

/**
 * @author Romain Gilles
 */
abstract class AbstractServerState /*implements ServerState */{
//	private final TomcatServerStateFactory serverStateFactory;
//
//	public AbstractServerState(TomcatServerStateFactory serverStateFactory) {
//		this.serverStateFactory = serverStateFactory;
//	}
//
//	TomcatServerStateFactory getServerStateFactory() {
//		return serverStateFactory;
//	}
//
//	<T> T throwIllegalState() {
//		return throwIllegalState(getState(), getSupportedOperations());
//	}
//
//	private <T> T throwIllegalState(States serverState,
//									Collection<String> supportedOperations) {
//		throw new IllegalStateException(
//				String.format(
//						"server current state is: %s. The only supported operation(s): %s",
//						serverState, supportedOperations));
//	}

	Collection<String> getSupportedOperations() {
		ArrayList<String> result = new ArrayList<>();
		result.add(formatSupportedOperation("configure", Configuration.class));
		return result;
	}

	String formatSupportedOperation(String methodName, Class<?>... parameters) {
		StringBuilder result = new StringBuilder();
		result.append('#').append(methodName).append('(');
		if (parameters != null) {
			Iterator<Class<?>> iterator = Arrays.asList(parameters).iterator();
			if (iterator.hasNext()) {
				result.append(iterator.next().getSimpleName());
			}
			while (iterator.hasNext()) {
				result.append(", ").append(iterator.next().getSimpleName());
			}
		}
		result.append(')');
		return result.toString();
	}

}