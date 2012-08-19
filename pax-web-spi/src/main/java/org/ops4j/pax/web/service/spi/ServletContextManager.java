/* Copyright 2012 Harald Wellmann
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
package org.ops4j.pax.web.service.spi;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps context paths to servlet contexts. Mainly used to store contexts after creation and 
 * before being started.
 * <p>
 * TODO Improve this design, this singleton is not nice. Turn it into a service, merge with
 * WarManager, or find a completely different solution.
 * 
 * @author Harald Wellmann
 */
public class ServletContextManager {
	
	/** Prevent instantiation. */
	private ServletContextManager() {
		// empty
	}
	
	/**
	 * Maps context paths (starting with a "/") to servlet contexts.
	 */
	public static Map<String, ServletContextWrapper> contextMap = new HashMap<String, ServletContextWrapper>();

	public static synchronized void startContext(String contextPath) {
		contextMap.get(contextPath).start();
	}

	public static synchronized void stopContext(String contextPath) {
		contextMap.get(contextPath).stop();		
	}
	
	public static synchronized void addContext(String contextPath, ServletContextWrapper wrapper) {
		contextMap.put(contextPath, wrapper);
	}

	public static synchronized void removeContext(String contextPath) {
		contextMap.remove(contextPath);
	}
	
	
	/** 
	 * Wraps a servlet context. Implementations of this class shall wrap a container specific
	 * delegate.
	 * 
	 * @author Harald Wellmann
	 */
	public interface ServletContextWrapper {
		
		/** Starts the wrapped context. */
		void start();

		/** Stops the wrapped context. */
		void stop();
	}
}
