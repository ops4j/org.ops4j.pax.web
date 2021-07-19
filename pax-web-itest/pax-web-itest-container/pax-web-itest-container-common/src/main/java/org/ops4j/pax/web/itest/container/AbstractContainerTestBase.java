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
package org.ops4j.pax.web.itest.container;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.web.itest.AbstractControlledTestBase;
import org.ops4j.pax.web.itest.utils.WaitCondition;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.events.FilterEventData;
import org.ops4j.pax.web.service.spi.model.events.ServerEvent;
import org.ops4j.pax.web.service.spi.model.events.ServerListener;
import org.ops4j.pax.web.service.spi.model.events.ServletEventData;
import org.ops4j.pax.web.service.spi.model.events.WebApplicationEvent;
import org.ops4j.pax.web.service.spi.model.events.WebApplicationEventListener;
import org.ops4j.pax.web.service.spi.model.events.WebElementEvent;
import org.ops4j.pax.web.service.spi.model.events.WebElementEventData;
import org.ops4j.pax.web.service.spi.model.events.WebElementEventListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * <p>Single base class for all container-related tests.</p>
 */
public abstract class AbstractContainerTestBase extends AbstractControlledTestBase {

	protected WebElementEventListener webElementEventListener;

	protected Option[] baseConfigure() {
		Option[] options = super.baseConfigure();
		Option[] containerOptions = new Option[] {
				frameworkProperty("org.osgi.service.http.port").value("8181")
		};
		options = combine(options, containerOptions);
		options = combine(options, defaultLoggingConfig());
		// with PAXLOGGING-308 we can simply point to _native_ configuration file understood directly
		// by selected pax-logging backend
		options = combine(options, systemProperty("org.ops4j.pax.logging.property.file")
				.value("../../etc/log4j2-osgi.properties"));

		options = combine(options, paxWebCore());
		options = combine(options, paxWebRuntime());
		options = combine(options, paxWebTestSupport());

		return options;
	}

	// --- helper methods to be used in all the tests

	/**
	 * Creates a listener for generic {@link WebElementEvent} events with
	 * associated {@link org.ops4j.pax.web.itest.utils.WaitCondition} fulfilled after satisfying passed
	 * {@link java.util.function.BiPredicate} operating on single {@link WebElementEvent}. This method sets up
	 * the listener, calls the passed {@code action} and waits for the condition that's satisfied according
	 * to passed {@code expectation}.
	 */
	protected void configureAndWait(Runnable action, final BiPredicate<WebElementEvent.State, WebElementEventData> expectation) {
		final List<WebElementEvent> events = new CopyOnWriteArrayList<>();
		webElementEventListener = events::add;
		ServiceRegistration<WebElementEventListener> reg
				= context.registerService(WebElementEventListener.class, webElementEventListener, null);

		action.run();

		try {
			new WaitCondition("Waiting for " + expectation) {
				@Override
				protected boolean isFulfilled() {
					return events.stream().anyMatch(e -> expectation.test(e.getType(), e.getData()));
				}
			}.waitForCondition();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			if (reg != null) {
				reg.unregister();
			}
		}
	}

	/**
	 * "configure and wait" method that allows to check entire stream of {@link WebElementEvent}s.
	 * @param action
	 * @param expectation
	 */
	protected void configureAndWait(Runnable action, final Predicate<List<WebElementEvent>> expectation) {
		final List<WebElementEvent> events = new CopyOnWriteArrayList<>();
		webElementEventListener = events::add;
		ServiceRegistration<WebElementEventListener> reg
				= context.registerService(WebElementEventListener.class, webElementEventListener, null);

		action.run();

		try {
			new WaitCondition("Waiting for " + expectation) {
				@Override
				protected boolean isFulfilled() {
					return expectation.test(events);
				}
			}.waitForCondition();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			if (reg != null) {
				reg.unregister();
			}
		}
	}

	/**
	 * Creates a listener for deployment of named {@link javax.servlet.Servlet}.
	 * @param servletName
	 * @param action
	 */
	protected void configureAndWaitForNamedServlet(final String servletName, Action action) throws Exception {
		final List<WebElementEvent> events = new CopyOnWriteArrayList<>();
		webElementEventListener = events::add;
		ServiceRegistration<WebElementEventListener> reg
				= context.registerService(WebElementEventListener.class, webElementEventListener, null);

		action.run();

		try {
			new WaitCondition("Waiting for " + servletName + " servlet") {
				@Override
				protected boolean isFulfilled() {
					return events.stream().anyMatch(e ->
							e.getType() == WebElementEvent.State.DEPLOYED
									&& e.getData() instanceof ServletEventData
									&& ((ServletEventData) e.getData()).getServletName().equals(servletName));
				}
			}.waitForCondition();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			if (reg != null) {
				reg.unregister();
			}
		}
	}

	/**
	 * Creates a listener for deployment of a {@link javax.servlet.Servlet} mapped to some URL.
	 * @param mapping
	 * @param action
	 */
	protected void configureAndWaitForServletWithMapping(final String mapping, Action action) throws Exception {
		final List<WebElementEvent> events = new CopyOnWriteArrayList<>();
		webElementEventListener = events::add;
		ServiceRegistration<WebElementEventListener> reg
				= context.registerService(WebElementEventListener.class, webElementEventListener, null);

		action.run();

		try {
			new WaitCondition("Waiting for servlet mapped to " + mapping) {
				@Override
				protected boolean isFulfilled() {
					return events.stream().anyMatch(e ->
							e.getType() == WebElementEvent.State.DEPLOYED
									&& e.getData() instanceof ServletEventData
									&& Arrays.asList(((ServletEventData) e.getData()).getUrlPatterns()).contains(mapping));
				}
			}.waitForCondition();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			if (reg != null) {
				reg.unregister();
			}
		}
	}

