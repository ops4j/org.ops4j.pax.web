package org.ops4j.pax.web.extender.whiteboard.internal.tracker;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.ops4j.pax.web.extender.whiteboard.WelcomeFileMapping;
import org.ops4j.pax.web.extender.whiteboard.internal.ExtenderContext;
import org.ops4j.pax.web.extender.whiteboard.internal.element.WelcomeFileWebElement;

/**
 * Tracks {@link org.ops4j.pax.web.extender.whiteboard.WelcomeFileMapping}.
 *
 * @author Dmitry Sklyut
 * @since 0.7.0
 */
public class WelcomeFileMappingTracker extends AbstractTracker<WelcomeFileMapping, WelcomeFileWebElement>
{

    /**
     * Constructor.
     *
     * @param extenderContext extender context; cannot be null
     * @param bundleContext   extender bundle context; cannot be null
     */
    public WelcomeFileMappingTracker( final ExtenderContext extenderContext,
                                      final BundleContext bundleContext )
    {
        super(
            extenderContext,
            bundleContext,
            WelcomeFileMapping.class
        );
    }

    /**
     * @see AbstractTracker#createWebElement(org.osgi.framework.ServiceReference , Object)
     */
    @Override
    WelcomeFileWebElement createWebElement( final ServiceReference serviceReference,
                                            final WelcomeFileMapping published )
    {
        return new WelcomeFileWebElement( published );
    }
}