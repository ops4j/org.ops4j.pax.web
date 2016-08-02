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
package org.ops4j.pax.web.itest.base;

import org.ops4j.pax.web.service.spi.WebEvent;
import org.ops4j.pax.web.service.spi.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebListenerImpl implements WebListener {

	protected Logger log = LoggerFactory.getLogger(getClass());

	private boolean event;

	public void webEvent(WebEvent webEvent) {
		log.info("Got event: " + webEvent);
		if (webEvent.getType() == WebEvent.DEPLOYED) {
			this.event = true;
		} else if (webEvent.getType() == WebEvent.UNDEPLOYED) {
			this.event = false;
		}
	}

	public boolean gotEvent() {
		return event;
	}

}