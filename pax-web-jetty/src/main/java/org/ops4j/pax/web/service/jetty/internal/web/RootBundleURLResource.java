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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;

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
	public boolean isContainedIn(Resource r) throws MalformedURLException {
		return delegate.isContainedIn(r);
	}

	@Override
	public void close() {
		delegate.close();
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
	public long lastModified() {
		return 0;
	}

	@Override
	public long length() {
		return 0;
	}

	@Override
	public URL getURL() {
		return delegate.getURL();
	}

	@Override
	public File getFile() throws IOException {
		return delegate.getFile();
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public ReadableByteChannel getReadableByteChannel() throws IOException {
		return null;
	}

	@Override
	public boolean delete() throws SecurityException {
		return false;
	}

	@Override
	public boolean renameTo(Resource dest) throws SecurityException {
		return false;
	}

	@Override
	public String[] list() {
		return new String[0];
	}

	@Override
	public Resource addPath(String path) throws IOException, MalformedURLException {
		return delegate.addPath(path);
	}

}
