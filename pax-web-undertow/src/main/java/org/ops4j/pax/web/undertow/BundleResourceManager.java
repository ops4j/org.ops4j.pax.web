/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.ops4j.pax.web.undertow;

import io.undertow.UndertowMessages;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.handlers.resource.URLResource;

import java.io.IOException;
import java.net.URL;

import org.osgi.framework.Bundle;

/**
 * @author Stuart Douglas
 */
public class BundleResourceManager implements ResourceManager {
    
    private final Bundle bundle;

    /**
     * The prefix that is appended to resources that are to be loaded.
     */
    private final String prefix;

    public BundleResourceManager(final Bundle bundle, final ClassLoader loader, final Package p) {
        this(bundle, loader, p.getName().replace(".", "/"));
    }

    public BundleResourceManager(final Bundle bundle, final ClassLoader classLoader, final String prefix) {
        this.bundle = bundle;
        if (prefix.equals("")) {
            this.prefix = "";
        } else if (prefix.endsWith("/")) {
            this.prefix = prefix;
        } else {
            this.prefix = prefix + "/";
        }
    }

    public BundleResourceManager(final Bundle bundle, final ClassLoader classLoader) {
        this(bundle, classLoader, "");
    }

    public BundleResourceManager(final Bundle bundle) {
        this(bundle, null, "");
    }

    @Override
    public Resource getResource(final String path) throws IOException {
        String modPath = path;
        if(modPath.startsWith("/")) {
            modPath = path.substring(1);
        }
        final String realPath = prefix + modPath;
        final URL resource = bundle.getEntry(realPath);
        if(resource == null) {
            return null;
        } else {
            return new URLResource(resource, resource.openConnection(), path);
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


    @Override
    public void close() throws IOException {
    }
}
