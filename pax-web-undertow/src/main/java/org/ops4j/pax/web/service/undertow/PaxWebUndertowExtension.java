/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.service.undertow;

import io.undertow.servlet.api.DeploymentInfo;
import org.ops4j.pax.web.service.undertow.configuration.model.UndertowConfiguration;

/**
 * <p>Extension interface that allows other bundles to alter {@link io.undertow.servlet.api.DeploymentInfo} for
 * Pax Web specific deployments.</p>
 *
 * <p>It's a bit like {@link io.undertow.servlet.ServletExtension}, but with Pax Web specifics.</p>
 */
public interface PaxWebUndertowExtension {

	/**
	 * Process the {@link DeploymentInfo} but with support from additional information related to Pax Web.
	 *
	 * @param deploymentInfo
	 * @param configuration
	 * @param support
	 */
	void handleDeployment(DeploymentInfo deploymentInfo, UndertowConfiguration configuration, UndertowSupport support);

}
