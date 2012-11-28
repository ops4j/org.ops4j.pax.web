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
package org.ops4j.pax.web.service.jetty.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.LazyList;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.ops4j.pax.swissbox.core.ContextClassLoaderUtils;
import org.ops4j.pax.web.service.spi.model.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.LifeCycle;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.FilterModel;
import org.ops4j.pax.web.service.spi.model.SecurityConstraintMappingModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.osgi.service.http.HttpContext;

class JettyServerImpl implements JettyServer {

	private static final Logger LOG = LoggerFactory.getLogger(JettyServerImpl.class);

	private final JettyServerWrapper m_server;

	JettyServerImpl(final ServerModel serverModel) {
		m_server = new JettyServerWrapper(serverModel);
		m_server.setThreadPool(new QueuedThreadPool());
	}

	public void start() {
		LOG.debug("Starting " + this);
		try {
			//PAXWEB-193 suggested we should open this up for external configuration
            URL jettyResource = getServerConfigURL();
            if (jettyResource == null) {
                jettyResource = getClass().getResource("/jetty.xml");
            }
            if (LOG.isDebugEnabled())
                LOG.debug("Jetty server configuration file location: " + jettyResource);

			if (jettyResource != null) {
				ClassLoader loader = Thread.currentThread().getContextClassLoader();
				try
				{
					Thread.currentThread().setContextClassLoader( getClass().getClassLoader() );
					LOG.debug("Configure using resource " + jettyResource);
					XmlConfiguration configuration = new XmlConfiguration(jettyResource);
//					configuration.configure(m_server);
					Method method = XmlConfiguration.class.getMethod("configure", Object.class);
					method.invoke(configuration, m_server);
				}
				finally
				{
					Thread.currentThread().setContextClassLoader( loader );
				}
			}
			m_server.start();
		} catch (Exception e) {
			LOG.error("Exception while starting Jetty:", e);
		}
	}

	public void stop() {
		LOG.debug("Stopping " + this);
		try {
			m_server.stop();
		} catch (Exception e) {
			LOG.error("Exception while stopping Jetty:",e);
		}
	}

	/**
	 * @see JettyServer#addConnector(org.mortbay.jetty.Connector)
	 */
	public void addConnector(final Connector connector) {
		LOG.info(String.format("Pax Web available at [%s]:[%s]",
				connector.getHost() == null ? "0.0.0.0" : connector.getHost(),
				connector.getPort()));
		m_server.addConnector(connector);
	}

	@Override
	public Connector[] getConnectors() {
		return m_server.getConnectors();
	}
	
	@Override
	public void removeConnector(final Connector connector) {
		LOG.info("Removing connection for [{}]:[{}]", 
				connector.getHost() == null ? "0.0.0.0" : connector.getHost(),
				connector.getPort());
		m_server.removeConnector(connector);
	}
	
	/**
	 * {@inheritDoc}
	 * @param userRealm
	 */
	public void configureContext(final Map<String, Object> attributes,
			final Integer sessionTimeout, final String sessionCookie,
			final String sessionUrl, final Boolean sessionCookieHttpOnly, final String workerName ) {
		m_server.configureContext(attributes, sessionTimeout, sessionCookie,
				sessionUrl, sessionCookieHttpOnly, workerName);
	}

    public LifeCycle getContext(final ContextModel model) {
        final ServletContextHandler context = m_server
                .getOrCreateContext(model);
        return new LifeCycle() {
            public void start() throws Exception {
                context.start();
            }
            public void stop() throws Exception {
                context.stop();
            }
        };
    }

