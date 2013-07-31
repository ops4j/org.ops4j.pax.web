package org.ops4j.pax.web.itest.support;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Filter;

import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardFilter;
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

}