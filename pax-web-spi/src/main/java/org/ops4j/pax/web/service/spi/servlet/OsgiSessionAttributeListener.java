/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.service.spi.servlet;

import java.util.EventListener;
import java.util.List;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;

/**
 * A runtime-agnostic interface used to call actual {@link javax.servlet.http.HttpSessionAttributeListener}
 * listeners registered in specific runtime.
 */
public final class OsgiSessionAttributeListener {

	private final List<EventListenerModel> sessionListenerModels;

	public OsgiSessionAttributeListener(List<EventListenerModel> sessionListenerModels) {
		this.sessionListenerModels = sessionListenerModels;
	}

	/**
	 * Notification about attribute change in {@link OsgiHttpSession}
	 *
	 * @param session
	 * @param model {@link OsgiContextModel} for which the {@link javax.servlet.http.HttpSession} is scoped
	 * @param name
	 * @param value
	 * @param old
	 */
	public void callSessionListeners(HttpSession session, OsgiContextModel model, String name, Object value, Object old) {
		for (EventListenerModel elm : sessionListenerModels) {
			if (model == null || elm.getContextModels().contains(model)) {
				EventListener listener = elm.getResolvedListener();
				if (listener instanceof HttpSessionAttributeListener) {
					// can't imagine other scenario...
					HttpSessionBindingEvent event = new HttpSessionBindingEvent(session, name, old == null ? value : old);
					if (value == null) {
						((HttpSessionAttributeListener) listener).attributeRemoved(event);
					} else if (old == null) {
						((HttpSessionAttributeListener) listener).attributeAdded(event);
					} else {
						((HttpSessionAttributeListener) listener).attributeReplaced(event);
					}
				}
			}
		}
	}

}
