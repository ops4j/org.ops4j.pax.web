/*
 * Copyright 2008 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.whiteboard.runtime;

import java.util.EventListener;

import org.ops4j.pax.web.service.whiteboard.ListenerMapping;

/**
 * Default implementation of {@link ListenerMapping}.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class DefaultListenerMapping extends AbstractContextRelated implements ListenerMapping {

	/** Actual Listener. */
	private EventListener listener;

	@Override
	public EventListener getListener() {
		return listener;
	}

	/**
	 * Setter.
	 *
	 * @param listener mapped listener
	 */
	public void setListener(final EventListener listener) {
		this.listener = listener;
	}

	@Override
	public String toString() {
		return "DefaultListenerMapping{listener=" + listener + "}";
	}

}
