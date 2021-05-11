/*
 * Copyright 2020 ops4j
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
package org.ops4j.pax.web.service.spi.task;

import java.util.Arrays;
import java.util.EventListener;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletContextListener;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;

public class EventListenerModelChange extends Change {

	private final List<EventListenerModel> eventListenerModels = new LinkedList<>();
	private EventListenerModel eventListenerModel;
	private final List<OsgiContextModel> newModels = new LinkedList<>();

	public EventListenerModelChange(OpCode op, EventListenerModel eventListenerModel, OsgiContextModel... newModels) {
		super(op);
		this.eventListenerModels.add(eventListenerModel);
		this.eventListenerModel = eventListenerModel;
		this.newModels.addAll(Arrays.asList(newModels));
	}

	public EventListenerModelChange(OpCode op, List<EventListenerModel> eventListenerModels) {
		super(op);
		this.eventListenerModels.addAll(eventListenerModels);
	}

	public EventListenerModel getEventListenerModel() {
		return eventListenerModel;
	}

	public List<EventListenerModel> getEventListenerModels() {
		return eventListenerModels;
	}

	public List<OsgiContextModel> getNewModels() {
		return newModels;
	}

	@Override
	public void uninstall(List<Change> operations) {
		if (getKind() == OpCode.ADD) {
			EventListener listener = eventListenerModel.getResolvedListener();
			if (listener != null) {
				if (ServletContextListener.class.isAssignableFrom(listener.getClass())) {
					// special case - we don't want such listeners to be removed, so they really get the
					// notification about context being destroyed
					// TODO: check the same for Session Listeners
					return;
				}
			}
			operations.add(new EventListenerModelChange(OpCode.DELETE, eventListenerModel));
		}
	}

	@Override
	public void accept(BatchVisitor visitor) {
		visitor.visit(this);
	}

	public List<OsgiContextModel> getContextModels() {
		return newModels.size() > 0 ? newModels : eventListenerModel.getContextModels();
	}

	@Override
	public String toString() {
		EventListenerModel model = eventListenerModel;
		if (model == null && eventListenerModels.size() == 1) {
			model = eventListenerModels.get(0);
		}
		if (model != null) {
			return getKind() + ": " + model;
		} else {
			return getKind() + ": " + eventListenerModels.size() + " event listener models";
		}
	}

}
