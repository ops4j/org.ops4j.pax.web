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
package org.ops4j.pax.web.service.spi.model.elements;

import org.ops4j.pax.web.annotations.Review;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.events.WebElementEventData;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWebContainerView;

@Review("Not refactored yet")
public class WebSocketModel extends ElementModel {

	private Object webSocket;

	public WebSocketModel(OsgiContextModel contextModel, Object webSocket) {
//		super(contextModel);
		this.webSocket = webSocket;
	}

	@Override
	public void register(WhiteboardWebContainerView view) {
	}

	@Override
	public void unregister(WhiteboardWebContainerView view) {
	}

	@Override
	public WebElementEventData asEventData() {
		return null;
	}

	public Object getWebSocket() {
		return webSocket;
	}
	@Override
	public Boolean performValidation() {
		return Boolean.TRUE;
	}

}
