/* Copyright 2008 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.jsp.internal;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;

import org.ops4j.pax.swissbox.core.BundleClassLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jasper enforces a URLClassLoader so he can lookup the jars in order to get the TLDs.
 * This class loader will use the Bundle-ClassPath to get the list of classloaders and delegate class loading to a
 * bundle class loader.
 *
 * @author Alin Dreghiciu
 * @author Raul Kripalani
 * @since 0.3.0 January 08, 2008
 */
public final class JasperClassLoader
    extends URLClassLoader
{

    /**
     * Internal bundle class loader.
     */
    private final BundleClassLoader m_bundleClassLoader;

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger( JasperClassLoader.class );
    
    /**
     * Class name of the classloader wrapped by this object
     */
    private static final String bundleClassLoaderClassName = "org.ops4j.pax.swissbox.core.BundleClassLoader";
    
    /**
     * Name of the standard bundle class, used by the equals method
     */
	private static final String osgiBundleClassName = "org.osgi.framework.Bundle";
	
    public JasperClassLoader( final Bundle bundle, final ClassLoader parent )
    {
        super( getClassPathJars( bundle ) );
        m_bundleClassLoader = new BundleClassLoader( bundle, parent );
    }

    /**
     * Delegate to bundle class loader.
     *
     * @see BundleClassLoader#getResource(String)
     */
    public URL getResource( String name )
    {
        return m_bundleClassLoader.getResource( name );
    }

    /**
     * Delegate to bundle class loader.
     *
     * @see BundleClassLoader#getResources(String)
     */
    public Enumeration<URL> getResources( String name )
        throws IOException
    {
        return m_bundleClassLoader.getResources( name );
    }

    /**
     * Delegate to bundle class loader.
     *
     * @see BundleClassLoader#loadClass(String)
     */
    public Class loadClass( final String name )
        throws ClassNotFoundException
    {
        return m_bundleClassLoader.loadClass( name );
    }

    @Override
    public String toString()
    {
        return new StringBuffer()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "bundleClassLoader=" ).append( m_bundleClassLoader )
            .append( "}" )
            .toString();
    }

    /**
     * Returns a list of urls to jars that composes the Bundle-ClassPath.
     *
     * @param bundle the bundle from which the class path should be taken
     *
     * @return list or urls to jars that composes the Bundle-ClassPath.
     */
    private static URL[] getClassPathJars( final Bundle bundle )
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
    	List<Bundle> importedBundles = getBundlesInClassSpace(bundle);
    	
    	try {
	    	for (Bundle importedBundle : importedBundles) {
	    		URL url = new URL(importedBundle.getLocation()); 
				urls.add(url);
	    	}
        } catch (MalformedURLException e) {
			LOG.warn("Exception while calculating location of imported bundles", e);
		}
        return urls;
	}
    
    /**
	 * Gets a list of bundles that are imported or required by this bundle.
	 * 
	 * @param 	bundle the bundle for which to perform the lookup
	 * 
	 * @return	list of imported and required bundles
	 * 	
	 */
    
    private static List<Bundle> getBundlesInClassSpace(Bundle bundle) {
    	List<Bundle> bundles = new ArrayList<Bundle>(); 
    	
		// Get package admin service.
        ServiceReference ref = bundle.getBundleContext().getServiceReference(PackageAdmin.class.getName());
        if (ref == null) {
            System.out.println("PackageAdmin service is unavailable.");
            return bundles;
        }
        try {
            PackageAdmin pa = (PackageAdmin) bundle.getBundleContext().getService(ref);
            if (pa == null) {
                System.out.println("PackageAdmin service is unavailable.");
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
       		bundle.getBundleContext().ungetService(ref);
        }
        return bundles;
	}
    
	/**
	 * Scans the imported and required bundles for matching resources. Can be used to obtain references to TLD files, XML definition files, etc.
	 * 
	 * @param 	directory 	the directory within the imported/required bundle where to perform the lookup (e.g. "META-INF/")
	 * @param 	filePattern the file pattern to lookup (e.g. "*.tld")
	 * @param	recursive	indicates whether the lookup should be recursive, i.e. if it will drill into child directories
	 * @return	list of matching resources, URLs as returned by the framework's {@link Bundle#findEntries(String, String, boolean)} method 
	 * 	
	 */
    
    public List<URL> scanBundlesInClassSpace(String directory, String filePattern, boolean recursive) {
    	List<Bundle> bundlesInClassSpace = getBundlesInClassSpace(m_bundleClassLoader.getBundle());
    	List<URL> matching = new ArrayList<URL>();
    	
    	for (Bundle bundle : bundlesInClassSpace) {
        	@SuppressWarnings("rawtypes")
    		Enumeration e = bundle.findEntries(directory, filePattern, recursive);
    		if (e == null) continue;
    		while(e.hasMoreElements()) {
    			URL u = (URL) e.nextElement();
    			matching.add(u);
    		}
    	}
    	
    	return matching;
    }
    
    @Override
    public URL[] getURLs() {
    	return super.getURLs();
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
    public boolean equals(Object o)
    {
        /*
         * Fix for PAXWEB-310 - MyFaces JSF2 is unable to serve requests due to classloader issues
         * 
         * These modifications are necessary for compatibility with JSF2 and MyFaces. The latter assumes that a webapp 
         * will only use a single, unique classloader all throughout. This assumption does not necessarily stand in OSGi environments.
         *
         */
    	
    	LOG.trace("JasperClassLoader.equals() invoked");
    	
        if (this == o) {
        	LOG.trace("JasperClassLoader.equals(): same object");
            return true;
        }
        
        if (o == null) {
        	LOG.trace("JasperClassLoader.equals(): testing equality against null object");
            return false;
        }

        // if the tested object is not of type BundleClassLoader, provide standard equals() behaviour, otherwise apply special magic
		if(!o.getClass().getCanonicalName().equals(JasperClassLoader.bundleClassLoaderClassName)) {
        	LOG.trace("JasperClassLoader.equals(): testing equality against another JasperClassLoader object");
            return super.equals(o);
        }
    
        else {
        	LOG.trace("JasperClassLoader.equals(): testing equality against a BundleClassLoader object");
        	
        	// Initialise bundle ids to differing values
        	long myBundleId = -1; 
        	long theirBundleId = -2;
        	
    		try {
    			// Get the bundle id of the BundleClassLoader wrapped by this JasperClassLoader
    			myBundleId = m_bundleClassLoader.getBundle().getBundleId();
    			
    			// Get the bundle id of the BundleClassLoader for which equality is being tested
    			// Forced to use reflection because classloaders are different
      			Class bundleClassLoaderClass = o.getClass().getClassLoader().loadClass(bundleClassLoaderClassName);
    			Object bundle = bundleClassLoaderClass.getMethod("getBundle").invoke(o, null);
            	Class bundleClass = o.getClass().getClassLoader().loadClass(osgiBundleClassName);
            	theirBundleId = (Long) bundleClass.getMethod("getBundleId").invoke(bundle, null);
            	
    		} catch (Exception e) {
    			LOG.error("Unable to evaluate equality of JasperClassLoader object against BundleClassLoader object", e);
    		} 
            
        	return myBundleId == theirBundleId;
        }
        
    }
    
    @Override
    public int hashCode()
    {
    	
        /*
         * Fix for PAXWEB-310 - MyFaces JSF2 is unable to serve requests due to classloader issues
         * 
         * Determine hashcode based on the Bundle/BundleClassLoader
         *
         */
    	
    	LOG.trace("Using m_bundleClassloader.hashCode()");
    	final Bundle bundle = m_bundleClassLoader.getBundle();
    	
    	// This operation guarantees that the JasperClassLoader will fall in the same bucket as the BundleClassLoader it is wrapping 
    	int hash = ( bundle != null ? bundle.hashCode() * 37 : m_bundleClassLoader.hashCode() );

    	LOG.trace("m_bundleClassloader.hashCode() result: " + hash);
    	
    	return hash;  
    
    }

    
    
}
