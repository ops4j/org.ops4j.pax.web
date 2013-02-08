/*
 * Copyright 2012 Achim Nierbeck.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.ops4j.pax.web.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author achim
 *
 */
public class ClassPathUtil {
	
	/**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger( ClassPathUtil.class );

	/**
	 * Returns a list of urls to jars that composes the Bundle-ClassPath.
	 *
	 * @param bundle the bundle from which the class path should be taken
	 *
	 * @return list or urls to jars that composes the Bundle-ClassPath.
	 */
	public static URL[] getClassPathJars( final Bundle bundle )
	{
	    final List<URL> urls = new ArrayList<URL>();
	    final String bundleClasspath = (String) bundle.getHeaders().get( "Bundle-ClassPath" );
	    if( bundleClasspath != null )
	    {
	        String[] segments = bundleClasspath.split( "," );
	        for( String segment : segments )
	        {
	            final URL url = bundle.getEntry( segment );
	            if( url != null )
	            {
	                if( url.toExternalForm().endsWith( "jar" ) )
	                {
	                    LOG.debug( "Using url: " + url );
	                    try
	                    {
	                        URL jarUrl = new URL( "jar:" + url.toExternalForm() + "!/" );
	                        urls.add( jarUrl );
	                    }
	                    catch( MalformedURLException ignore )
	                    {
	                        LOG.debug( ignore.getMessage() );
	                    }
	                }
	            }
	        }
	    }
	    LOG.debug( "Bundle-ClassPath URLs: " + urls );
	    //adds the depending bundles to the "classloader" space
	    urls.addAll(getLocationsOfBundlesInClassSpace(bundle));
	    return urls.toArray( new URL[urls.size()] );
	}

	/**
	 * Gets the locations of the bundles in the Class Space. Beware, in Karaf this will return the URL with which the bundle
	 * was originally provisioned, i.e. could potentially return wrap:..., mvn:..., etc. and even include URL parameters (i.e. ?Webapp-Context=...).
	 * 
	 * @param bundle the bundle for which to perform the lookup
	 * 
	 * @return	list of locations of bundles in class space
	 * 	
	 */
	
	private static List<URL> getLocationsOfBundlesInClassSpace(Bundle bundle) {
		List<URL> urls = new ArrayList<URL>();
		Set<Bundle> importedBundles = getBundlesInClassSpace(bundle, new HashSet<Bundle>());
		for (Bundle importedBundle : importedBundles) {
			URL url = getLocationOfBundle(importedBundle);
	        if (url != null) {
	        	urls.add(url);
	        }
		}
	    return urls;
	}

	private static URL getLocationOfBundle(Bundle importedBundle) {
		URL url = null;
		try {
			url = new URL(importedBundle.getLocation()); 
		} catch (MalformedURLException e) {
			try {
			url = importedBundle.getEntry("/");
			} catch (Exception e2) {
				LOG.warn("Exception while calculating location of bundle", e);
			}
		}
		return url;
	}

	/**
	 * Gets a list of bundles that are imported or required by this bundle.
	 * 
	 * @param 	bundle the bundle for which to perform the lookup
	 * 
	 * @return	list of imported and required bundles
	 * 	
	 */
    public static Set<Bundle> getBundlesInClassSpace(Bundle bundle, Set<Bundle> bundleSet) {
        return getBundlesInClassSpace(bundle.getBundleContext(), bundle, bundleSet);
    }

    private static Set<Bundle> getBundlesInClassSpace(BundleContext context, Bundle bundle, Set<Bundle> bundleSet) {
		Set<Bundle> bundles = new HashSet<Bundle>(); 
		if (bundle == null) {
			LOG.error("Incoming bundle is null");
			return bundles; 
		}
		if (context == null) {
			LOG.error("Incoming context is null");
			return bundles;
		}
		
		// Get package admin service.
	    ServiceReference ref = context.getServiceReference(PackageAdmin.class.getName());
	    if (ref == null) {
	        LOG.error("PackageAdmin service is unavailable.");
	        return bundles;
	    }
	    try {
	        PackageAdmin pa = (PackageAdmin) context.getService(ref);
	        if (pa == null) {
	            LOG.error("PackageAdmin service is unavailable.");
	            return bundles;
	        }
	        
	        Dictionary headers = bundle.getHeaders();
	        String importPackage = (String) headers.get("Import-Package");
	        String requiredBundleHeader = (String) headers.get("Require-Bundle");
	        
	        // Process the Import-Package header
	        if (importPackage != null) {
	            String[] importPackages = importPackage.split(",");
	            for (String impPackage : importPackages) {
	            	String[] split = impPackage.split(";");
	            	String name = split[0].trim();
	            	if (name.matches("^[0-9].*"))
	            		continue; //we split into a version range jump over it. 
					ExportedPackage[] exportedPackages = pa.getExportedPackages(name);
	            	if (exportedPackages != null) {
		            	for (ExportedPackage exportedPackage : exportedPackages) {
							if (Arrays.asList(exportedPackage.getImportingBundles()).contains(bundle)) {
								Bundle exportingBundle = exportedPackage.getExportingBundle();
								//skip System-Bundle
								if (exportingBundle.getBundleId() == 0)
									continue;
								if (!bundles.contains(exportingBundle))
									bundles.add(exportingBundle);
							}
						}
	            	}
				}
	        }
	        
	        // Process the Require-Bundle header 
	        if (requiredBundleHeader != null) {
	        	String[] reqBundles = requiredBundleHeader.split(",");
	            for (String reqBundle : reqBundles) {
	            	String[] split = reqBundle.split(";");
	            	String name = split[0].trim();
	            	if (name.matches("^[0-9].*"))
	            		continue; //we split into a version range jump over it. 
					RequiredBundle[] requiredBundles = pa.getRequiredBundles(name);
					if (requiredBundles != null) {
		            	for (RequiredBundle requiredBundle : requiredBundles) {
							if (Arrays.asList(requiredBundle.getRequiringBundles()).contains(bundle)) {
								if (requiredBundle.getBundle().getBundleId() == 0)
									continue;
								bundles.add(requiredBundle.getBundle());
							}
						}
	            	}
				}
	        }
	        
	    }
	    finally {
            context.ungetService(ref);
	    }
	    Set<Bundle> transitiveBundles = new HashSet();
	    
	    if (!bundleSet.containsAll(bundles)) {
	    	bundles.removeAll(bundleSet);
	    	bundleSet.addAll(bundles);
	    	for (Bundle importedBundle : bundles) {
                transitiveBundles.addAll(getBundlesInClassSpace(context, importedBundle, bundleSet));
	    	}
	    }
	    return bundleSet;
	}
	
}
