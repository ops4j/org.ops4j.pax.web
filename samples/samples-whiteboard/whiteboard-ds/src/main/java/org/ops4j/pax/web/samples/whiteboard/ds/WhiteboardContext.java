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

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

@Component(
		service = { ServletContextHelper.class, WhiteboardContext.class }, // WhiteboardContext only for testing
		scope = ServiceScope.BUNDLE,
		property = {
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH + "=/context",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=CustomContext"
		})
public class WhiteboardContext extends ServletContextHelper {

	// without passing "up" the bundle, getResource() would return null, so default resource servlet would not work!

	@Activate
	public WhiteboardContext(BundleContext context) {
		super(context.getBundle());
	}

}