	public void addServlet(final ServletModel model) {
		LOG.debug("Adding servlet [" + model + "]");
		final ServletMapping mapping = new ServletMapping();
		mapping.setServletName(model.getName());
		mapping.setPathSpecs(model.getUrlPatterns());
		final ServletContextHandler context = m_server.getOrCreateContext(model);
		final ServletHandler servletHandler = context.getServletHandler();
		if (servletHandler == null) {
			throw new IllegalStateException(
					"Internal error: Cannot find the servlet holder");
		}

		ServletHolder servletHolder = null;
		if (isLazyInitializationRequired(model)) {
			servletHolder = new ServletHolder(model.getServlet().getClass());			
		}
		else {
			servletHolder = new ServletHolder(model.getServlet());			
		}
		final ServletHolder holder = servletHolder;
		holder.setName(model.getName());
		if (model.getInitParams() != null) {
			holder.setInitParameters(model.getInitParams());
		}
		// Jetty does not set the context class loader on adding the filters so
		// we do that instead
		try {
			ContextClassLoaderUtils.doWithClassLoader(context.getClassLoader(),
					new Callable<Void>() {

						public Void call() {
							servletHandler.addServlet(holder);
							servletHandler.addServletMapping(mapping);
							return null;
						}

					});
		} catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			LOG.error("Ignored exception during servlet registration", e);
		}
	}
	
	/**
	 * Is lazy initialization required for the given servlet? This means that the servlet gets
	 * instantiated by Jetty from the class name and then gets decorated by any decorators
	 * registered by servlet container initializers or by other means. Jetty servlet decorators
	 * have no effect when the ServletHolder is created from a Servlet instance.
	 * <p>
	 * Lazy initialization is required for servlets in web applications, with the exception of Pax
	 * Web's own servlets for JSPs and resources which cannot be loaded from the TCCL.
	 * <p>
	 * TODO Find a better way than working with hard-coded class names.
	 * 
	 * @param model servlet model
	 *            .
	 * @return true if servlet needs to be initialized lazily
	 */
	private boolean isLazyInitializationRequired(ServletModel model) {
		boolean lazy = false;
		HttpContext httpContext = model.getContextModel().getHttpContext();
		boolean isWebApp = httpContext.getClass().getName().equals("org.ops4j.pax.web.extender.war.internal.WebAppWebContainerContext");
		if (isWebApp) {
			String className = model.getServlet().getClass().getName();
			if (className.equals("org.ops4j.pax.web.jsp.JspServletWrapper") 
				|| className.equals("org.ops4j.pax.web.service.jetty.internal.ResourceServlet")) {
				// not lazy
			} 
			else {
				lazy = true;
			}
		}
		return lazy;
	}

	public void removeServlet(final ServletModel model) {
		LOG.debug("Removing servlet [" + model + "]");
		// jetty does not provide a method fro removing a servlet so we have to
		// do it by our own
		// the facts bellow are found by analyzing ServletHolder implementation
		boolean removed = false;
		final ServletContextHandler context = m_server.getContext(model.getContextModel()
				.getHttpContext());
		if (context == null)
			return; //context is already removed so no need for deregistration
		
		final ServletHandler servletHandler = context.getServletHandler();
		final ServletHolder[] holders = servletHandler.getServlets();
		if (holders != null) {
			final ServletHolder holder = servletHandler.getServlet(model
					.getName());
			if (holder != null) {
				servletHandler.setServlets((ServletHolder[]) LazyList
						.removeFromArray(holders, holder));
				// we have to find the servlet mapping by hand :( as there is no
				// method provided by jetty
				// and the remove is done based on equals, that is not
				// implemented by servletmapping
				// so it is == based.
				ServletMapping[] mappings = servletHandler.getServletMappings();
				if (mappings != null) {
					ServletMapping mapping = null;
					for (ServletMapping item : mappings) {
						if (holder.getName().equals(item.getServletName())) {
							mapping = item;
							break;
						}
					}
					if (mapping != null) {
						servletHandler
								.setServletMappings((ServletMapping[]) LazyList
										.removeFromArray(mappings, mapping));
						removed = true;
					}
				}
				// if servlet is still started stop the servlet holder
				// (=servlet.destroy()) as Jetty will not do that
				if (holder.isStarted()) {
					try {
						ContextClassLoaderUtils.doWithClassLoader(
								context.getClassLoader(), new Callable<Void>() {

									public Void call() throws Exception {
										holder.stop();
										return null;
									}

								});
					} catch (Exception e) {
						if (e instanceof RuntimeException) {
							throw (RuntimeException) e;
						}
						LOG.warn("Exception during unregistering of servlet ["
								+ model + "]");
					}
				}
			}
		}
		if (servletHandler.getServlets() == null || servletHandler.getServlets().length == 0)
			removeContext(model.getContextModel().getHttpContext());
		if (!removed) {
			throw new IllegalStateException(model + " was not found");
		}
	}

	public void addEventListener(final EventListenerModel model) {
		m_server.getOrCreateContext(model).addEventListener(
				model.getEventListener());
	}

	public void removeEventListener(final EventListenerModel model) {
		final ServletContextHandler context = m_server.getContext(model.getContextModel()
				.getHttpContext());
		
		if (context == null)
			return; //Obviously context is already destroyed
		
		final List<EventListener> listeners = new ArrayList<EventListener>(
				Arrays.asList(context.getEventListeners()));
		listeners.remove(model.getEventListener());
		context.setEventListeners(listeners.toArray(new EventListener[listeners
				.size()]));
	}

	public void removeContext(final HttpContext httpContext) {
		m_server.removeContext(httpContext);
	}

	public void addFilter(final FilterModel model) {
		LOG.debug("Adding filter model [" + model + "]");
		final FilterMapping mapping = new FilterMapping();
		mapping.setFilterName(model.getName());
		if (model.getUrlPatterns() != null && model.getUrlPatterns().length > 0) {
			mapping.setPathSpecs(model.getUrlPatterns());
		}
		if (model.getServletNames() != null
				&& model.getServletNames().length > 0) {
			mapping.setServletNames(model.getServletNames());
		}
		// set-up dispatcher
        int dispatcher = FilterMapping.DEFAULT;
        for( String d : model.getDispatcher() )
        {
            dispatcher |= FilterMapping.dispatch( d ).ordinal();
        }
        mapping.setDispatches( dispatcher );

		final ServletContextHandler context = m_server.getOrCreateContext(model);
		final ServletHandler servletHandler = context.getServletHandler();
		if (servletHandler == null) {
			throw new IllegalStateException(
					"Internal error: Cannot find the servlet holder");
		}
		final FilterHolder holder = new FilterHolder(model.getFilter());
		holder.setName(model.getName());
		if (model.getInitParams() != null) {
			holder.setInitParameters(model.getInitParams());
		}

		// Jetty does not set the context class loader on adding the filters so
		// we do that instead
		try {
			ContextClassLoaderUtils.doWithClassLoader(context.getClassLoader(),
					new Callable<Void>() {

						public Void call() {
							servletHandler.addFilter(holder, mapping);
							return null;
						}

					});
		} catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			LOG.error("Ignored exception during filter registration", e);
		}
	}

	public void removeFilter(FilterModel model) {
		LOG.debug("Removing filter model [" + model + "]");
		final ServletContextHandler context = m_server.getContext(model.getContextModel()
				.getHttpContext());
		if (context == null)
			return; //Obviously no context available anymore the server is already down
		
		final ServletHandler servletHandler = context.getServletHandler();
		// first remove filter mappings for the removed filter
		final FilterMapping[] filterMappings = servletHandler
				.getFilterMappings();
		FilterMapping[] newFilterMappings = null;
		for (FilterMapping filterMapping : filterMappings) {
			if (filterMapping.getFilterName().equals(model.getName())) {
				if (newFilterMappings == null) {
					newFilterMappings = filterMappings;
				}
				newFilterMappings = (FilterMapping[]) LazyList.removeFromArray(
						newFilterMappings, filterMapping);
			}
		}
		servletHandler.setFilterMappings(newFilterMappings);
		// then remove the filter
		final FilterHolder filterHolder = servletHandler.getFilter(model
				.getName());
		final FilterHolder[] filterHolders = servletHandler.getFilters();
		final FilterHolder[] newFilterHolders = (FilterHolder[]) LazyList
				.removeFromArray(filterHolders, filterHolder);
		servletHandler.setFilters(newFilterHolders);
		// if filter is still started stop the filter (=filter.destroy()) as
		// Jetty will not do that
		if (filterHolder.isStarted()) {
			try {
				ContextClassLoaderUtils.doWithClassLoader(
						context.getClassLoader(), new Callable<Void>() {

							public Void call() throws Exception {
								filterHolder.stop();
								return null;
							}

						});
			} catch (Exception e) {
				if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				LOG.warn("Exception during unregistering of filter ["
						+ filterHolder.getFilter() + "]");
			}
		}
	}

	public void addErrorPage(final ErrorPageModel model) {
		final ServletContextHandler context = m_server.getOrCreateContext(model);
		final ErrorPageErrorHandler errorPageHandler = (ErrorPageErrorHandler) context
				.getErrorHandler();
		if (errorPageHandler == null) {
			throw new IllegalStateException(
					"Internal error: Cannot find the error handler. Please report.");
		}
		
		try {
			int code = Integer.parseInt(model.getError());
			errorPageHandler.addErrorPage(code, model.getLocation());
		} catch (NumberFormatException nfe) {
			//OK, not a number must be a class then
			errorPageHandler.addErrorPage(model.getError(), model.getLocation());
		}
		
	}

	public void removeErrorPage(final ErrorPageModel model) {
		final ServletContextHandler context = m_server.getContext(model.getContextModel().getHttpContext());
		if (context == null)
			return;//Obviously context is already removed
		final ErrorPageErrorHandler errorPageHandler = (ErrorPageErrorHandler) context
				.getErrorHandler();
		if (errorPageHandler == null) {
			throw new IllegalStateException(
					"Internal error: Cannot find the error handler. Please report.");
		}
		final Map<String, String> errorPages = errorPageHandler.getErrorPages();
		if (errorPages != null) {
			errorPages.remove(model.getError());
		}
	}

	//PAXWEB-210: create security constraints
	public void addSecurityConstraintMappings(final SecurityConstraintMappingModel model) {
		final ServletContextHandler context = m_server.getOrCreateContext(model);
		final SecurityHandler securityHandler = context.getSecurityHandler();
		if (securityHandler == null) {
			throw new IllegalStateException(
					"Internal error: Cannot find the security handler. Please report.");
		}
		String mappingMethod = model.getMapping();
		String constraintName = model.getConstraintName();
		String url = model.getUrl();
		String dataConstraint = model.getDataConstraint();
		List<String> roles = model.getRoles();
		boolean authentication = model.isAuthentication();

		ConstraintMapping newConstraintMapping = new ConstraintMapping();
		newConstraintMapping.setMethod(mappingMethod);
		newConstraintMapping.setPathSpec(url);
		Constraint constraint = new Constraint();
		constraint.setAuthenticate(authentication);
		constraint.setName(constraintName);
		constraint.setRoles(roles.toArray(new String[roles.size()]));

		if (dataConstraint == null || "NONE".equals(dataConstraint))
            constraint.setDataConstraint(Constraint.DC_NONE);
        else if ("INTEGRAL".equals(dataConstraint))
        	constraint.setDataConstraint(Constraint.DC_INTEGRAL);
        else if ("CONFIDENTIAL".equals(dataConstraint))
        	constraint.setDataConstraint(Constraint.DC_CONFIDENTIAL);
        else
        {
            LOG.warn("Unknown user-data-constraint:" + dataConstraint);
            constraint.setDataConstraint(Constraint.DC_CONFIDENTIAL);
        }

		newConstraintMapping.setConstraint(constraint);

		((ConstraintSecurityHandler)securityHandler).addConstraintMapping(newConstraintMapping);
	}

	public void addServletContainerInitializer(ContainerInitializerModel model) {
	}


	public void removeSecurityConstraintMappings(final SecurityConstraintMappingModel model) {
		final ServletContextHandler context = m_server.getContext(model.getContextModel().getHttpContext());
		if (context == null)
			return; //context already gone
		final SecurityHandler securityHandler = context.getSecurityHandler();
		if (securityHandler == null) {
			throw new IllegalStateException(
					"Internal error: Cannot find the security handler. Please report.");
		}

		List<ConstraintMapping> constraintMappings = ((ConstraintSecurityHandler)securityHandler).getConstraintMappings();
		for (ConstraintMapping constraintMapping : constraintMappings) {
			boolean urlMatch = constraintMapping.getPathSpec().equalsIgnoreCase(model.getUrl());
			boolean methodMatch = constraintMapping.getMethod().equalsIgnoreCase(model.getMapping());
			if (urlMatch && methodMatch)
				constraintMappings.remove(constraintMapping);
		}
	}


	public void configureRequestLog(String format, String retainDays,
			Boolean append, Boolean extend, Boolean dispatch,
			String TimeZone, String directory) {


          RequestLogHandler requestLogHandler = new RequestLogHandler();

          // TODO - Improve that to set the path of the LOG relative to $JETTY_HOME

          if (directory == null || directory.isEmpty())
        	  directory = "./logs/";
          File file = new File(directory);
          if (!file.exists()) {
        	  file.mkdirs();
        	  try {
				file.createNewFile();
				} catch (IOException e) {
					LOG.error("can't create NCSARequestLog", e);
				}
          }

          if (!directory.endsWith("/"))
        	  directory += "/";

          NCSARequestLog requestLog = new NCSARequestLog(directory + format);
          requestLog.setRetainDays(Integer.parseInt(retainDays));
          requestLog.setAppend(append);
          requestLog.setExtended(extend);
          requestLog.setLogDispatch(dispatch);
          requestLog.setLogTimeZone(TimeZone);
          requestLogHandler.setRequestLog(requestLog);

          ((HandlerCollection)m_server.getHandler()).addHandler(requestLogHandler);

    }



	@Override
	public String toString() {
		return new StringBuilder()
				.append(JettyServerImpl.class.getSimpleName()).append("{")
				.append("}").toString();
	}

	public void setServerConfigURL(URL serverConfigURL) {
		m_server.setServerConfigURL(serverConfigURL);
	}

	public URL getServerConfigURL() {
		return m_server.getServerConfigURL();
	}

}

