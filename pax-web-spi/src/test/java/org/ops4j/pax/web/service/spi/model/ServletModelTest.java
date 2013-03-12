/* Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.spi.model;

import static org.easymock.EasyMock.createMock;

import java.util.Hashtable;

import javax.servlet.Servlet;

import org.junit.Test;
import org.osgi.service.http.HttpContext;

public class ServletModelTest {

	@Test(expected = IllegalArgumentException.class)
	public void registerServletWithNullAlias() {
		new ServletModel(new ContextModel(createMock(HttpContext.class), null,
				getClass().getClassLoader()), createMock(Servlet.class), null,
				new Hashtable<String, Object>(), null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void registerServletWithNullServlet() {
		new ServletModel(new ContextModel(createMock(HttpContext.class), null,
				getClass().getClassLoader()), null, "/test",
				new Hashtable<String, Object>(), null, null);
	}

	@Test
	public void registerServletWithNullInitParams() {
		new ServletModel(new ContextModel(createMock(HttpContext.class), null,
				getClass().getClassLoader()), createMock(Servlet.class),
				"/test", null, null, null);
	}

	@Test
	public void registerServletWithOnlySlashInAlias() {
		new ServletModel(new ContextModel(createMock(HttpContext.class), null,
				getClass().getClassLoader()), createMock(Servlet.class), "/",
				new Hashtable<String, Object>(), null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void registerServletWithEndSlashInAlias() {
		new ServletModel(new ContextModel(createMock(HttpContext.class), null,
				getClass().getClassLoader()), createMock(Servlet.class),
				"/test/", new Hashtable<String, Object>(), null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void registerServletWithoutStartingSlashInAlias() {
		new ServletModel(new ContextModel(createMock(HttpContext.class), null,
				getClass().getClassLoader()), createMock(Servlet.class),
				"test", new Hashtable<String, Object>(), null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void registerServletWithoutStartingSlashAndWithEndingSlashInAlias() {
		new ServletModel(new ContextModel(createMock(HttpContext.class), null,
				getClass().getClassLoader()), createMock(Servlet.class),
				"test/", new Hashtable<String, Object>(), null, null);
	}

	// @Test( expected = NamespaceException.class )
	// public void registerServletWithDuplicateAliasWithinTheSameRegistrations()
	// throws NamespaceException, ServletException
	// {
	// m_underTest.registerServlet(
	// "/test",
	// m_servlet,
	// new Hashtable()
	// );
	// m_underTest.registerServlet(
	// "/test",
	// m_servlet,
	// new Hashtable()
	// );
	// }
	//
	// @Test( expected = ServletException.class )
	// public void
	// registerSameServletForDifferentAliasesWithinTheSameRegistrations()
	// throws NamespaceException, ServletException
	// {
	// //execute
	// m_underTest.registerServlet(
	// "/alias1",
	// m_servlet,
	// new Hashtable()
	// );
	// m_underTest.registerServlet(
	// "/alias2",
	// m_servlet,
	// new Hashtable()
	// );
	// }
	//
	// @Test( expected = ServletException.class )
	// public void
	// registerSameServletForDifferentAliasesWithinDifferentRegistrations()
	// throws NamespaceException, ServletException
	// {
	// //execute
	// m_underTest.registerServlet(
	// "/alias1",
	// m_servlet,
	// new Hashtable()
	// );
	// m_underTest.registerServlet(
	// "/alias2",
	// m_servlet,
	// new Hashtable()
	// );
	// }
	//
	// @Test( expected = NamespaceException.class )
	// public void
	// registerServletWithDuplicateAliasWithinDifferentRegistrations()
	// throws NamespaceException, ServletException
	// {
	// // execute
	// new RegistrationsImpl( m_registrationsSet, m_context ).registerServlet(
	// "/test",
	// m_servlet,
	// new Hashtable()
	// );
	// new RegistrationsImpl( m_registrationsSet, m_context ).registerServlet(
	// "/test",
	// m_servlet,
	// new Hashtable()
	// );
	// }
	//
	// @Test( expected = NamespaceException.class )
	// public void
	// registerResourcesWithDuplicateAliasWithinTheSameRegistrations()
	// throws NamespaceException
	// {
	// m_underTest.registerResources(
	// "/test",
	// "resources"
	// );
	// m_underTest.registerResources(
	// "/test",
	// "resources"
	// );
	// }
	//
	// @Test( expected = NamespaceException.class )
	// public void
	// registerResourceWithDuplicateAliasWithinDifferentRegistrations()
	// throws NamespaceException, ServletException
	// {
	// // execute
	// new RegistrationsImpl( m_registrationsSet, m_context ).registerResources(
	// "/test",
	// "/name"
	// );
	// new RegistrationsImpl( m_registrationsSet, m_context ).registerResources(
	// "/test",
	// "/name"
	// );
	// }

}
