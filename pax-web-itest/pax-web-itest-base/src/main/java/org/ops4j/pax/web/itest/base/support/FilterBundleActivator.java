package org.ops4j.pax.web.itest.base.support;

import java.util.Hashtable;

import javax.servlet.Filter;

import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterBundleActivator implements BundleActivator {

	private static final transient Logger LOG = LoggerFactory
			.getLogger(FilterBundleActivator.class);
	private ServiceRegistration<Filter> filterReg;

	@Override
	public void start(BundleContext context) throws Exception {

		// register a filter
		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put(ExtenderConstants.PROPERTY_URL_PATTERNS, "/sharedContext/*");
		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "shared");
		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_SHARED, "true");
		filterReg = context.registerService(Filter.class,
				new SimpleOnlyFilter(), props);

	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (filterReg != null) {
			filterReg.unregister();
		}
	}

}
