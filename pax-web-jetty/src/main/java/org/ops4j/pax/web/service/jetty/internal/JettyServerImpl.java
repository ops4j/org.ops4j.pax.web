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
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
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
import org.ops4j.pax.swissbox.core.BundleClassLoader;
import org.ops4j.pax.swissbox.core.ContextClassLoaderUtils;
import org.ops4j.pax.web.service.spi.LifeCycle;
import org.ops4j.pax.web.service.spi.model.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.FilterModel;
import org.ops4j.pax.web.service.spi.model.SecurityConstraintMappingModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.ops4j.pax.web.service.spi.model.WelcomeFileModel;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JettyServerImpl implements JettyServer {

	private static final Logger LOG = LoggerFactory
			.getLogger(JettyServerImpl.class);

	private final JettyServerWrapper server;

	private Bundle bundle;


	JettyServerImpl(final ServerModel serverModel, Bundle bundle) {
		server = new JettyServerWrapper(serverModel, new QueuedThreadPool());
		this.bundle = bundle;
	}

	@Override
	public void start() {
		LOG.debug("Starting " + this);
		try {
			// PAXWEB-193 suggested we should open this up for external
			// configuration
			URL jettyResource = getServerConfigURL();
			if (jettyResource == null) {
				jettyResource = getClass().getResource("/jetty.xml");
			}
			File serverConfigurationFile = getServerConfigDir();
			if (serverConfigurationFile != null) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("server configuration file location: "
							+ serverConfigurationFile);
				}
				if (!serverConfigurationFile.isDirectory()
						&& serverConfigurationFile.canRead()) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("server configuration directory exists and is readable");
					}
					String fileName = serverConfigurationFile.getName();
					if (fileName.equalsIgnoreCase("jetty.xml")) {
						jettyResource = serverConfigurationFile.toURI().toURL();
					} else {
						LOG.warn("No configuration file found with name jetty.xml. Filename given: {}, will read this one instead. ", fileName);
						jettyResource = serverConfigurationFile.toURI().toURL();
					}
				} else {
					LOG.warn("server configuration file location is invalid");
				}
			}
			if (jettyResource != null) {
				ClassLoader loader = Thread.currentThread()
						.getContextClassLoader();
				try {
					Thread.currentThread().setContextClassLoader(
							getClass().getClassLoader());
					LOG.debug("Configure using resource " + jettyResource);
					XmlConfiguration configuration = new XmlConfiguration(
							jettyResource);
					// configuration.configure(m_server);
					Method method = XmlConfiguration.class.getMethod(
							"configure", Object.class);
					method.invoke(configuration, server);
				} finally {
					Thread.currentThread().setContextClassLoader(loader);
				}
			}

			// PAXWEB-568
			//CHECKSTYLE:OFF
			// Setup JMX
			try {
				Class.forName("javax.management.JMX");
				MBeanContainer mbContainer = new MBeanContainer(
						ManagementFactory.getPlatformMBeanServer());
				server.addBean(mbContainer);
			} catch (Throwable t) { 
				// no jmx available just ignore it!
				LOG.debug("No JMX available will keep going");
			}
			server.start();
			//CHECKSTYLE:ON

			Connector[] connectors = server.getConnectors();
			if (connectors != null) {
				for (Connector connector : connectors) {
					LOG.info(
							"Pax Web available at [{}]:[{}]",
							((ServerConnector) connector).getHost() == null ? "0.0.0.0"
									: ((ServerConnector) connector).getHost(),
							((ServerConnector) connector).getPort());
				}
			} else {
				LOG.info("Pax Web is started with it's default configuration most likely it's listening on port 8181");
			}

			//CHECKSTYLE:OFF
		} catch (Exception e) { 
			throw new RuntimeException("Exception while starting Jetty", e);
		}
		//CHECKSTYLE:ON
	}

	@Override
	public void stop() {
		LOG.debug("Stopping " + this);
		try {
			server.stop();
			server.destroy();
			//CHECKSTYLE:OFF
		} catch (Exception e) { 
			LOG.error("Exception while stopping Jetty:", e);
		}
		//CHECKSTYLE:ON
	}

	@Override
	public void addConnector(final Connector connector) {
		LOG.info("Pax Web available at [{}]:[{}]",
				((ServerConnector) connector).getHost() == null ? "0.0.0.0"
						: ((ServerConnector) connector).getHost(),
				((ServerConnector) connector).getPort());
		server.addConnector(connector);
	}

	@Override
	public Connector[] getConnectors() {
		return server.getConnectors();
	}

	@Override
	public void removeConnector(final Connector connector) {
		LOG.info("Removing connection for [{}]:[{}]",
				((ServerConnector) connector).getHost() == null ? "0.0.0.0"
						: ((ServerConnector) connector).getHost(),
				((ServerConnector) connector).getPort());
		server.removeConnector(connector);
	}

	@Override
	public void configureContext(final Map<String, Object> attributes,
			final Integer sessionTimeout, final String sessionCookie, 
			final String sessionDomain, final String sessionPath,
			final String sessionUrl, final Boolean sessionCookieHttpOnly, 
			final Boolean sessionCookieSecure, final String workerName, 
			final Boolean lazyLoad, final String storeDirectory) {
		server.configureContext(attributes, sessionTimeout, sessionCookie, 
				sessionDomain, sessionPath, sessionUrl, sessionCookieHttpOnly, 
				sessionCookieSecure, workerName, lazyLoad, storeDirectory);
	}

	@Override
	public LifeCycle getContext(final ContextModel model) {
		final ServletContextHandler context = server.getOrCreateContext(model);
		return new LifeCycle() {
			@Override
			public void start() throws Exception {
				ClassLoader classLoader = context.getClassLoader();
				BundleClassLoader containerSpecificClassLoader 
					= new BundleClassLoader(bundle , classLoader);
				context.setClassLoader(containerSpecificClassLoader);
				context.start();
			}

			@Override
			public void stop() throws Exception {
				context.stop();
			}
		};
	}

	@Override
	public void addServlet(final ServletModel model) {
		LOG.debug("Adding servlet [" + model + "]");
		final ServletMapping mapping = new ServletMapping();
		mapping.setServletName(model.getName());
		mapping.setPathSpecs(model.getUrlPatterns());
		final ServletContextHandler context = server.getOrCreateContext(model);
		final ServletHandler servletHandler = context.getServletHandler();
		if (servletHandler == null) {
			throw new IllegalStateException(
					"Internal error: Cannot find the servlet holder");
		}

		final ServletHolder holder;
		if (model.getServlet() == null) {
			holder = new ServletHolder(model.getServletClass());
		} else {
			holder = new ServletHolder(model.getServlet());
		}
		holder.setName(model.getName());
		if (model.getInitParams() != null) {
			holder.setInitParameters(model.getInitParams());
		}
		if (model.getAsyncSupported() != null) {
			holder.setAsyncSupported(model.getAsyncSupported());
		}
		if (model.getLoadOnStartup() != null) {
			holder.setInitOrder(model.getLoadOnStartup());
		}

		// Jetty does not set the context class loader on adding the filters so
		// we do that instead
		try {
			ContextClassLoaderUtils.doWithClassLoader(context.getClassLoader(),
					new Callable<Void>() {

						@Override
						public Void call() {
							servletHandler.addServlet(holder);
							servletHandler.addServletMapping(mapping);
							return null;
						}

					});
			if (holder.isStarted()) {
				// initialize servlet
				holder.getServlet();
			}
			//CHECKSTYLE:OFF
		} catch (Exception e) { 
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			LOG.error("Ignored exception during servlet registration", e);
		}
		//CHECKSTYLE:ON
	}

	@Override
	public void removeServlet(final ServletModel model) {
		LOG.debug("Removing servlet [" + model + "]");
		// jetty does not provide a method for removing a servlet so we have to
		// do it by our own
		// the facts below are found by analyzing ServletHolder implementation
		boolean removed = false;
		final ServletContextHandler context = server.getContext(model
				.getContextModel().getHttpContext());
		if (context == null) {
			return; // context is already removed so no need for deregistration
		}

		final ServletHandler servletHandler = context.getServletHandler();
		final ServletHolder[] holders = servletHandler.getServlets();
		if (holders != null) {
			final ServletHolder holder = servletHandler.getServlet(model
					.getName());
			if (holder != null) {
				servletHandler.setServlets((ServletHolder[]) LazyList.remove(
						holders, holder));
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
										.remove(mappings, mapping));
						removed = true;
					}
				}
				// if servlet is still started stop the servlet holder
				// (=servlet.destroy()) as Jetty will not do that
				if (holder.isStarted()) {
					try {
						ContextClassLoaderUtils.doWithClassLoader(
								context.getClassLoader(), new Callable<Void>() {

									@Override
									public Void call() throws Exception {
										holder.stop();
										return null;
									}

								});
						//CHECKSTYLE:OFF
					} catch (Exception e) { 
						if (e instanceof RuntimeException) {
							throw (RuntimeException) e;
						}
						LOG.warn("Exception during unregistering of servlet ["
								+ model + "]");
					}
					//CHECKSTYLE:ON
				}
			}
		}
		if (servletHandler.getServlets() == null
				|| servletHandler.getServlets().length == 0) {
			removeContext(model.getContextModel().getHttpContext());
		}
		if (!removed) {
			throw new IllegalStateException(model + " was not found");
		}
	}

	@Override
	public void addEventListener(final EventListenerModel model) {
		server.getOrCreateContext(model).addEventListener(
				model.getEventListener());
	}

	@Override
	public void removeEventListener(final EventListenerModel model) {
		final ServletContextHandler context = server.getContext(model
				.getContextModel().getHttpContext());

		if (context == null) {
			return; // Obviously context is already destroyed
		}

		final List<EventListener> listeners = new ArrayList<EventListener>(
				Arrays.asList(context.getEventListeners()));
		listeners.remove(model.getEventListener());
		context.setEventListeners(listeners.toArray(new EventListener[listeners
				.size()]));
	}

	@Override
	public void removeContext(final HttpContext httpContext) {
		server.removeContext(httpContext);
	}

	@Override
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
		for (String d : model.getDispatcher()) {
			//dispatcher = FilterMapping.dispatch(baseRequest.getDispatcherType());
			/*
			DispatcherType type = DispatcherType.valueOf(d);
			dispatcher |= FilterMapping.dispatch(type);
			*/
			if ("ALL".equalsIgnoreCase(d)) {
				dispatcher |= FilterMapping.ALL;
			} else if ("ASYNC".equalsIgnoreCase(d)) {
				dispatcher |= FilterMapping.ASYNC;
			} else if ("DEFAULT".equalsIgnoreCase(d)) {
				dispatcher |= FilterMapping.DEFAULT;
			} else if ("ERROR".equalsIgnoreCase(d)) {
				dispatcher |= FilterMapping.ERROR;
			} else if ("FORWARD".equalsIgnoreCase(d)) {
				dispatcher |= FilterMapping.FORWARD;
			} else if ("INCLUDE".equalsIgnoreCase(d)) {
				dispatcher |= FilterMapping.INCLUDE;
			} else if ("REQUEST".equalsIgnoreCase(d)) {
				dispatcher |= FilterMapping.REQUEST;
			}
		}
		mapping.setDispatches(dispatcher);

		final ServletContextHandler context = server.getOrCreateContext(model);
		final ServletHandler servletHandler = context.getServletHandler();
		if (servletHandler == null) {
			throw new IllegalStateException(
					"Internal error: Cannot find the servlet holder");
		}
		
		final FilterHolder holder;
		if (model.getFilter() == null) {
			holder = new FilterHolder(model.getFilterClass());
		} else {
			holder = new FilterHolder(model.getFilter());
		}
		holder.setName(model.getName());
		if (model.getInitParams() != null) {
			holder.setInitParameters(model.getInitParams());
		}

		// Jetty does not set the context class loader on adding the filters so
		// we do that instead
		try {
			ContextClassLoaderUtils.doWithClassLoader(context.getClassLoader(),
					new Callable<Void>() {

						@Override
						public Void call() {
							servletHandler.addFilter(holder, mapping);
							return null;
						}

					});
			//CHECKSTYLE:OFF
		} catch (Exception e) { 
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			LOG.error("Ignored exception during filter registration", e);
		}
		//CHECKSTYLE:OFF
	}

	@Override
	public void removeFilter(FilterModel model) {
		LOG.debug("Removing filter model [" + model + "]");
		final ServletContextHandler context = server.getContext(model
				.getContextModel().getHttpContext());
		if (context == null) {
			return; // Obviously no context available anymore the server is
					// already down
		}

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
				newFilterMappings = (FilterMapping[]) LazyList.remove(
						newFilterMappings, filterMapping);
			}
		}
		servletHandler.setFilterMappings(newFilterMappings);
		// then remove the filter
		final FilterHolder filterHolder = servletHandler.getFilter(model
				.getName());
		final FilterHolder[] filterHolders = servletHandler.getFilters();
		final FilterHolder[] newFilterHolders = (FilterHolder[]) LazyList
				.remove(filterHolders, filterHolder);
		servletHandler.setFilters(newFilterHolders);
		// if filter is still started stop the filter (=filter.destroy()) as
		// Jetty will not do that
		if (filterHolder.isStarted()) {
			try {
				ContextClassLoaderUtils.doWithClassLoader(
						context.getClassLoader(), new Callable<Void>() {

							@Override
							public Void call() throws Exception {
								filterHolder.stop();
								return null;
							}

						});
				//CHECKSTYLE:OFF
			} catch (Exception e) {
				if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				LOG.warn("Exception during unregistering of filter ["
						+ filterHolder.getFilter() + "]");
			}
			//CHECKSTYLE:ON
		}
	}

	@Override
	public void addErrorPage(final ErrorPageModel model) {
		final ServletContextHandler context = server.getOrCreateContext(model);
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
			// OK, not a number must be a class then
			errorPageHandler
					.addErrorPage(model.getError(), model.getLocation());
		}

	}

	@Override
	public void removeErrorPage(final ErrorPageModel model) {
		final ServletContextHandler context = server.getContext(model
				.getContextModel().getHttpContext());
		if (context == null) {
			return;// Obviously context is already removed
		}
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
	
	// PAXWEB-123: try to register WelcomeFiles differently
	@Override
	public void addWelcomeFiles(final WelcomeFileModel model) {
		final ServletContextHandler context = server
				.getOrCreateContext(model);
		
		context.setWelcomeFiles(model.getWelcomeFiles());

	}
	
	@Override
	public void removeWelcomeFiles(final WelcomeFileModel model) {
		final ServletContextHandler context = server.getContext(model
				.getContextModel().getHttpContext());
		if (context == null) {
			return;// Obviously context is already removed
		}
		String[] welcomeFiles = context.getWelcomeFiles();
		List<String> welcomeFileList = new ArrayList<String>(Arrays.asList(welcomeFiles));
		welcomeFileList.removeAll(Arrays.asList(model.getWelcomeFiles()));
	}
	// PAXWEB-123: done

	// PAXWEB-210: create security constraints
	@Override
	public void addSecurityConstraintMappings(
			final SecurityConstraintMappingModel model) {
		final ServletContextHandler context = server.getOrCreateContext(model);
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

		if (dataConstraint == null || "NONE".equals(dataConstraint)) {
			constraint.setDataConstraint(Constraint.DC_NONE);
		} else if ("INTEGRAL".equals(dataConstraint)) {
			constraint.setDataConstraint(Constraint.DC_INTEGRAL);
		} else if ("CONFIDENTIAL".equals(dataConstraint)) {
			constraint.setDataConstraint(Constraint.DC_CONFIDENTIAL);
		} else {
			LOG.warn("Unknown user-data-constraint:" + dataConstraint);
			constraint.setDataConstraint(Constraint.DC_CONFIDENTIAL);
		}

		newConstraintMapping.setConstraint(constraint);

		((ConstraintSecurityHandler) securityHandler)
				.addConstraintMapping(newConstraintMapping);
	}

	@Override
	public void addServletContainerInitializer(ContainerInitializerModel model) {
	}

	@Override
	public void removeSecurityConstraintMappings(
			final SecurityConstraintMappingModel model) {
		final ServletContextHandler context = server.getContext(model
				.getContextModel().getHttpContext());
		if (context == null) {
			return; // context already gone
		}
		final SecurityHandler securityHandler = context.getSecurityHandler();
		if (securityHandler == null) {
			throw new IllegalStateException(
					"Internal error: Cannot find the security handler. Please report.");
		}

		List<ConstraintMapping> constraintMappings = ((ConstraintSecurityHandler) securityHandler)
				.getConstraintMappings();
		for (ConstraintMapping constraintMapping : constraintMappings) {
			boolean urlMatch = constraintMapping.getPathSpec()
					.equalsIgnoreCase(model.getUrl());
			boolean methodMatch = constraintMapping.getMethod()
					.equalsIgnoreCase(model.getMapping());
			if (urlMatch && methodMatch) {
				constraintMappings.remove(constraintMapping);
			}
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void configureRequestLog(ConfigureRequestLogParameter configureRequestParameters) {

		String directory = configureRequestParameters.dir;
		RequestLogHandler requestLogHandler = new RequestLogHandler();

		// TODO: Improve that to set the path of the LOG relative to
		// $JETTY_HOME

		if (directory == null || directory.isEmpty()) {
			directory = "./logs/"; 
		}
		File file = new File(directory);
		if (!file.exists()) {
			file.mkdirs();
			try {
				file.createNewFile();
			} catch (IOException e) {
				LOG.error("can't create NCSARequestLog", e);
			}
		}
		LOG.info("NCSARequestlogging is using the following directory: {}",
				file.getAbsolutePath());

		if (!directory.endsWith("/")) {
			directory += "/";
		}

		NCSARequestLog requestLog = new NCSARequestLog(directory + configureRequestParameters.format);
		requestLog.setRetainDays(Integer.parseInt(configureRequestParameters.retainDays));
		requestLog.setAppend(configureRequestParameters.append);
		requestLog.setExtended(configureRequestParameters.extend);
		requestLog.setLogDispatch(configureRequestParameters.dispatch);
		requestLog.setLogTimeZone(configureRequestParameters.timeZone);
		requestLog.setLogLatency(configureRequestParameters.logLatency);
		requestLog.setLogCookies(configureRequestParameters.logCookies);
		requestLog.setLogServer(configureRequestParameters.logServer);
		requestLogHandler.setRequestLog(requestLog);

		((HandlerCollection) server.getHandler()).addHandler(requestLogHandler);
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append(JettyServerImpl.class.getSimpleName()).append("{")
				.append("}").toString();
	}

	@Override
	public void setServerConfigDir(File serverConfigDir) {
		server.setServerConfigDir(serverConfigDir);
	}

	@Override
	public File getServerConfigDir() {
		return server.getServerConfigDir();
	}

	@Override
	public void setServerConfigURL(URL serverConfigURL) {
		server.setServerConfigURL(serverConfigURL);
	}

	@Override
	public URL getServerConfigURL() {
		return server.getServerConfigURL();
	}

	@Override
	public JettyServerWrapper getServer() {
		return server;
	}
}
