/*
 * Copyright 2013 Harald Wellmann
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.ops4j.pax.web.service.spi.util;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.ops4j.pax.swissbox.core.BundleClassLoader;
import org.osgi.framework.Bundle;

/**
 * A bundle class loader which delegates resource loading to a list
 * of delegate bundles.
 * 
 * @author Harald Wellmann
 */
public class ResourceDelegatingBundleClassLoader extends BundleClassLoader {

  private List<Bundle> bundles;

  public ResourceDelegatingBundleClassLoader(List<Bundle> bundles) {
    super(bundles.get(0));
    this.bundles = bundles;
  }

  protected URL findResource(String name) {
    for (Bundle delegate : bundles) {
      try {
        URL resource = delegate.getResource(name);
        if (resource != null) {
          return resource;
        }
      } catch (IllegalStateException exc) {
        // ignore
      }
    }
    return null;
  }

  @Override
  protected Enumeration<URL> findResources(String name) throws IOException {
    Vector<URL> resources = new Vector<URL>();

    for (Bundle delegate : bundles) {
      try {
        Enumeration<URL> urls = delegate.getResources(name);
        if (urls != null) {
          while (urls.hasMoreElements()) {
            resources.add(urls.nextElement());
          }
        }
      } catch (IllegalStateException exc) {
        // ignore
      }
    }

    return resources.elements();
  }
}