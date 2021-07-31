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

import java.util.Map;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;

public class MimeAndLocaleMappingChange extends Change {

	private final OsgiContextModel osgiContextModel;
	private final Map<String, String> mimeMapping;
	private final Map<String, String> localeEncodingMapping;

	public MimeAndLocaleMappingChange(OpCode kind, Map<String, String> mimeMapping, Map<String, String> localeEncodingMapping, OsgiContextModel ocm) {
		super(kind);
		this.mimeMapping = mimeMapping;
		this.localeEncodingMapping = localeEncodingMapping;
		this.osgiContextModel = ocm;
	}

	public Map<String, String> getMimeMapping() {
		return mimeMapping;
	}

	public Map<String, String> getLocaleEncodingMapping() {
		return localeEncodingMapping;
	}

	public OsgiContextModel getOsgiContextModel() {
		return osgiContextModel;
	}

	@Override
	public void accept(BatchVisitor visitor) {
		visitor.visitMimeAndLocaleMappingChange(this);
	}

}
