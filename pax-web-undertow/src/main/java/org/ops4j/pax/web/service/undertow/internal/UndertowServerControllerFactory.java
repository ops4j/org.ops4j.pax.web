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
package org.ops4j.pax.web.service.undertow.internal;

import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.xnio.XnioProvider;

/**
 * @author Achim Nierbeck
 */
@Component
public class UndertowServerControllerFactory implements ServerControllerFactory {

	// dummy reference to make sure the provider is available by the time the server starts
	private XnioProvider provider;

	private BundleContext context;

	@Activate
	public void activate(BundleContext context) {
		this.context = context;
	}

	@Reference
	public void setNioXnioProvider(XnioProvider provider) {
		this.provider = provider;
	}

	public void unsetNioXnioProvider(XnioProvider provider) {
		this.provider = null;
	}

	@Override
	public ServerController createServerController(ServerModel serverModel) {
		return new ServerControllerImpl(context);
	}

}
