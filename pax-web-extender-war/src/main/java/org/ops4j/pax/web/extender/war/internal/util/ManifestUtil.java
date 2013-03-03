/**
 * 
 */
package org.ops4j.pax.web.extender.war.internal.util;

import java.util.Dictionary;

import org.ops4j.pax.web.extender.war.internal.BundleServletScanner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author achim
 *
 */
public class ManifestUtil {
	
	private static final Logger LOG = LoggerFactory.getLogger(BundleServletScanner.class);
	
	private ManifestUtil() {
		//utility class
	}
	
	/**
     * @param bundle
     * @return
     */
    public static String getHeader(final Bundle bundle, String...keys) {
    	BundleContext bundleContext = bundle.getBundleContext();
    	
        // Look in the bundle...
        Dictionary<String,String> headers = bundle.getHeaders();
        for(String key:keys) {
            String value = (String) headers.get(key);
            if( value != null )
            {
                return value;
            }
        }

        // Next, look in the bundle's fragments.
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle fragment : bundles) {
        	//only fragments are in resolved state
            if (fragment.getState() != Bundle.RESOLVED) 
                continue;

            // A fragment must also have the FRAGMENT_HOST header and the FRAGMENT_HOST header
            // must be equal to the bundle symbolic name
            String fragmentHost = (String) fragment.getHeaders().get(Constants.FRAGMENT_HOST);
            if ((fragmentHost == null) || (!fragmentHost.equals(bundle.getSymbolicName()))) {
            	continue;
            }
            headers = fragment.getHeaders();
            for(String key:keys) {
                String value = (String) headers.get(key);
                if( value != null )
                {
                    return value;
                }
            }
        }
        return null;
    }


	/**
	 * @param bundle
	 * @return
	 */
	public static String extractContextName(final Bundle bundle) {
		// set the context name as first looking for a manifest entry named Web-ContextPath
        String contextName = getHeader(bundle, "Web-ContextPath", "Webapp-Context");
        // if not found use the old pax Webapp-Context
        if( contextName == null )
        {
            LOG.debug( "No 'Web-ContextPath' or 'Webapp-Context' manifest attribute specified" );
            final String symbolicName = bundle.getSymbolicName();
            if( symbolicName == null )
            {
                contextName = String.valueOf( bundle.getBundleId() );
                LOG.debug( String.format( "Using bundle id [%s] as context name", contextName ) );
            }
            else
            {
                contextName = symbolicName;
                LOG.debug( String.format( "Using bundle symbolic name [%s] as context name", contextName ) );
            }
        }
        contextName = contextName.trim();
        if( contextName.startsWith( "/" ) )
        {
            contextName = contextName.substring( 1 );
        }
		return contextName;
	}

}
