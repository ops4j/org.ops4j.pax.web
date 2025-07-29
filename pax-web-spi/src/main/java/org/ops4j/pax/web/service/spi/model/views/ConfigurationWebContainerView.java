/*
 * Copyright 2025 OPS4J.
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
package org.ops4j.pax.web.service.spi.model.views;

import java.util.concurrent.Executor;

import org.ops4j.pax.web.service.views.PaxWebContainerView;

/**
 * A view through which we can access global {@link java.util.concurrent.Executor} which allows us to
 * synchronize between various areas of Pax Web (WAR, Whiteboard and HttpService)
 */
public interface ConfigurationWebContainerView extends PaxWebContainerView {

    /**
     * Returns {@link Executor} associated with the {@link org.ops4j.pax.web.service.WebContainer}
     * @return
     */
    Executor configurationExecutor();

}
