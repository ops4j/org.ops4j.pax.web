package org.ops4j.pax.web.utils;


import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.List;

import org.apache.xbean.osgi.bundle.util.BundleResourceHelper;
import org.apache.xbean.osgi.bundle.util.DelegatingBundle;
import org.apache.xbean.osgi.bundle.util.DelegatingBundleReference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

public class FelixBundleClassLoader extends URLClassLoader implements DelegatingBundleReference {

    private final List<Bundle> bundles;
    private final BundleResourceHelper resourceHelper;
    private Bundle bundle;
    
    public FelixBundleClassLoader(List<Bundle> bundles) {
        super(new URL[] {});
        this.bundles = bundles;
        this.bundle = new DelegatingBundle(bundles);
        this.resourceHelper = new FelixBundleResourceHelper(bundle, bundles);
    }
    
    @Override
    public String toString() {
        return "[FelixBundleClassLoader] " + bundles;
    }
  
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = bundle.loadClass(name);
        if (resolve) {
            resolveClass(clazz);
        }
        return clazz;
    }
    
    @Override
    public URL getResource(String name) {
        return resourceHelper.getResource(name);
    }
    
    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        return resourceHelper.getResources(name);
    }
    
    /**
     * Return the bundle associated with this classloader.
     * 
     * In most cases the bundle associated with the classloader is a regular framework bundle. 
     * However, in some cases the bundle associated with the classloader is a {@link DelegatingBundle}.
     * In such cases, the <tt>unwrap</tt> parameter controls whether this function returns the
     * {@link DelegatingBundle} instance or the main application bundle backing with the {@link DelegatingBundle}.
     *
     * @param unwrap If true and if the bundle associated with this classloader is a {@link DelegatingBundle}, 
     *        this function will return the main application bundle backing with the {@link DelegatingBundle}. 
     *        Otherwise, the bundle associated with this classloader is returned as is.
     * @return The bundle associated with this classloader.
     */
    public Bundle getBundle(boolean unwrap) {
        if (unwrap && bundle instanceof DelegatingBundle) {
            return ((DelegatingBundle) bundle).getMainBundle();
        }
        return bundle;
    }
    
    /**
     * Return the bundle associated with this classloader.
     * 
     * This method calls {@link #getBundle(boolean) getBundle(true)} and therefore always returns a regular 
     * framework bundle.  
     * <br><br>
     * Note: Some libraries use {@link BundleReference#getBundle()} to obtain a bundle for the given 
     * classloader and expect the returned bundle instance to be work with any OSGi API. Some of these API might
     * not work if {@link DelegatingBundle} is returned. That is why this function will always return
     * a regular framework bundle. See {@link #getBundle(boolean)} for more information.
     *
     * @return The bundle associated with this classloader.
     */
    public Bundle getBundle() {
        return getBundle(true);
    }
    
    @Override
    public int hashCode() {
        return bundle.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || !other.getClass().equals(getClass())) {
            return false;
        }
        FelixBundleClassLoader otherBundleClassLoader = (FelixBundleClassLoader) other;
        return this.bundle == otherBundleClassLoader.bundle;
    }
    
}
