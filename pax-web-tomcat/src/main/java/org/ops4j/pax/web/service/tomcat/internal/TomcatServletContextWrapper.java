package org.ops4j.pax.web.service.tomcat.internal;

import org.apache.catalina.Context;
import org.ops4j.pax.web.service.spi.ServletContextManager.ServletContextWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TomcatServletContextWrapper implements ServletContextWrapper {

	private static final Logger LOG = LoggerFactory.getLogger(TomcatServletContextWrapper.class);
	
	private Context context;

	public TomcatServletContextWrapper(Context context) {
		this.context = context;
	}

	@Override
	public void start() {
		try {
			context.start();
		}
		catch (Exception exc) {
			LOG.error(
				"Could not start the servlet context for context path [" + context.getPath()
					+ "]", exc);
		}
	}

	@Override
	public void stop() {
		try {
			context.stop();
		}
		catch (Exception exc) {
			LOG.error(
				"Could not stop the servlet context for context path [" + context.getPath()
					+ "]", exc);
		}
	}

}
