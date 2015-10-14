package org.ops4j.pax.web.jsf.resourcehandler.extender;

import javax.faces.application.Resource;
import javax.faces.application.ViewResource;

import org.ops4j.pax.web.jsf.resourcehandler.extender.internal.IndexedOsgiResourceLocator;
import org.osgi.framework.Bundle;

/**
 * <p>
 * Services implementing this interface must be able to serve
 * {@link ViewResource}s and {@link Resource}s from other bundles.
 * </p>
 * <p>
 * Instead of providing another custom {@link Resource}-implementation,
 * the {@link OsgiResource} can be used.
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
	 * Lookup the given resource according to JSF 2 specification.
	 * 
	 * @param resourceName
	 *            name or path of the resource to find
	 * @return {@code Resource} matching the given name, or {@code null}
	 */
	Resource createResource(String resourceName);

	/**
	 * Lookup the given resource according to JSF 2 specification.
	 * 
	 * @param resourceName
	 *            name or path of the resource to find
	 * @param libraryName
	 *            the library, or {@code null} when no library is used
	 * @return {@code Resource} matching the given name and library (optional),
	 *         or {@code null}
	 */
	Resource createResource(String resourceName, String libraryName);

	/**
	 * 
	 * @param resourceName
	 *            name or path of the resource to find
	 * @return {@code ViewResource} matching the given name, or null
	 */
	ViewResource createViewResource(String resourceName);

}
