/*
 * Copyright 2009 David Conde.
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
package org.ops4j.pax.web.service;

import org.ops4j.pax.web.annotations.Review;
import org.osgi.framework.Bundle;

/**
 * {@link WebContainerContext} that can be shared between different bundles. Users may obtain such context by
 * name using {@link WebContainer#createDefaultSharedHttpContext(String)} and register different bundles for shared
 * context access.
 */
@Review("\"shared\" flag should be in WebContainerContext, new Whiteboard context (ServletContextHelper) should" +
		"be shared by default an this interface should be for \"pax-web shared contexts\"." +
		"\"sharing\" in new Whiteboard is about allowing the contribution of web elements to cross-bundle webapp." +
		"\"sharing\" in PaxWeb Whiteboard is about accessing more bundles in search for resources.")
public interface MultiBundleWebContainerContext extends WebContainerContext {

	/**
	 * Register given {@link Bundle} as a bundle using given shared <em>context</em>.
	 *
	 * @param bundle
	 * @return
	 */
	boolean registerBundle(Bundle bundle);

	/**
	 * Unrgister given {@link Bundle} as a bundle using given shared <em>context</em>.
	 *
	 * @param bundle
	 * @return
	 */
	boolean deregisterBundle(Bundle bundle);

	default boolean isShared() {
		return true;
	}

}
