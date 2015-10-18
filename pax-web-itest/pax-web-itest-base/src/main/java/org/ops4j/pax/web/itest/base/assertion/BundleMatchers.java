package org.ops4j.pax.web.itest.base.assertion;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.Arrays;

import static org.ops4j.pax.web.itest.base.assertion.Assert.assertThat;

public class BundleMatchers {

    public static void isBundleActive(String bundleSymbolicName, BundleContext bundleContext) {
        assertThat(String.format("Bundle '%s' must be active", bundleSymbolicName),
                Arrays.stream(bundleContext.getBundles()),
                bundles ->
                        bundles.filter(
                                bundle -> bundle.getState() == Bundle.ACTIVE
                                        && bundle.getSymbolicName().equals(bundleSymbolicName)).count() == 1);
    }

}
