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
package org.ops4j.pax.web.service.spi.model;

import java.util.EventListener;
import java.util.Objects;

import org.ops4j.lang.NullArgumentException;

public class EventListenerModel extends Model {

	private final EventListener eventListener;

	public EventListenerModel(final ContextModel contextModel,
							  final EventListener eventListener) {
		super(contextModel);
		NullArgumentException.validateNotNull(eventListener, "Listener");
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EventListenerModel that = (EventListenerModel) o;
		return Objects.equals(eventListener, that.eventListener) &&
				Objects.equals(getContextModel().getContextName(), that.getContextModel().getContextName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(eventListener, getContextModel().getContextName());
	}

}