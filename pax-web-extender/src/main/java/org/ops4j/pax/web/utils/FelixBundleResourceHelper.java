package org.ops4j.pax.web.utils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.apache.xbean.osgi.bundle.util.BundleResourceHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;


public class FelixBundleResourceHelper extends BundleResourceHelper {
    
    
    private List<Bundle> bundles;

    public FelixBundleResourceHelper(Bundle bundle, List<Bundle> bundles) {
        super(bundle);
        this.bundles = bundles;
    }
    
    @Override
    public URL getResource(String name) {
        if (!name.contains("META-INF/")) {
            return super.getResource(name); 
        }
        for (Bundle delegate : bundles) {
            try {
                URL resource = delegate.getResource(name);
                if (resource != null) {
                    BundleRevision revision = delegate.adapt(BundleRevision.class);
                    Method method = revision.getClass().getMethod("getLocalURL", int.class,
                        String.class);
                    if (resource.getProtocol().equals("bundle")) {
                        return (URL) method.invoke(revision, 0, resource.getPath());
                    }
                    else {
                        return resource;
                    }
                }
            }
            catch (IllegalStateException | NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException | InvocationTargetException exc) {
                // ignore
            }
        }
        return null;
    }
    
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (!name.contains("META-INF/")) {
            return super.getResources(name); 
        }
        Vector<URL> resources = new Vector<URL>();

        for (Bundle delegate : bundles) {
            try {
                BundleWiring wiring = delegate.adapt(BundleWiring.class);
                Enumeration<URL> urls = wiring.getClassLoader().getResources(name);
                BundleRevision revision = delegate.adapt(BundleRevision.class);
                Method method = revision.getClass().getMethod("getLocalURL", int.class,
                    String.class);
                if (urls != null) {
                    while (urls.hasMoreElements()) {
                        URL url = urls.nextElement();
                        if (url.getProtocol().equals("bundle")) {
                            resources.add((URL) method.invoke(revision, 0, url.getPath()));
                        }
                        else {
                            resources.add(url);
                        }
                    }
                }
            }
            catch (IllegalStateException | NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException | InvocationTargetException exc) {
                // ignore
            }
        }

        return resources.elements();
    }

}
