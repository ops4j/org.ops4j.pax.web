/*
 * Copyright 2014 Harald Wellmann.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.undertow.service;

import io.undertow.UndertowMessages;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.handlers.resource.URLResource;

import java.io.IOException;
import java.net.URL;

import org.osgi.service.http.HttpContext;

public class HttpContextResourceManager implements ResourceManager {

    private String prefix;
    private HttpContext context;

    public HttpContextResourceManager(String prefix, HttpContext context) {
        this.prefix = prefix;
        this.context = context;
    }

    @Override
    public void close() throws IOException {
        // empty
    }

    @Override
    public Resource getResource(String path) throws IOException {
        String mappedPath;
        if (prefix.equals("/")) {
            mappedPath = path;
        }
        else {
            mappedPath = String.format("%s%s", prefix, path);
        }
        URL url = context.getResource(mappedPath);
        if (url == null) {
            return null;
        }
        else {
            return new URLResource(url, url.openConnection(), path);
        }
    }

    @Override
    public boolean isResourceChangeListenerSupported() {
        return false;
    }

    @Override
    public void registerResourceChangeListener(ResourceChangeListener listener) {
        throw UndertowMessages.MESSAGES.resourceChangeListenerNotSupported();
    }

    @Override
    public void removeResourceChangeListener(ResourceChangeListener listener) {
        throw UndertowMessages.MESSAGES.resourceChangeListenerNotSupported();
    }

}
