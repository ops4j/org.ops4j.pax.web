/* Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.jsp;

import java.io.IOException;
import java.net.URLClassLoader;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.jasper.servlet.JspServlet;

/**
 * Wrapper of Jasper JspServlet that knows how to deal with resources loaded
 * from osgi context.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 07, 2008
 */
public class JspServletWrapper implements Servlet {

//	/**
//	 *
//	 */
//	private static final long serialVersionUID = 1L;
//	/**
//	 * Logger.
//	 */
//	private static final Logger LOG = LoggerFactory
//			.getLogger(JspServletWrapper.class);
//	/**
//	 * Jasper Servlet.
//	 */
//	private final JspServlet jasperServlet;
//	/**
//	 * Jasper specific class loader.
//	 */
//	private final URLClassLoader jasperClassLoader;
//
//	private final String jspFile;

//	/**
//	 * Constructor that provides a custom class loader, in order to be able to
//	 * customize the behavior of Jasper with full control over the class loading
//	 * mechanism. Only advanced users will need this, most others should simply
//	 * use the other constructors that will provide a default class loader that
//	 * delegates to the bundle.
//	 *
//	 * @param jspFile
//	 * @param classLoader
//	 */
//	public JspServletWrapper(final String jspFile,
//							 final URLClassLoader classLoader) {
//		jasperServlet = new JspServlet();
//		jasperClassLoader = classLoader;
//		this.jspFile = jspFile;
//	}

//	public JspServletWrapper(final Bundle bundle, final String jspFile) {
//		this(jspFile, new JasperClassLoader(bundle,
//				JasperClassLoader.class.getClassLoader()));
//	}

//	public JspServletWrapper(final Bundle bundle) {
//		this(bundle, null);
//	}

	/**
	 * Delegates to jasper servlet with a controlled context class loader.
	 *
	 * @see JspServlet#init(ServletConfig)
	 */
	@Override
	public void init(final ServletConfig config) throws ServletException {
//		try {
//			ContextClassLoaderUtils.doWithClassLoader(jasperClassLoader,
//					new Callable<Void>() {
//
//						@Override
//						public Void call() throws Exception {
//							config.getServletContext().setAttribute(
//									org.apache.tomcat.InstanceManager.class
//											.getName(), new InstanceManager());
//							jasperServlet.init(config);
//							return null;
//						}
//
//					});
//		} catch (ServletException e) {
//			// re-thrown
//			throw e;
//			//CHECKSTYLE:OFF
//		} catch (Exception ignore) {
//			// ignored as it should never happen
//			LOG.error("Ignored exception", ignore);
//		}
//		//CHECKSTYLE:ON
	}

	/**
	 * Delegates to jasper servlet.
	 *
	 * @see JspServlet#getServletConfig()
	 */
	@Override
	public ServletConfig getServletConfig() {
//		return jasperServlet.getServletConfig();
		return null;
	}

	/**
	 * Delegates to jasper servlet with a controlled context class loader.
	 *
	 * @see JspServlet#service(ServletRequest, ServletResponse)
	 */
	@Override
	public void service(final ServletRequest req, final ServletResponse res)
			throws ServletException, IOException {
//		if (jspFile != null) {
//			req.setAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH, jspFile);
//		}
//		String includeRequestUri = (String) req
//				.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI);
//
//		if (includeRequestUri != null) {
//			req.removeAttribute(RequestDispatcher.INCLUDE_REQUEST_URI);
//		}

//		try {
//			ContextClassLoaderUtils.doWithClassLoader(jasperClassLoader,
//					new Callable<Void>() {
//
//						@Override
//						public Void call() throws Exception {
//							jasperServlet.service(req, res);
//							return null;
//						}
//
//					});
//		} catch (ServletException | IOException e) {
//			// re-thrown
//			throw e;
//		} catch (Exception ignore) {
//			// ignored as it should never happen
//			LOG.error("Ignored exception", ignore);
//		}
		//CHECKSTYLE:ON
	}

	/**
	 * Delegates to jasper servlet.
	 *
	 * @see JspServlet#getServletInfo()
	 */
	@Override
	public String getServletInfo() {
//		return jasperServlet.getServletInfo();
		return null;
	}

	/**
	 * Delegates to jasper servlet with a controlled context class loader.
	 *
	 * @see JspServlet#destroy()
	 */
	@Override
	public void destroy() {
//		try {
//			ContextClassLoaderUtils.doWithClassLoader(jasperClassLoader,
//					new Callable<Void>() {
//
//						@Override
//						public Void call() throws Exception {
//							jasperServlet.destroy();
//							return null;
//						}
//
//					});
//			//CHECKSTYLE:OFF
//		} catch (Exception ignore) {
//			// ignored as it should never happen
//			LOG.error("Ignored exception", ignore);
//		}
		//CHECKSTYLE:ON
	}

	/**
	 * Provides access to the embedded class loader, mostly useful for
	 * performing validation checks on the class loader in integration tests.
	 *
	 * @return the internal class loader used to dispatch to the underlying
	 * Jasper servlet.
	 */
	public URLClassLoader getClassLoader() {
//		return jasperClassLoader;
		return null;
	}
}
