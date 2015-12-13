package org.ops4j.pax.web.jsf.resourcehandler.extender;

import java.util.function.Function;
import java.util.function.Supplier;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.application.ResourceHandlerWrapper;
import javax.faces.application.ViewResource;
import javax.faces.context.FacesContext;

import org.ops4j.pax.web.jsf.resourcehandler.extender.internal.IndexedOsgiResourceLocator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * This ResourceHandler can be used in OSGi-enabled JSF applications to access
 * resources in other bundles.
 * <p>
 * It will first try to find resources provided by the appication. If none was
 * found it will lookup an instance of a {@link OsgiResourceLocator} to find the
 * requested resource in other bundles.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * Bundles providing resources must set the <strong>Manifest-Header</strong>
 * <code>WebResources: true</code>.
 * </p>
 * <p>
 * This class has to be configured in the applications
 * <strong>faces-config.xml</strong>.
 * 
 * <pre>
 * {@literal
 * <?xml version="1.0" encoding="UTF-8"?>
 * <faces-config xmlns="http://xmlns.jcp.org/xml/ns/javaee"
 *     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *     xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-facesconfig_2_2.xsd"
 *     version="2.2">
 *   <application>
 *     <resource-handler>org.ops4j.pax.web.jsf.resourcehandler.extender.OsgiResourceHandler</resource-handler>
 *   </application>
 * </faces-config>
 * }
 * </pre>
 * </p>
 * 
 * @see IndexedOsgiResourceLocator
 */
public class OsgiResourceHandler extends ResourceHandlerWrapper {

	private final ResourceHandler wrapped;

	public OsgiResourceHandler(ResourceHandler wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public ResourceHandler getWrapped() {
		return wrapped;
	}

	@Override
	public ViewResource createViewResource(FacesContext context, String resourceName) {
		return getResource(() -> super.createViewResource(context, resourceName),
				x -> x.createViewResource(resourceName));
	}

	@Override
	public Resource createResource(String resourceName) {
		return getResource(() -> super.createResource(resourceName), x -> x.createResource(resourceName, null));
	}

	@Override
	public Resource createResource(String resourceName, String libraryName) {

		return getResource(() -> super.createResource(resourceName, libraryName),
				x -> x.createResource(resourceName, libraryName));
	}
	

	/**
	 * Will first attempt to retrieve a resource via the first given function.
	 * If that failed, the second function will be used.
	 * 
	 * @param firstFunction
	 *            the function which is used to retrieve a resource in the first
	 *            place.
	 * @param secondFunction
	 *            the fallback-function to apply against the
	 *            {@link OsgiResourceLocator} after the first attempt did not
	 *            yied any resource.
	 * @return a {@link Resource}, {@link ViewResource} depending on the
	 *         functions or {@code null}.
	 */
	private <R extends ViewResource> R getResource(Supplier<R> firstFunction,
			Function<OsgiResourceLocator, R> secondFunction) {
		// check standard first
		R resource = firstFunction.get();
		if (resource == null) {
			// lookup resource in jsf-resource bundles
			resource = getServiceAndExecute(secondFunction);
		}
		return resource;
	}

	/**
	 * Gets a {@link OsgiResourceLocator}-service, applies the given function,
	 * and ungets the service.
	 * 
	 * @param function
	 *            the function to apply against the {@link OsgiResourceLocator}
	 * @return a {@link Resource}, {@link ViewResource} depending on the
	 *         functions or {@code null}.
	 */
	private <R extends ViewResource> R getServiceAndExecute(Function<OsgiResourceLocator, R> function) {
		// hook into OSGi-Framework
		final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
		// get-service, execute function, and unget-service
		ServiceReference<OsgiResourceLocator> serviceRef = context.getServiceReference(OsgiResourceLocator.class);
		R resource = null;
		if (serviceRef != null) {
			OsgiResourceLocator resourceLocatorService = context.getService(serviceRef);
			if (resourceLocatorService != null) {
				resource = function.apply(resourceLocatorService);
				resourceLocatorService = null;
			}
		}
		context.ungetService(serviceRef);

		return resource;
	}
}
