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
package org.ops4j.pax.web.service.spi.servlet;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.ServletContext;

import org.osgi.framework.Bundle;

/**
 * <p>A {@link ClassLoader} added in Pax Web to replace all pax-swissbox/xbean <em>bundle classloaders</em> and to be used
 * as <em>the</em> {@link ClassLoader} for {@link OsgiServletContext}.</p>
 *
 * <p>In Whiteboard scenario, an {@link OsgiServletContext} should get a {@link ClassLoader} from a bundle that
 * was used to register {@link org.osgi.service.http.context.ServletContextHelper} OSGi service and in case of
 * servlets (and filters), the {@link javax.servlet.ServletContext} obtained from {@link javax.servlet.ServletConfig}
 * should return (in {@link ServletContext#getClassLoader()}) the {@link ClassLoader} of a bundle associated with
 * the {@link Bundle} that registered this {@link javax.servlet.Servlet} or {@link javax.servlet.Filter} OSGi service.</p>
 *
 * <p>In practice (and in WAB scenario), the actual {@link ClassLoader} returned from {@link OsgiServletContext} should
 * also be able to reach to resources/classes for given server runtime (Jetty, Tomcat, Undertow) and engines like JSP.</p>
 *
 * <p>To this end, When an instance of {@link OsgiServletContext} is created by server-specific
 * {@link org.ops4j.pax.web.service.spi.ServerController}, an instance of {@link OsgiServletContextClassLoader} will
 * be created and set as the {@link ClassLoader} of <em>the</em> runtime-specific {@link ServletContext}.</p>
 *
 * <p>This {@link ClassLoader} doesn't follow the delegation model - it has no parent and simply collects the resources
 * and attempts classloading from all the bundles. It never calls {@link ClassLoader#defineClass} on its own.</p>
 *
 * <p>To get the picture, here are three methods for all three supported runtimes:<ul>
 *     <li>{@code org.eclipse.jetty.server.handler.ContextHandler#setClassLoader(java.lang.ClassLoader)}</li>
 *     <li>{@code org.apache.catalina.core.ContainerBase#setParentClassLoader(java.lang.ClassLoader)}</li>
 *     <li>{@code io.undertow.servlet.api.DeploymentInfo#setClassLoader(java.lang.ClassLoader)}</li>
 * </ul></p>
 *
 * <p>By default (outside of OSGi), here are the actual classloader classes used:<ul>
 *     <li>{@code org.eclipse.jetty.webapp.WebAppClassLoader}</li>
 *     <li>{@code org.apache.catalina.loader.ParallelWebappClassLoader} and {@code org.apache.catalina.loader.WebappLoader}</li>
 *     <li>Nothing particular in plain Undertow, but it's set to {@code org.jboss.as.web.host.WebDeploymentBuilder#getClassLoader()}
 *         in Wildfly/EAP.</li>
 * </ul></p>
 */
public class OsgiServletContextClassLoader extends ClassLoader {

	private List<Bundle> bundles = new ArrayList<>();

	/**
	 * Adds a {@link Bundle} to be reachable from this {@link ClassLoader}
	 * @param bundle
	 */
	public void addBundle(Bundle bundle) {
		if (bundle != null) {
			bundles.add(bundle);
		}
	}

	/**
	 * After adding all bundles that back up this {@link ClassLoader}, this method prevents adding more bundles.
	 */
	public void makeImmutable() {
		bundles = Collections.unmodifiableList(bundles);
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		List<Exception> suppressed = new ArrayList<>(bundles.size());
		for (Bundle b : bundles) {
			if (b.getState() != Bundle.UNINSTALLED) {
				try {
					return b.loadClass(name);
				} catch (Exception e) {
					suppressed.add(e);
				}
			}
		}

		ClassNotFoundException cnfe = new ClassNotFoundException(name);
		suppressed.forEach(cnfe::addSuppressed);
		throw cnfe;
	}

	@Override
	protected URL findResource(String name) {
		for (Bundle b : bundles) {
			if (b.getState() != Bundle.UNINSTALLED) {
				URL res = b.getResource(name);
				if (res != null) {
					return res;
				}
			}
		}

		return null;
	}

	@Override
	protected Enumeration<URL> findResources(String name) throws IOException {
		List<URL> urls = new ArrayList<>(32);
		for (Bundle b : bundles) {
			if (b.getState() != Bundle.UNINSTALLED) {
				Enumeration<URL> e = b.getResources(name);
				if (e != null) {
					while (e.hasMoreElements()) {
						urls.add(e.nextElement());
					}
				}
			}
		}

		return Collections.enumeration(urls);
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		// this classloader never loads classes on its own
		throw new ClassNotFoundException(name);
	}

}
