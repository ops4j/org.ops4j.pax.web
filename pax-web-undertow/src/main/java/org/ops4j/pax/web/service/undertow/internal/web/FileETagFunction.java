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
package org.ops4j.pax.web.service.undertow.internal.web;

import java.nio.file.Path;

import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.util.ETag;

public class FileETagFunction implements PathResourceManager.ETagFunction {

	@Override
	public ETag generate(Path path) {
		return new ETag(true, path.toFile().length() + "-" + path.toFile().lastModified());
	}

}
