/*
 * Copyright 2020 OPS4J.
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
package org.ops4j.pax.web.service.internal.views;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.task.Batch;
import org.ops4j.pax.web.service.views.PaxWebContainerView;
import org.osgi.framework.Bundle;

/**
 * <p>A {@link PaxWebContainerView view} that allows post-processing of existing contexts. Used by HTTP context
 * processing to configure security and context param using Config Admin.</p>
 */
public interface ProcessingWebContainerView extends PaxWebContainerView {

	/**
	 * Allows to obtain highest ranked bundle-scoped (if {@code bundle} is not null) or shared (if {@code bundle}
	 * is null) {@link OsgiContextModel} with specific ID.
	 * @param bundle
	 * @param contextId
	 * @return
	 */
	OsgiContextModel getContextModel(Bundle bundle, String contextId);

	/**
	 * Configuration specified via Configuration Admin (as of 2022-04-06) contains only login config, security
	 * constraints, security roles and context params. However any configuration may be passed using a generic
	 * "sendBatch" operation.
	 *
	 * @param batch
	 */
	void sendBatch(Batch batch);

}
