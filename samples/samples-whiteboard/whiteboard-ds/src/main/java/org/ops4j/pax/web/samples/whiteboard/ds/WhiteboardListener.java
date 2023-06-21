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
package org.ops4j.pax.web.samples.whiteboard.ds;

import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
		service = { ServletRequestListener.class, WhiteboardListener.class }, // WhiteboardListener only for testing
		property = {
				HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER + "=true",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT + "=(osgi.http.whiteboard.context.name=CustomContext)"
		}
)
public class WhiteboardListener implements ServletRequestListener {

	private static final Logger LOG = LoggerFactory.getLogger(WhiteboardListener.class);

	@Override
	public void requestDestroyed(ServletRequestEvent sre) {
		LOG.info("Listener has been called on request-destruction");
	}

	@Override
	public void requestInitialized(ServletRequestEvent sre) {
		LOG.info("Listener has been called on request-initialization");
	}

}
