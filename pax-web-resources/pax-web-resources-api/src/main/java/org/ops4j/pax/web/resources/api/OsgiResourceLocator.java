package org.ops4j.pax.web.resources.api;

import org.osgi.framework.Bundle;

/**
 * <p>
 * Services implementing this interface must be able to serve
 * {@link ResourceInfo}s from other bundles.
 * </p>
 * 
 * @see IndexedOsgiResourceLocator
 */
public interface OsgiResourceLocator {

	/**
	 * <p>
	 * Register the given bundle to take part in the lookup-process for JSF
	 * resources.
	 * </p>
	 * <p>
	 * This method is called from the BundleListener in this module.
	 * </p>
	 * 
	 * @param bundle
	 *            the starting bundle containing JSF resources to share
	 */
	void register(Bundle bundle);

	/**
	 * <p>
	 * Unregister the given bundle from the lookup-process for JSF resources.
	 * Resources must be cleaned.
	 * </p>
	 * <p>
	 * This method is called from the BundleListener in this module.
	 * </p>
	 * 
	 * @param bundle
	 *            the stopping bundle containing JSF resources
	 */
	void unregister(Bundle bundle);

	/**
	 * Lookup the given resource according to Servlet 3.0 specification.
	 * 
	 * @param resourceName
	 *            name or path of the resource to find
	 * @return {@code ResourceInfo} matching the given name, or {@code null}
	 */
	ResourceInfo locateResource(String resourceName);
}
