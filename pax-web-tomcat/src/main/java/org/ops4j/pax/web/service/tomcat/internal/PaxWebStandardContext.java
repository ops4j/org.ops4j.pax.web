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
package org.ops4j.pax.web.service.tomcat.internal;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.Container;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.valves.ValveBase;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.servlet.Default404Servlet;
import org.ops4j.pax.web.service.spi.servlet.OsgiFilterChain;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
import org.ops4j.pax.web.service.spi.servlet.SCIWrapper;
import org.osgi.service.http.whiteboard.Preprocessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of {@link StandardContext} that keeps track of default
 * {@link org.ops4j.pax.web.service.spi.model.OsgiContextModel} and
 * {@link javax.servlet.ServletContext} to use for chains that do not have target servlet mapped. These are
 * required by filters which may be associated with such servlet-less chains.
 */
public class PaxWebStandardContext extends StandardContext {

	public static final Logger LOG = LoggerFactory.getLogger(PaxWebStandardContext.class);

	/** Name of an attribute that indicates a {@link PaxWebStandardContext} for given request processing */
	public static final String PAXWEB_STANDARD_CONTEXT = ".paxweb.standard.context";
	/** Name of an attribute that indicates a {@link PaxWebStandardWrapper} for given request processing */
	public static final String PAXWEB_STANDARD_WRAPPER = ".paxweb.standard.wrapper";

	/** Default {@link ServletContext} to use for chains without target servlet (e.g., filters only) */
	private OsgiServletContext defaultServletContext;
	/** Default {@link OsgiContextModel} to use for chains without target servlet (e.g., filters only) */
	private OsgiContextModel defaultOsgiContextModel;
	/** Default {@link WebContainerContext} for chains without target {@link Servlet} */
	private WebContainerContext defaultWebContainerContext;

	private String osgiInitFilterName;

	// TODO: these are kept, so we can replace the active context and preprocessors

	private PaxWebFilterMap osgiInitFilterMap;
	private PaxWebFilterDef osgiInitFilterDef;

	/** Highest ranked {@link OsgiServletContext} set when Tomcat's context starts */
	private OsgiServletContext osgiServletContext;

	/**
	 * {@link Preprocessor} are registered as filters, but without particular target
	 * {@link org.ops4j.pax.web.service.spi.servlet.OsgiServletContext}, so they're effectively registered in
	 * all available physical servlet contexts.
	 */
	private final List<Preprocessor> preprocessors = new LinkedList<>();

	private final Collection<SCIWrapper> servletContainerInitializers = new LinkedList<>();

	public PaxWebStandardContext(Default404Servlet defaultServlet) {
		super();
		getPipeline().addValve(new PaxWebStandardContextValve((ValveBase) getPipeline().getBasic(), defaultServlet));
	}

	/**
	 * Called just after creation of this {@link StandardContext} to add first filter that will handle OSGi specifics.
	 * Due to Tomcat's usage of static and final methods, it's really far from beautiful code.
	 */
	public void createInitialOsgiFilter() {
		// turn a chain into a filter - to satisfy Tomcat's static methods
		Filter osgiInitFilter = (request, response, chain) -> {
			// this is definitiely the first filter, so we should get these attributes
			PaxWebStandardContext delegate = PaxWebStandardContext.this;
			PaxWebStandardWrapper wrapper = (PaxWebStandardWrapper) request.getAttribute(PAXWEB_STANDARD_WRAPPER);
			request.removeAttribute(PAXWEB_STANDARD_WRAPPER);

			if (wrapper == null) {
				Container[] children = PaxWebStandardContext.this.findChildren();
				for (Container c : children) {
					if (c instanceof PaxWebStandardWrapper && request instanceof HttpServletRequest
							&& c.getName() != null
							&& c.getName().equals(((HttpServletRequest) request).getHttpServletMapping().getServletName())) {
						wrapper = (PaxWebStandardWrapper) c;
					}
				}
			}

			final OsgiFilterChain osgiChain;
			if (wrapper != null && !wrapper.is404()) {
				osgiChain = new OsgiFilterChain(delegate.getPreprocessors(),
						wrapper.getServletContext(), wrapper.getWebContainerContext(), null);
			} else {
				osgiChain = new OsgiFilterChain(delegate.getPreprocessors(),
						delegate.getDefaultServletContext(), delegate.getDefaultWebContainerContext(), null);
			}

			// this chain will be called (or not)
			osgiChain.setChain(chain);
			osgiChain.doFilter(request, response);
		};

		FilterModel filterModel = new FilterModel("__osgi@" + System.identityHashCode(osgiInitFilter),
				new String[] { "*" }, null, null, osgiInitFilter, null, true);
		filterModel.getMappingsPerDispatcherTypes().get(0).setDispatcherTypes(new DispatcherType[] {
				DispatcherType.ERROR,
				DispatcherType.FORWARD,
				DispatcherType.INCLUDE,
				DispatcherType.REQUEST,
				DispatcherType.ASYNC
		});
		osgiInitFilterDef = new PaxWebFilterDef(filterModel, true, null);
		osgiInitFilterMap = new PaxWebFilterMap(filterModel, true);

		addFilterDef(osgiInitFilterDef);
		addFilterMapBefore(osgiInitFilterMap);
	}

