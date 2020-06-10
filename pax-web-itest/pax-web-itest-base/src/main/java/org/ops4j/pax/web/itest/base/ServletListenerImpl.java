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

import org.ops4j.pax.web.service.spi.ServletEvent;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServletListenerImpl implements ServletListener {
	private static final Logger LOG = LoggerFactory.getLogger(ServletListenerImpl.class);

	private boolean event;
	private boolean replay;

	private String servletName;

	public ServletListenerImpl(String servletName) {
		this.servletName = servletName;
	}

	public ServletListenerImpl() {
	}

	@Override
	public void servletEvent(ServletEvent servletEvent) {
		LOG.info("Got event: " + servletEvent);
		boolean checkServletName = servletName != null;

		boolean servletMatch = true;
		if (checkServletName) {
			servletMatch = servletName.equalsIgnoreCase(servletEvent.getServletName());
		}
		if (servletEvent.getType() == ServletEvent.State.DEPLOYED && servletMatch) {
			LOG.info("servletEventMatched with checkServletName?{}", checkServletName);
			this.event = true;
		} else if (servletEvent.getType() == ServletEvent.State.UNDEPLOYED) {
			this.event = false;
		}
		this.replay = servletEvent.isReplay();
	}

	public boolean gotEvent() {
		return event;
	}

	public boolean gotNewEvent() {
		return event && !replay;
	}

}
