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

import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.component.Container;
import org.junit.Test;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.osgi.service.http.HttpContext;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class HandlerDestructionTest {

	@Test
	public void testHandler() throws Exception {
		ServerModel serverModel = new ServerModel();
		JettyServerImpl server = new JettyServerImpl(serverModel, null);
		server.start();

		TestListener listener = new TestListener();
		server.getServer().getContainer().addEventListener(listener);

		HttpContext httpContext = new HttpContext() {
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

		{
			Servlet servlet = new DefaultServlet();
			ContextModel contextModel = new ContextModel(httpContext, null,
					getClass().getClassLoader());
			ServletModel servletModel = new ServletModel(contextModel, servlet,
					"/", null, null, null);
			server.addServlet(servletModel);
			server.removeServlet(servletModel);
		}

		final Set<Object> oldbeans = new HashSet<Object>(listener.getBeans()
				.values());

		Servlet servlet = new DefaultServlet();
		ContextModel contextModel = new ContextModel(httpContext, null,
				getClass().getClassLoader());
		ServletModel servletModel = new ServletModel(contextModel, servlet,
				"/", null, null, null);
		server.addServlet(servletModel);

		assertNotSame(oldbeans.size(), listener.getBeans().size());

		server.removeServlet(servletModel);

		System.out.println(listener.diff(oldbeans));

		assertEquals(oldbeans.size(), listener.getBeans().size());

	}

	private static String format(Container.Relationship relationship) {
		return relationship.getParent() + "---"
				+ relationship.getRelationship() + "-->"
				+ relationship.getChild();
	}

	static class TestListener implements Container.Listener {

		final WeakHashMap<Object, String> beans = new WeakHashMap<Object, String>();
		final WeakHashMap<String, List<org.eclipse.jetty.util.component.Container.Relationship>> relations = new WeakHashMap<String, List<org.eclipse.jetty.util.component.Container.Relationship>>();

		public void addBean(Object bean) {
			System.out.println("Adding bean " + bean);
			beans.put(bean, bean.toString());
		}

		public void removeBean(Object bean) {
			System.out.println("Removing bean " + bean);
			String b = beans.remove(bean);
			if (b != null) {
				List<Container.Relationship> beanRelations = relations.remove(b);
				if (beanRelations != null) {
					List<Container.Relationship> removeList = new ArrayList<Container.Relationship>(
							beanRelations);
					for (Container.Relationship relation : removeList) {
						relation.getContainer().update(relation.getParent(),
								relation.getChild(), null,
								relation.getRelationship(), true);
					}
				}
			}
		}

		public void add(
				org.eclipse.jetty.util.component.Container.Relationship relationship) {
			System.out.println("Adding relationship " + format(relationship));
			String parent = beans.get(relationship.getParent());
			if (parent == null) {
				addBean(relationship.getParent());
				parent = beans.get(relationship.getParent());
			}
			String child = beans.get(relationship.getChild());
			if (child == null) {
				addBean(relationship.getChild());
				child = beans.get(relationship.getChild());
			}
			if (parent != null && child != null) {
				List<org.eclipse.jetty.util.component.Container.Relationship> rels = relations
						.get(parent);
				if (rels == null) {
					rels = new ArrayList<org.eclipse.jetty.util.component.Container.Relationship>();
					relations.put(parent, rels);
				}
				rels.add(relationship);
			}
		}

		public void remove(
				org.eclipse.jetty.util.component.Container.Relationship relationship) {
			System.out.println("Removing relationship " + format(relationship));
			String parent = beans.get(relationship.getParent());
			String child = beans.get(relationship.getChild());
			if (parent != null && child != null) {
				List<org.eclipse.jetty.util.component.Container.Relationship> rels = relations
						.get(parent);
				if (rels != null) {
					for (Iterator<org.eclipse.jetty.util.component.Container.Relationship> i = rels
							.iterator(); i.hasNext();) {
						org.eclipse.jetty.util.component.Container.Relationship r = i
								.next();
						if (relationship.equals(r) || r.getChild() == null) {
							i.remove();
						}
					}
				}
			}
		}

		public WeakHashMap<Object, String> getBeans() {
			return beans;
		}

		public WeakHashMap<String, List<org.eclipse.jetty.util.component.Container.Relationship>> getRelations() {
			return relations;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (Object bean : beans.keySet()) {
				sb.append("Bean ").append(bean).append("\n");
			}
			for (String key : relations.keySet()) {
				for (org.eclipse.jetty.util.component.Container.Relationship r : relations
						.get(key)) {
					sb.append("Relation ").append(format(r)).append("\n");
				}
			}
			return sb.toString();
		}

		public String diff(Set<Object> oldbeans) {
			StringBuilder sb = new StringBuilder();
			for (Object bean : beans.keySet()) {
				if (!oldbeans.contains(bean)) {
					sb.append("Bean ").append(bean).append("\n");
				}
			}
			for (String key : relations.keySet()) {
				for (org.eclipse.jetty.util.component.Container.Relationship r : relations
						.get(key)) {
					if (!oldbeans.contains(r.getChild().toString())
							|| !oldbeans.contains(r.getParent().toString())) {
						sb.append("Relation ").append(format(r)).append("\n");
					}
				}
			}
			return sb.toString();
		}
	}
}
