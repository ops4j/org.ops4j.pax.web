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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.ops4j.pax.swissbox.core.BundleClassLoader;

/**
 * Jasper enforces a URLClassLoader so he can lookup the jars in order to get the TLDs.
 * This class loader will use the Bundle-ClassPath to get the list of classloaders and delegate class loading to a
 * bundle class loader.
 *
 * @author Alin Dreghiciu
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
        urls.addAll(getImportedBundles(bundle));
        return urls.toArray( new URL[urls.size()] );
    }

    private static List<URL> getImportedBundles(Bundle bundle) {
    	
    	List<URL> urls = new ArrayList<URL>(); 
    	
		// Get package admin service.
        ServiceReference ref = bundle.getBundleContext().getServiceReference(PackageAdmin.class.getName());
        if (ref == null) {
            System.out.println("PackageAdmin service is unavailable.");
            return urls;
        }
        try {
            PackageAdmin pa = (PackageAdmin) bundle.getBundleContext().getService(ref);
            if (pa == null) {
                System.out.println("PackageAdmin service is unavailable.");
                return urls;
            }
            
            Dictionary headers = bundle.getHeaders();
            String importPackage = (String) headers.get("Import-Package");
            String[] importPackages = importPackage.split(",");
            
            for (String impPackage : importPackages) {
            	String[] split = impPackage.split(";");
            	String name = split[0];
            	if (name.matches("^[0-9].*"))
            		continue; //we splitted into a version range jump over it. 
				ExportedPackage[] exportedPackages = pa.getExportedPackages(name);
            	if (exportedPackages != null) {
	            	for (ExportedPackage exportedPackage : exportedPackages) {
						if (Arrays.asList(exportedPackage.getImportingBundles()).contains(bundle)) {
							Bundle exportingBundle = exportedPackage.getExportingBundle();
							//skip System-Bundle
							if (exportingBundle.getBundleId() == 0)
								continue;
							URL url = null;
							String location = exportingBundle.getLocation();
							try {
								url = new URL(location);
							} catch (MalformedURLException mfe) {
								//it's ok here just skip this one then
							}
							if (url != null)
								urls.add(url);
						}
					}
            	}
			}
            
        } finally {
       		bundle.getBundleContext().ungetService(ref);
        }
        return urls;
	}
    
    @Override
    public URL[] getURLs() {
    	return super.getURLs();
    }
}
