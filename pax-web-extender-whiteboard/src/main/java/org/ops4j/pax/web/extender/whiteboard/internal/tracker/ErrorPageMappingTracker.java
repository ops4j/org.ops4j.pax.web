package org.ops4j.pax.web.extender.whiteboard.internal.tracker;

import org.ops4j.pax.web.extender.whiteboard.ErrorPageMapping;
import org.ops4j.pax.web.extender.whiteboard.internal.ExtenderContext;
import org.ops4j.pax.web.extender.whiteboard.internal.element.ErrorPageWebElement;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tracks {@link org.ops4j.pax.web.extender.whiteboard.ErrorPageMapping}.
 * 
 * @author Dmitry Sklyut
 * @since 0.7.0
 */
public class ErrorPageMappingTracker extends
		AbstractTracker<ErrorPageMapping, ErrorPageWebElement> {

	/**
	 * Constructor.
	 * 
	 * @param extenderContext
	 *            extender context; cannot be null
	 * @param bundleContext
	 *            extender bundle context; cannot be null
	 */
	private ErrorPageMappingTracker(final ExtenderContext extenderContext,
			final BundleContext bundleContext) {
		super(extenderContext, bundleContext);
	}

	public static ServiceTracker<ErrorPageMapping, ErrorPageWebElement> createTracker(
			final ExtenderContext extenderContext,
			final BundleContext bundleContext) {
		return new ErrorPageMappingTracker(extenderContext, bundleContext)
				.create(ErrorPageMapping.class);
	}

	/**
	 * @see AbstractTracker#createWebElement(org.osgi.framework.ServiceReference
	 *      , Object)
	 */
	@Override
	ErrorPageWebElement createWebElement(
			final ServiceReference<ErrorPageMapping> serviceReference,
			final ErrorPageMapping published) {
		return new ErrorPageWebElement(published);
	}
}
