/*
 * Copyright 2020 ops4j
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
package org.ops4j.pax.web.service.spi.task;

import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;

public class EventListenerModelChange extends Change {

    private final ServerModel serverModel;
    private EventListenerModel eventListenerModel;
    private boolean disabled;

    public EventListenerModelChange(OpCode op, ServerModel serverModel, EventListenerModel eventListenerModel) {
        this(op, serverModel, eventListenerModel, false);
    }

    public EventListenerModelChange(OpCode op, ServerModel serverModel, EventListenerModel eventListenerModel, boolean disabled) {
        super(op);
        this.serverModel = serverModel;
        this.eventListenerModel = eventListenerModel;
        this.disabled = disabled;
    }

    public ServerModel getServerModel() {
        return serverModel;
    }

    public EventListenerModel getEventListenerModel() {
        return eventListenerModel;
    }

    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public void accept(BatchVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return getKind() + ": " + eventListenerModel + (disabled ? " (disabled)" : " (enabled)");
    }
    
}
