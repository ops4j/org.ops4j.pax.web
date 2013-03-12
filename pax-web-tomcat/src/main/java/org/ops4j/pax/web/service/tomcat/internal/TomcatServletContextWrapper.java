package org.ops4j.pax.web.service.tomcat.internal;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
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
			context.start();
		} catch (LifecycleException e) {
			LOG.info("LifecycleException, context already started", e);
		}
		// }
		// catch (Exception exc) {
		// LOG.error(
		// "Could not start the servlet context for context path [" +
		// context.getPath()
		// + "]", exc);
		// }
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
