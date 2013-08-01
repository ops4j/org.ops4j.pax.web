package org.ops4j.pax.web.itest.base.support;

import java.io.IOException;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestActivator implements BundleActivator {

	private static final Logger LOG = LoggerFactory
			.getLogger(TestActivator.class);

	private ServiceRegistration<Filter> filterReg;

	@Override
	public void start(BundleContext context) throws Exception {
		Dictionary<String, String> props;
		// register a filter
		props = new Hashtable<String, String>();
		props.put(ExtenderConstants.PROPERTY_URL_PATTERNS, "/filtered/*");
		filterReg = context.registerService(Filter.class,
				new WhiteboardFilter(), props);

		LOG.info("Test activator started ... ");
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (filterReg != null) {
			filterReg.unregister();
			filterReg = null;
		}
	}
	
	public class WhiteboardFilter implements Filter {

		public void init(FilterConfig filterConfig) throws ServletException {
			LOG.info("Initialized");
		}

		public void doFilter(ServletRequest request, ServletResponse response,
				FilterChain chain) throws IOException, ServletException {
			response.getWriter().println(
					"Filter was there before. Time: " + new Date().toString());
			chain.doFilter(request, response);
			response.getWriter().println(
					"Filter was there after. Time: " + new Date().toString());
			response.getWriter().close();
		}

		public void destroy() {
			LOG.info("Destroyed");
		}
	}

}