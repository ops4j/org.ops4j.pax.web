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
package org.ops4j.pax.web.service.spi.task;

import org.ops4j.pax.web.service.spi.model.ContextMetadataModel;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;

public class ContextMetadataModelChange extends Change {

	private final ContextMetadataModel metadata;
	private final OsgiContextModel osgiContextModel;

	public ContextMetadataModelChange(OpCode kind, ContextMetadataModel meta, OsgiContextModel ocm) {
		super(kind);
		this.metadata = meta;
		this.osgiContextModel = ocm;
	}

	public ContextMetadataModel getMetadata() {
		return metadata;
	}

	public OsgiContextModel getOsgiContextModel() {
		return osgiContextModel;
	}

	@Override
	public void accept(BatchVisitor visitor) {
		visitor.visitContextMetadataModelChange(this);
	}

}
