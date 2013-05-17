package org.ops4j.pax.web.service.tomcat.internal;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.ops4j.pax.web.service.spi.ServletContextManager.ServletContextWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TomcatServletContextWrapper implements ServletContextWrapper {

	private static final Logger LOG = LoggerFactory
			.getLogger(TomcatServletContextWrapper.class);

	private Context context;

	private Host host;

	public TomcatServletContextWrapper(Context context, Host host) {
		this.context = context;
		// this.host = host;
	}

	@Override
	public void start() {
		try {
			LifecycleState state = context.getState();
			if (LifecycleState.STARTING_PREP.equals(state) ||
	                LifecycleState.STARTING.equals(state) ||
	                LifecycleState.STARTED.equals(state)) {
	            
	            if (LOG.isDebugEnabled()) {
	                Exception e = new LifecycleException();
	                LOG.debug("Lifecylce already started this call will be ignored: ", e);
	            } else if (LOG.isInfoEnabled()) {
	            	LOG.info("Lifecylce already started this call will be ignored");
	            }
	            
	            return;
	        }
			context.start();
		} catch (LifecycleException e) {
			LOG.info("LifecycleException, context already started", e);
		}
	}

	@Override
	public void stop() {
		try {
			context.stop();
		} catch (Exception exc) {
			LOG.error("Could not stop the servlet context for context path ["
					+ context.getPath() + "]", exc);
		}
	}

}
