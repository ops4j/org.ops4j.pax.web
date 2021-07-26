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
package org.ops4j.pax.web.samples.vaadin08;

import org.springframework.boot.autoconfigure.websocket.WebSocketContainerCustomizer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.stereotype.Component;

@Component("websocketContainerCustomizer")
public class NullWebSocketContainerCustomizer extends WebSocketContainerCustomizer<EmbeddedServletContainerFactory> {

    @Override
    protected void doCustomize(EmbeddedServletContainerFactory container) {
        // do nothing - Spring Boot inside the WAB detects Jetty/Tomcat/Undertow, but we manage the configuration
    }

}