	/**
	 * This method may be called long after initial filter was created. In Jetty and Undertow there's no
	 * <em>initial</em> filter, because we can do it better, but with Tomcat we have to do it like this.
	 * @param defaultServletContext
	 */
	public void setDefaultServletContext(OsgiServletContext defaultServletContext) {
		// TODO: release previous WebContainerContext
		this.defaultServletContext = defaultServletContext;
		if (defaultServletContext != null) {
			this.defaultWebContainerContext = defaultOsgiContextModel.resolveHttpContext(defaultOsgiContextModel.getOwnerBundle());
		}
	}

	/**
	 * We have to ensure that this {@link StandardContext} will always return
	 * proper instance of {@link javax.servlet.ServletContext} - especially in the events passed to listeners
	 * @param osgiServletContext
	 */
	public void setOsgiServletContext(OsgiServletContext osgiServletContext) {
		this.osgiServletContext = osgiServletContext;
	}

	@Override
	public void addServletContainerInitializer(ServletContainerInitializer sci, Set<Class<?>> classes) {
		// we don't want initializers in Tomcat's context, because we manage them ourselves
	}

	public void setServletContainerInitializers(Collection<SCIWrapper> wrappers) {
		this.servletContainerInitializers.clear();
		this.servletContainerInitializers.addAll(wrappers);
	}

	@Override
	public ServletContext getServletContext() {
		if (osgiServletContext != null) {
			return osgiServletContext;
		}
		return super.getServletContext();
	}

	@Override
	public boolean listenerStart() {
		// This is a method overriden JUST because it is invoked just after original
		// org.apache.catalina.core.StandardContext.startInternal() invokes ServletContainerInitializers.
		// We have to call SCIs ourselves to pass better OsgiServletContext there.

		// I know listenerStart() is NOT the method which should invoke SCIs, but hey - we want to stay as consistent
		// between Jetty, Tomcat and Undertow in Pax Web as possible

		boolean ok = true;
		for (SCIWrapper wrapper : servletContainerInitializers) {
			ClassLoader tccl = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(getParentClassLoader());
				wrapper.onStartup();
			} catch (ServletException e) {
				LOG.error(sm.getString("standardContext.sciFail"), e);
				ok = false;
			} finally {
				Thread.currentThread().setContextClassLoader(tccl);
			}
		}

		if (ok) {
			return super.listenerStart();
		}

		return false;
	}

	public void setDefaultOsgiContextModel(OsgiContextModel defaultOsgiContextModel) {
		this.defaultOsgiContextModel = defaultOsgiContextModel;
	}

	public OsgiServletContext getDefaultServletContext() {
		return defaultServletContext;
	}

	public OsgiContextModel getDefaultOsgiContextModel() {
		return defaultOsgiContextModel;
	}

	public WebContainerContext getDefaultWebContainerContext() {
		return defaultWebContainerContext;
	}

	public List<Preprocessor> getPreprocessors() {
		return this.preprocessors;
	}

}
