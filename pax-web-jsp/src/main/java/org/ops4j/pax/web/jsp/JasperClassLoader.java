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
package org.ops4j.pax.web.jsp;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ops4j.pax.swissbox.core.BundleClassLoader;
import org.ops4j.pax.web.utils.ClassPathUtil;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jasper enforces a URLClassLoader so he can lookup the jars in order to get the TLDs.
 * This class loader will use the Bundle-ClassPath to get the list of classloaders and delegate class loading to a
 * bundle class loader.
 *
 * @author Alin Dreghiciu
 * @author Raul Kripalani
 * @author Marc Klinger - mklinger[at]nightlabs[dot]de
 * @since 0.3.0 January 08, 2008
 */
public class JasperClassLoader
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
        super( ClassPathUtil.getClassPathJars( bundle ), parent );
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

        /*
         * Fix for PAXWEB-450 - Issue with constructor when running under Websphere.
         */
        if (m_bundleClassLoader == null) {
            return super.hashCode();
        }

    	final Bundle bundle = m_bundleClassLoader.getBundle();
    	
    	// This operation guarantees that the JasperClassLoader will fall in the same bucket as the BundleClassLoader it is wrapping 
    	int hash = ( bundle != null ? bundle.hashCode() * 37 : m_bundleClassLoader.hashCode() );

    	LOG.trace("m_bundleClassloader.hashCode() result: " + hash);
    	
    	return hash;  
    
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
    	Set<Bundle> bundlesInClassSpace = ClassPathUtil.getBundlesInClassSpace(m_bundleClassLoader.getBundle(), new HashSet<Bundle>());
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
    
}
