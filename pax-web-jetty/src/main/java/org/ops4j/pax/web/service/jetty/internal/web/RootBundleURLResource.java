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
import java.time.Instant;

import org.eclipse.jetty.util.resource.Resource;

/**
 * Special wrapper for {@link Resource} to handle URLs representing roots of the bundles.
 */
public class RootBundleURLResource extends Resource {

	private final Resource delegate;

	public RootBundleURLResource(Resource delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean isContainedIn(Resource r) {
		return delegate.isContainedIn(r);
	}

	@Override
	public boolean exists() {
		// root of the bundle always exists
		return true;
	}

	@Override
	public boolean isDirectory() {
		return true;
	}

	@Override
	public Instant lastModified() {
		return Instant.ofEpochMilli(0);
	}

	@Override
	public long length() {
		return 0;
	}

	@Override
	public URI getURI() {
		return delegate.getURI();
	}

	@Override
	public Path getPath() {
		return delegate.getPath();
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public boolean isReadable() {
		return true;
	}

	@Override
	public String getFileName() {
		return delegate.getFileName();
	}

	@Override
	public Resource resolve(String subUriPath) {
		return delegate.resolve(subUriPath);
	}

}
