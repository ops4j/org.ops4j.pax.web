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
package org.ops4j.pax.web.service.undertow.internal;

import java.util.EventListener;

import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.ListenerInfo;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;

public class PaxWebListenerInfo extends ListenerInfo {

	private EventListenerModel model;

	public PaxWebListenerInfo(Class<? extends EventListener> listenerClass, InstanceFactory<? extends EventListener> instanceFactory) {
		super(listenerClass, instanceFactory);
	}

	public PaxWebListenerInfo(Class<? extends EventListener> listenerClass, InstanceFactory<? extends EventListener> instanceFactory, boolean programatic) {
		super(listenerClass, instanceFactory, programatic);
	}

	public PaxWebListenerInfo(Class<? extends EventListener> listenerClass) {
		super(listenerClass);
	}

	public PaxWebListenerInfo(Class<? extends EventListener> listenerClass, boolean programatic) {
		super(listenerClass, programatic);
	}

	public EventListenerModel getModel() {
		return model;
	}

	public void setModel(EventListenerModel model) {
		this.model = model;
	}

}
