/**
 * 
 */
package org.ops4j.pax.web.extender.war.internal;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;

import org.apache.xbean.finder.BundleAnnotationFinder;
import org.ops4j.pax.swissbox.extender.BundleScanner;
import org.ops4j.pax.web.extender.war.internal.util.ManifestUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author achim
 *
 */
public class BundleServletScanner implements BundleScanner<String> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BundleServletScanner.class);

	private static final String OSGI_WIRING_PACKAGE_NAMESPACE = "osgi.wiring.package";

	private static final String OSGI_WIRING_BUNDLE_NAMESPACE = "osgi.wiring.bundle";

	private static final String FILTER_DIRECTIVE = "filter";

	private static final String JAVAX_SERVLET_NAMESPACE = "javax.servlet";

	private static final Pattern PACKAGE_PATTERN_SERVLET = Pattern
			.compile("(" + OSGI_WIRING_PACKAGE_NAMESPACE + "="
					+ Pattern.quote(JAVAX_SERVLET_NAMESPACE) + ".*)");

	private final BundleContext bundleContext;

	public BundleServletScanner(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	/* (non-Javadoc)
	 * @see org.ops4j.pax.swissbox.extender.BundleScanner#scan(org.osgi.framework.Bundle)
	 */
	@Override
	public List<String> scan(Bundle bundle) {
		List<String> servletClasses = null;
		if (ManifestUtil.getHeader(bundle, "Web-ContextPath", "Webapp-Context") != null && isImportingServlet(bundle) && !containsWebXML(bundle)) {
			servletClasses = new ArrayList<String>();
			
			LOGGER.debug("scanning for annotated classes");
			
			BundleAnnotationFinder baf = createBundleAnnotationFinder(bundle);
			
			Set<Class> webServletClasses = new LinkedHashSet<Class>(baf.findAnnotatedClasses(WebServlet.class));
			Set<Class> webFilterClasses = new LinkedHashSet<Class>(baf.findAnnotatedClasses(WebFilter.class));
			Set<Class> webListenerClasses = new LinkedHashSet<Class>(baf.findAnnotatedClasses(WebListener.class));
			
			for (Class clazz : webListenerClasses) {
				servletClasses.add(clazz.getSimpleName());
			}
			
			for (Class clazz : webFilterClasses) {
				servletClasses.add(clazz.getSimpleName());
			}
			
			for (Class clazz : webServletClasses) {
				servletClasses.add(clazz.getSimpleName());
			}
			
			LOGGER.debug("class scanning done");
		}
		return servletClasses;
	}
	
	private boolean containsWebXML(Bundle bundle) {
		try {
			Enumeration<URL> resources = bundle.getResources("web.xml");
			if (resources == null)
				return false;
			return resources.hasMoreElements();
		} catch (IOException e) {
			return false;
		}
	}

	public boolean isImportingServlet(Bundle bundle) {
		BundleWiring bundleWiring = (BundleWiring) bundle.adapt(BundleWiring.class);
		// First check if there is a wiring to any package of org.apache.wicket
		List<BundleWire> importPackageWires = bundleWiring
				.getRequiredWires(OSGI_WIRING_PACKAGE_NAMESPACE);
		for (BundleWire bundleWire : importPackageWires) {
			BundleRequirement requirement = bundleWire.getRequirement();
			String filter = (String) requirement.getDirectives().get(FILTER_DIRECTIVE);
			if (filter != null) {
				Matcher matcher = PACKAGE_PATTERN_SERVLET.matcher(filter);
				if (matcher.find()) {
					return true;
				}
			}
		}
		List<BundleWire> requireBundleWires = bundleWiring
				.getRequiredWires(OSGI_WIRING_BUNDLE_NAMESPACE);
		if (!requireBundleWires.isEmpty()) {
			// find all apache.wicket bundles and check if there are wirings...
			Bundle[] bundles = bundleContext.getBundles();
			for (Bundle bundleCheck : bundles) {
				String symbolicName = bundleCheck.getSymbolicName();
				if (symbolicName.startsWith(JAVAX_SERVLET_NAMESPACE)) {
					Map<String, Object> map = new HashMap<String, Object>();
			        map.put(OSGI_WIRING_BUNDLE_NAMESPACE, symbolicName);
			        map.put("version", bundleCheck.getVersion());
					if (hasWireMatchingFilter(requireBundleWires, map)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean hasWireMatchingFilter(List<BundleWire> wires,
			Map<String, ?> map) {
		for (BundleWire bundleWire : wires) {
			BundleRequirement requirement = bundleWire.getRequirement();
			if (matchFilter((String)requirement.getDirectives().get(FILTER_DIRECTIVE),
					map)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param filterString
	 * @param map
	 * @return
	 */
	public boolean matchFilter(String filterString, Map<String, ?> map) {
		if (filterString != null) {
			try {
				Filter filter = bundleContext.createFilter(filterString);
				if (filter.matches(map)) {
					LOGGER.trace("filter = {} matches {}", map);
					return true;
				} else {
					LOGGER.trace("filter = {} not matches {}",
							map);
				}
			} catch (InvalidSyntaxException e) {
				LOGGER.warn("can't parse filter expression: {}", filterString);
			}

		}
		return false;
	}
	
	public static BundleAnnotationFinder createBundleAnnotationFinder(Bundle bundle) {
		       ServiceReference sr = bundle.getBundleContext().getServiceReference(PackageAdmin.class.getName());
		       PackageAdmin pa = (PackageAdmin) bundle.getBundleContext().getService(sr);
		       BundleAnnotationFinder baf = null;
		       try {
		           baf = new BundleAnnotationFinder(pa, bundle);
		       } catch (Exception e) {
		           LOGGER.warn("can't create BundleAnnotation finder");
		           e.printStackTrace();
		       }
		
		       bundle.getBundleContext().ungetService(sr);
		       
		       return baf;
		   }
}