	/**
	 * Creates a listener for deployment of a {@link javax.servlet.Filter} mapped to some URL.
	 * @param mapping
	 * @param action
	 */
	protected void configureAndWaitForFilterWithMapping(final String mapping, Action action) throws Exception {
		final List<WebElementEvent> events = new CopyOnWriteArrayList<>();
		webElementEventListener = events::add;
		ServiceRegistration<WebElementEventListener> reg
				= context.registerService(WebElementEventListener.class, webElementEventListener, null);

		action.run();

		try {
			new WaitCondition("Waiting for filter mapped to " + mapping) {
				@Override
				protected boolean isFulfilled() {
					return events.stream().anyMatch(e ->
							e.getType() == WebElementEvent.State.DEPLOYED
									&& e.getData() instanceof FilterEventData
									&& Arrays.asList(((FilterEventData) e.getData()).getUrlPatterns()).contains(mapping));
				}
			}.waitForCondition();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			if (reg != null) {
				reg.unregister();
			}
		}
	}

	/**
	 * Performs an action and waits for {@link org.ops4j.pax.web.service.spi.model.events.WebApplicationEvent} related
	 * to started WAB
	 * @param action
	 */
	protected void configureAndWaitForDeployment(Action action) throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		WebApplicationEventListener listener = event -> {
			if (event.getType() == WebApplicationEvent.State.DEPLOYED) {
				latch.countDown();
			}
		};
		ServiceRegistration<WebApplicationEventListener> reg
				= context.registerService(WebApplicationEventListener.class, listener, null);

		action.run();

		try {
			new WaitCondition("Waiting for deployment") {
				@Override
				protected boolean isFulfilled() {
					return latch.getCount() == 0L;
				}
			}.waitForCondition();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			if (reg != null) {
				reg.unregister();
			}
		}
	}

	/**
	 * Performs an action and waits for {@link org.ops4j.pax.web.service.spi.model.events.WebApplicationEvent} related
	 * to started WAB, but only if the provided sample bundle is not ACTIVE.
	 * @param sample
	 * @param action
	 */
	protected Bundle configureAndWaitForDeploymentUnlessInstalled(String sample, Action action) throws Exception {
		Bundle b = sampleBundle(sample);
		if (b != null && b.getState() == Bundle.ACTIVE) {
			return b;
		}

		configureAndWaitForDeployment(action);

		return sampleBundle(sample);
	}

	/**
	 * Performs an action and waits for {@link org.ops4j.pax.web.service.spi.model.events.ServerEvent} related
	 * to started container at given port
	 * @param port
	 * @param actions
	 */
	protected void configureAndWaitForListener(int port, Action ... actions) throws Exception {
		final List<ServerEvent> events = new CopyOnWriteArrayList<>();
		ServerListener listener = events::add;
		ServiceRegistration<ServerListener> reg = context.registerService(ServerListener.class, listener, null);

		if (actions != null) {
			for (Action a : actions) {
				a.run();
			}
		}

		try {
			new WaitCondition("Waiting for server listening at " + port) {
				@Override
				protected boolean isFulfilled() {
					return events.stream().anyMatch(e ->
							e.getState() == ServerEvent.State.STARTED
									&& Arrays.stream(e.getAddresses()).map(InetSocketAddress::getPort)
									.anyMatch(p -> p == port));
				}
			}.waitForCondition();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			if (reg != null) {
				reg.unregister();
			}
		}
	}

	/**
	 * Checks whether an event is for {@link org.ops4j.pax.web.service.spi.model.elements.ElementModel} registration
	 * to all the passed context names.
	 * @param data
	 * @param names
	 * @return
	 */
	public static boolean usesContexts(WebElementEventData data, String ... names) {
		Set<String> used = new HashSet<>(data.getContextNames());
		return used.containsAll(Arrays.asList(names));
	}

	public static HttpService getHttpService(final BundleContext bundleContext) {
		ServiceTracker<HttpService, HttpService> tracker = new ServiceTracker<>(bundleContext, HttpService.class, null);
		tracker.open();
		try {
			return tracker.waitForService(5000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	public static WebContainer getWebContainer(final BundleContext bundleContext) {
		ServiceTracker<WebContainer, WebContainer> tracker = new ServiceTracker<>(bundleContext, WebContainer.class, null);
		tracker.open();
		try {
			return tracker.waitForService(5000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	/**
	 * Creates {@link javax.servlet.Servlet} init parameters with legacy name that can be (which is deprecated, but
	 * the only way with pure {@link HttpService} to specify servlet name) used to configure {@link javax.servlet.Servlet}
	 * name.
	 * @param servletName
	 * @return
	 */
	@SuppressWarnings("deprecation")
	protected Dictionary<?,?> legacyName(String servletName) {
		Dictionary<String, Object> initParams = new Hashtable<>();
		initParams.put(PaxWebConstants.INIT_PARAM_SERVLET_NAME, servletName);
		return initParams;
	}

	@FunctionalInterface
	public interface Action {
		void run() throws Exception;
	}

}
