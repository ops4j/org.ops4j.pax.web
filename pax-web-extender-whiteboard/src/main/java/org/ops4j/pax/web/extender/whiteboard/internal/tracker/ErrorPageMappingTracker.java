package org.ops4j.pax.web.extender.whiteboard.internal.tracker;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.ops4j.pax.web.extender.whiteboard.ErrorPageMapping;
import org.ops4j.pax.web.extender.whiteboard.internal.ExtenderContext;
import org.ops4j.pax.web.extender.whiteboard.internal.element.ErrorPageWebElement;

/**
 * Tracks {@link org.ops4j.pax.web.extender.whiteboard.ErrorPageMapping}.
 *
 * @author Dmitry Sklyut
 * @since 0.7.0
 */
public class ErrorPageMappingTracker extends AbstractTracker<ErrorPageMapping, ErrorPageWebElement>
{

    /**
     * Constructor.
     *
     * @param extenderContext extender context; cannot be null
     * @param bundleContext   extender bundle context; cannot be null
     */
    public ErrorPageMappingTracker( final ExtenderContext extenderContext,
                                    final BundleContext bundleContext )
    {
        super(
            extenderContext,
            bundleContext,
            ErrorPageMapping.class
        );
    }

    /**
     * @see AbstractTracker#createWebElement(org.osgi.framework.ServiceReference , Object)
     */
    @Override
    ErrorPageWebElement createWebElement( final ServiceReference serviceReference,
                                          final ErrorPageMapping published )
    {
        return new ErrorPageWebElement( published );
    }
}
