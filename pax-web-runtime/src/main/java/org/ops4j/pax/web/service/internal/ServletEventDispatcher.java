/* Copyright 2011 Achim Nierbeck.
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

package org.ops4j.pax.web.service.internal;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.spi.ServletEvent;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.ops4j.pax.web.service.spi.WebEvent.WebTopic;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Achim Nierbeck
 * 
 */
public class ServletEventDispatcher implements ServletListener {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(ServletEventDispatcher.class);

	private final ScheduledExecutorService executors;
	private EventAdmin eventAdminService;
	private LogService logService;
	private final ServiceTracker servletListenerTracker;
	private final Set<ServletListener> listeners = new CopyOnWriteArraySet<ServletListener>();
	private final Map<Bundle, ServletEvent> states = new ConcurrentHashMap<Bundle, ServletEvent>();

	public ServletEventDispatcher(final BundleContext bundleContext,
			ScheduledExecutorService executors) {
		NullArgumentException.validateNotNull(bundleContext, "Bundle Context");
		NullArgumentException.validateNotNull(executors, "Thread executors");

		this.executors = executors;

		this.servletListenerTracker = new ServiceTracker(bundleContext,
				ServletListener.class.getName(),
				new ServiceTrackerCustomizer() {
					public Object addingService(ServiceReference reference) {
						ServletListener listener = (ServletListener) bundleContext
								.getService(reference);

						synchronized (listeners) {
							sendInitialEvents(listener);
							listeners.add(listener);
						}

						return listener;
					}

					public void modifiedService(ServiceReference reference,
							Object service) {
					}

					public void removedService(ServiceReference reference,
							Object service) {
						listeners.remove(service);
						bundleContext.ungetService(reference);
					}
				});
		this.servletListenerTracker.open();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ops4j.pax.web.service.spi.ServletListener#servletEvent(org.ops4j.
	 * pax.web.service.spi.ServletEvent)
	 */
	public void servletEvent(final ServletEvent event) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Sending web event " + event + " for bundle "
					+ event.getBundle().getSymbolicName());
		}

		synchronized (listeners) {
			callListeners(event);
			states.put(event.getBundle(), event);
		}
		
		final String topic;
    	switch(event.getType()) {
    		case WebEvent.DEPLOYING:
    			topic = WebTopic.DEPLOYING.toString();
    			break;
    		case WebEvent.DEPLOYED:
    			topic = WebTopic.DEPLOYED.toString();
    			break;
    		case WebEvent.UNDEPLOYING:
    			topic = WebTopic.UNDEPLOYING.toString();
    			break;
    		case WebEvent.UNDEPLOYED:
    			topic = WebTopic.UNDEPLOYED.toString();
    			break;
    		case WebEvent.FAILED:
    			topic = WebTopic.FAILED.toString();
    			break;
            case WebEvent.WAITING:
                //topic = WebTopic.WAITING.toString(); 
            	// A Waiting Event is not supported by the specification 
            	// therefore it is mapped to FAILED, because of collision. 
            	topic = WebTopic.FAILED.toString();
            	break;
    		default:
    			topic = WebTopic.FAILED.toString();
    	}

		if (eventAdminService != null) {
			try {
                executors.submit(new Runnable() {
                    public void run() {
                        Dictionary<String, Object> properties = new Hashtable<String, Object>();
                        properties.put("servlet.alias", event.getAlias()==null ? "" : event.getAlias());
                        properties.put("servlet.name", event.getServletName() == null ? "" : event.getServletName());
                        properties.put("servlet.urlparameter", event.getUrlParameter() == null ? "" : event.getUrlParameter());
                        properties.put("servlet.servlet", event.getServlet());
                        properties.put("timestamp", event.getTimestamp());
                        if(event.getHttpContext() != null ) properties.put("servlet.httpcontext", event.getHttpContext());

                        Event event = new Event(topic, properties);
                        EventAdmin adminService = getEventAdminService();
                        if (adminService != null)
                            adminService.postEvent(event);

                    }
                });
			} catch (RejectedExecutionException ree) {
				LOG.debug("Executor shut down", ree);
			}

		}

		if (logService != null) {
			try {
				executors.submit(new Runnable() {
					public void run() {
						getLogService().log(LogService.LOG_DEBUG, topic);
					}
				});
			} catch (RejectedExecutionException ree) {
				LOG.debug("Executor shut down", ree);
			}

		} else {
			LOG.debug(topic);
		}
	}
	
	public void servletEvent(int type, Bundle bundle, ServletModel model) {
	    servletEvent(new ServletEvent(type, bundle, model.getAlias(),
	            model.getName(), model.getUrlPatterns(), model.getServlet(), 
	            model.getServletClass(), model.getContextModel().getHttpContext()));
	}

	void destroy() {
		executors.shutdown();
		// wait for the queued tasks to execute
		try {
			executors.awaitTermination(60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// ignore
		}
		servletListenerTracker.close();
		// clean up the EventAdmin tracker if we're using that
		eventAdminService = null;
	}

	private void sendInitialEvents(ServletListener listener) {
		for (Map.Entry<Bundle, ServletEvent> entry : states.entrySet()) {
			try {
				callListener(listener, new ServletEvent(entry.getValue(), true));
			} catch (RejectedExecutionException ree) {
				LOG.warn("Executor shut down", ree);
				break;
			}
		}
	}

	private void callListeners(ServletEvent servletEvent) {
		for (ServletListener listener : listeners) {
			try {
				callListener(listener, servletEvent);
			} catch (RejectedExecutionException ree) {
				LOG.warn("Executor shut down", ree);
				break;
			}
		}
	}

	private void callListener(final ServletListener listener,
			final ServletEvent event) throws RejectedExecutionException {
		try {
			executors.invokeAny(Collections
					.<Callable<Void>> singleton(new Callable<Void>() {
						public Void call() throws Exception {
							listener.servletEvent(event);
							return null;
						}
					}), 60L, TimeUnit.SECONDS);
		} catch (InterruptedException ie) {
			LOG.warn("Thread interrupted", ie);
			Thread.currentThread().interrupt();
		} catch (TimeoutException te) {
			LOG.warn("Listener timed out, will be ignored", te);
			listeners.remove(listener);
		} catch (ExecutionException ee) {
			LOG.warn("Listener caused an exception, will be ignored", ee);
			listeners.remove(listener);
		}
	}

	public void setLogService(Object logService) {
		if (logService instanceof LogService)
			this.logService = (LogService) logService;
	}

	public void setEventAdminService(Object eventService) {
		if (eventService instanceof EventAdmin)
			this.eventAdminService = (EventAdmin) eventService;
	}

	private EventAdmin getEventAdminService() {
		return eventAdminService;
	}
	
	private LogService getLogService() {
		return logService;
	}

}
