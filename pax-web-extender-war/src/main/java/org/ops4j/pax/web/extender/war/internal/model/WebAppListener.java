/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.war.internal.model;

import java.util.EventListener;
import java.util.Objects;

import org.ops4j.pax.web.annotations.Review;

//import org.ops4j.lang.NullArgumentException;

/**
 * Models a listener element in web.xml.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, December 28, 2007
 */
@Review("To be refactored")
public class WebAppListener {

	/**
	 * Listener class name.
	 */
	private String listenerClass;
	/**
	 * Listener instance. This is set during registration process and set to
	 * null during unregistration.
	 */
	private EventListener listener;

	/**
	 * Getter.
	 *
	 * @return listener class name
	 */
	public String getListenerClass() {
		return listenerClass;
	}

	/**
	 * Setter.
	 *
	 * @param listenerClass value to set. Cannot be null.
	 * @throws NullArgumentException if listener class is null
	 */
	public void setListenerClass(final String listenerClass) {
//		NullArgumentException.validateNotNull(listenerClass, "Listener class");
		this.listenerClass = listenerClass;
	}

	/**
	 * Getter.
	 *
	 * @return listener
	 */
	public EventListener getListener() {
		return listener;
	}

	/**
	 * Setter.
	 *
	 * @param listener value to set
	 */
	public void setListener(final EventListener listener) {
		this.listener = listener;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		WebAppListener that = (WebAppListener) o;
		return Objects.equals(listenerClass, that.listenerClass);
	}

	@Override
	public int hashCode() {
		return Objects.hash(listenerClass);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() +
				"{" + "listenerClass=" + listenerClass +
				"}";
	}

}
