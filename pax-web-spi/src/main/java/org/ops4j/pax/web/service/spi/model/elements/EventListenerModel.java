/* Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.spi.model.elements;

import java.util.EventListener;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.spi.ServerEvent;
import org.ops4j.pax.web.service.spi.ServerListener;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWebContainerView;

public class EventListenerModel extends ElementModel<EventListener>{

	private final EventListener eventListener;

	public EventListenerModel(final EventListener eventListener) {
		NullArgumentException.validateNotNull(eventListener, "Listener");
		this.eventListener = eventListener;
	}

	@Override
	public void register(WhiteboardWebContainerView view) {
	}

	@Override
	public void unregister(WhiteboardWebContainerView view) {
	}

	public EventListener getEventListener() {
		return eventListener;
	}
	@Override
	public Boolean performValidation() {
		return Boolean.TRUE;
	}

}