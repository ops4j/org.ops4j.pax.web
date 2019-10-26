/* Copyright 2013 Guillaume Nodet.
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
package org.ops4j.pax.web.service.jetty.internal;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.*;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.component.Container;
import org.junit.Test;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.http.HttpContext;

public class HandlerDestructionTest {

	//CHECKSTYLE:OFF
	@Test
	public void testHandler() throws Exception {
		ServerModel serverModel = new ServerModel();
		JettyServerImpl server = new JettyServerImpl(serverModel, null);
		server.start();

		TestListener listener = new TestListener();
		JettyServerWrapper container = server.getServer();
		container.addBean(listener);

		WebContainerContext httpContext = new WebContainerContext() {
			@Override
			public Set<String> getResourcePaths(String name) {
				return Collections.emptySet();
			}

			@Override
			public String getContextId() {
				return "test";
			}

			public boolean handleSecurity(HttpServletRequest request,
										  HttpServletResponse response) throws IOException {
				return false;
			}

			public URL getResource(String name) {
				return null;
			}

			public String getMimeType(String name) {
				return null;
			}
		};

		Bundle testBundle = new Bundle() {

			@Override
			public int compareTo(Bundle arg0) {
				return 0;
			}

			@Override
			public int getState() {
				return 0;
			}

			@Override
			public void start(int options) throws BundleException {

			}

			@Override
			public void start() throws BundleException {

			}

			@Override
			public void stop(int options) throws BundleException {

			}

			@Override
			public void stop() throws BundleException {

			}

			@Override
			public void update(InputStream input) throws BundleException {

			}

			@Override
			public void update() throws BundleException {

			}

			@Override
			public void uninstall() throws BundleException {

			}

			@Override
			public Dictionary<String, String> getHeaders() {
				Dictionary<String, String> dict = new Hashtable<>();
				dict.put(Constants.BUNDLE_VERSION, "1.0.0");
				return dict;
			}

			@Override
			public long getBundleId() {
				return 0;
			}

			@Override
			public String getLocation() {
				return null;
			}

			@Override
			public ServiceReference<?>[] getRegisteredServices() {
				return null;
			}

			@Override
			public ServiceReference<?>[] getServicesInUse() {
				return null;
			}

			@Override
			public boolean hasPermission(Object permission) {
				return false;
			}

			@Override
			public URL getResource(String name) {
				return null;
			}

			@Override
			public Dictionary<String, String> getHeaders(String locale) {
				return null;
			}

			@Override
			public String getSymbolicName() {
				return "HandlerDestructorTest-SymbolicNameBundle";
			}

			@Override
			public Class<?> loadClass(String name)
					throws ClassNotFoundException {
				return null;
			}

			@Override
			public Enumeration<URL> getResources(String name)
					throws IOException {
				return null;
			}

			@Override
			public Enumeration<String> getEntryPaths(String path) {
				return null;
			}

			@Override
			public URL getEntry(String path) {
				return null;
			}

			@Override
			public long getLastModified() {
				return 0;
			}

			@Override
			public Enumeration<URL> findEntries(String path,
												String filePattern, boolean recurse) {
				return null;
			}

			@Override
			public BundleContext getBundleContext() {
				return new BundleContext() {

					@Override
					public boolean ungetService(
							ServiceReference<?> reference) {
						return false;
					}

					@Override
					public void removeServiceListener(
							ServiceListener listener) {

					}

					@Override
					public void removeFrameworkListener(
							FrameworkListener listener) {

					}

					@Override
					public void removeBundleListener(BundleListener listener) {

					}

					@Override
					public <S> ServiceRegistration<S> registerService(
							Class<S> clazz, S service,
							Dictionary<String, ?> properties) {
						return null;
					}

					@Override
					public ServiceRegistration<?> registerService(
							String clazz, Object service,
							Dictionary<String, ?> properties) {
						return null;
					}

					@Override
					public ServiceRegistration<?> registerService(
							String[] clazzes, Object service,
							Dictionary<String, ?> properties) {
						return null;
					}

					@Override
					public Bundle installBundle(String location,
												InputStream input) throws BundleException {
						return null;
					}

					@Override
					public Bundle installBundle(String location)
							throws BundleException {
						return null;
					}

					@Override
					public <S> Collection<ServiceReference<S>> getServiceReferences(
							Class<S> clazz, String filter)
							throws InvalidSyntaxException {
						return null;
					}

					@Override
					public ServiceReference<?>[] getServiceReferences(
							String clazz, String filter)
							throws InvalidSyntaxException {
						return null;
					}

					@Override
					public <S> ServiceReference<S> getServiceReference(
							Class<S> clazz) {
						return null;
					}

					@Override
					public ServiceReference<?> getServiceReference(
							String clazz) {
						return null;
					}

					@Override
					public <S> S getService(ServiceReference<S> reference) {
						return null;
					}

					@Override
					public String getProperty(String key) {
						return null;
					}

					@Override
					public File getDataFile(String filename) {
						return null;
					}

					@Override
					public Bundle[] getBundles() {
						return null;
					}

					@Override
					public Bundle getBundle(String location) {
						return null;
					}

					@Override
					public Bundle getBundle(long id) {
						return null;
					}

					@Override
					public Bundle getBundle() {
						return null;
					}

					@Override
					public ServiceReference<?>[] getAllServiceReferences(
							String clazz, String filter)
							throws InvalidSyntaxException {
						return null;
					}

					@Override
					public Filter createFilter(String filter)
							throws InvalidSyntaxException {
						return null;
					}

					@Override
					public void addServiceListener(
							ServiceListener listener, String filter)
							throws InvalidSyntaxException {

					}

					@Override
					public void addServiceListener(ServiceListener listener) {

					}

					@Override
					public void addFrameworkListener(
							FrameworkListener listener) {

					}

					@Override
					public void addBundleListener(BundleListener listener) {

					}

					@Override
					public <S> ServiceRegistration<S> registerService(Class<S> clazz, ServiceFactory<S> factory,
																	  Dictionary<String, ?> properties) {
						return null;
					}

					@Override
					public <S> ServiceObjects<S> getServiceObjects(ServiceReference<S> reference) {
						return null;
					}
				};
			}

			@Override
			public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(
					int signersType) {
				return null;
			}

			@Override
			public Version getVersion() {
				return null;
			}

			@Override
			public <A> A adapt(Class<A> type) {
				return null;
			}

			@Override
			public File getDataFile(String filename) {
				return null;
			}
		};
		{
			Servlet servlet = new DefaultServlet();
			ContextModel contextModel = new ContextModel(httpContext,
					testBundle, getClass().getClassLoader(), null);
			ServletModel servletModel = new ServletModel(contextModel, servlet,
					"/", null, null, null);
			server.addServlet(servletModel);
			server.removeServlet(servletModel);
		}

		final Set<Object> oldbeans = new HashSet<>(container.getBeans());

		Servlet servlet = new DefaultServlet();
		ContextModel contextModel = new ContextModel(httpContext,
				testBundle, getClass().getClassLoader(), null);
		ServletModel servletModel = new ServletModel(contextModel, servlet,
				"/", null, null, null);
		server.addServlet(servletModel);

		// assertNotSame("OldSize:"+oldbeans.size()+" new size: "+container.getBeans().size(),
		// oldbeans.size(), container.getBeans().size());

		server.removeServlet(servletModel);

		// System.out.println(listener.diff(oldbeans));

		assertEquals(oldbeans.size(), container.getBeans().size());

	}
	//CHECKSTYLE:OFF

	/*
	 * private static String format(Container.Relationship relationship) {
	 * return relationship.getParent() + "---" + relationship.getRelationship()
	 * + "-->" + relationship.getChild(); }
	 */
	static class TestListener implements Container.Listener {

		final WeakHashMap<Object, String> beans = new WeakHashMap<>();

		@Override
		public void beanAdded(Container parent, Object bean) {
			System.out.println("Adding bean " + bean);
			beans.put(bean, bean.toString());
		}

		@Override
		public void beanRemoved(Container parent, Object bean) {
			System.out.println("Removing bean " + bean);
			beans.remove(bean);
		}

		/*
		 * final WeakHashMap<Object, String> beans = new WeakHashMap<Object,
		 * String>(); final WeakHashMap<String,
		 * List<org.eclipse.jetty.util.component.Container.Relationship>>
		 * relations = new WeakHashMap<String,
		 * List<org.eclipse.jetty.util.component.Container.Relationship>>();
		 * 
		 * public void addBean(Object bean) { System.out.println("Adding bean "
		 * + bean); beans.put(bean, bean.toString()); }
		 * 
		 * public void removeBean(Object bean) {
		 * System.out.println("Removing bean " + bean); String b =
		 * beans.remove(bean); if (b != null) { List<Container.Relationship>
		 * beanRelations = relations.remove(b); if (beanRelations != null) {
		 * List<Container.Relationship> removeList = new
		 * ArrayList<Container.Relationship>( beanRelations); for
		 * (Container.Relationship relation : removeList) {
		 * relation.getContainer().update(relation.getParent(),
		 * relation.getChild(), null, relation.getRelationship(), true); } } } }
		 * 
		 * public void add(
		 * org.eclipse.jetty.util.component.Container.Relationship relationship)
		 * { System.out.println("Adding relationship " + format(relationship));
		 * String parent = beans.get(relationship.getParent()); if (parent ==
		 * null) { addBean(relationship.getParent()); parent =
		 * beans.get(relationship.getParent()); } String child =
		 * beans.get(relationship.getChild()); if (child == null) {
		 * addBean(relationship.getChild()); child =
		 * beans.get(relationship.getChild()); } if (parent != null && child !=
		 * null) { List<org.eclipse.jetty.util.component.Container.Relationship>
		 * rels = relations .get(parent); if (rels == null) { rels = new
		 * ArrayList<org.eclipse.jetty.util.component.Container.Relationship>();
		 * relations.put(parent, rels); } rels.add(relationship); } }
		 * 
		 * public void remove(
		 * org.eclipse.jetty.util.component.Container.Relationship relationship)
		 * { System.out.println("Removing relationship " +
		 * format(relationship)); String parent =
		 * beans.get(relationship.getParent()); String child =
		 * beans.get(relationship.getChild()); if (parent != null && child !=
		 * null) { List<org.eclipse.jetty.util.component.Container.Relationship>
		 * rels = relations .get(parent); if (rels != null) { for
		 * (Iterator<org.eclipse.jetty.util.component.Container.Relationship> i
		 * = rels .iterator(); i.hasNext();) {
		 * org.eclipse.jetty.util.component.Container.Relationship r = i
		 * .next(); if (relationship.equals(r) || r.getChild() == null) {
		 * i.remove(); } } } } }
		 * 
		 * public WeakHashMap<Object, String> getBeans() { return beans; }
		 * 
		 * public WeakHashMap<String,
		 * List<org.eclipse.jetty.util.component.Container.Relationship>>
		 * getRelations() { return relations; }
		 * 
		 * public String toString() { StringBuilder sb = new StringBuilder();
		 * for (Object bean : beans.keySet()) {
		 * sb.append("Bean ").append(bean).append("\n"); } for (String key :
		 * relations.keySet()) { for
		 * (org.eclipse.jetty.util.component.Container.Relationship r :
		 * relations .get(key)) {
		 * sb.append("Relation ").append(format(r)).append("\n"); } } return
		 * sb.toString(); }
		 * 
		 * public String diff(Set<Object> oldbeans) { StringBuilder sb = new
		 * StringBuilder(); for (Object bean : beans.keySet()) { if
		 * (!oldbeans.contains(bean)) {
		 * sb.append("Bean ").append(bean).append("\n"); } } for (String key :
		 * relations.keySet()) { for
		 * (org.eclipse.jetty.util.component.Container.Relationship r :
		 * relations .get(key)) { if
		 * (!oldbeans.contains(r.getChild().toString()) ||
		 * !oldbeans.contains(r.getParent().toString())) {
		 * sb.append("Relation ").append(format(r)).append("\n"); } } } return
		 * sb.toString(); }
		 */
	}
}
