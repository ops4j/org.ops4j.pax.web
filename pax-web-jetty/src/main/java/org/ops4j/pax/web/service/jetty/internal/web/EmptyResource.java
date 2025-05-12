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
package org.ops4j.pax.web.service.jetty.internal.web;

import java.net.URI;
import java.nio.file.Path;

import org.eclipse.jetty.util.resource.Resource;

public class EmptyResource extends Resource {

	@Override
	public Path getPath() {
		return null;
	}

	@Override
	public boolean isContainedIn(Resource r) {
		return false;
	}

	@Override
	public boolean isDirectory() {
		return false;
	}

	@Override
	public boolean isReadable() {
		return false;
	}

	@Override
	public URI getURI() {
		return null;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public String getFileName() {
		return null;
	}

	@Override
	public Resource resolve(String subUriPath) {
		return null;
	}

}
