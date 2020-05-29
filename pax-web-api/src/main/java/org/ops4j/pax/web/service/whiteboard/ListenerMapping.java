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
package org.ops4j.pax.web.service.whiteboard;

import java.util.EventListener;

import org.ops4j.pax.web.annotations.Review;

/**
 * Listener mapping.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
@Review("Not yet refactored")
public interface ListenerMapping {

	/**
	 * Getter.
	 *
	 * @return id of the http context this listener belongs to
	 */
	String getHttpContextId();

	/**
	 * Getter.
	 *
	 * @return listener
	 */
	EventListener getListener();

}