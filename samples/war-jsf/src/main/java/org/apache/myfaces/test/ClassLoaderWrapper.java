package org.apache.myfaces.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

public class ClassLoaderWrapper extends ClassLoader
{
    private ClassLoader contextLoader;

    private ClassLoader bundleLoader;

    public ClassLoaderWrapper(ClassLoader contextLoader,
            ClassLoader bundleLoader)
    {
        this.contextLoader = contextLoader;
        this.bundleLoader = bundleLoader;
    }

    public ClassLoader getContextLoader()
    {
        return contextLoader;
    }

    public ClassLoader getBundleLoader()
    {
        return bundleLoader;
    }

    public void clearAssertionStatus()
    {
        contextLoader.clearAssertionStatus();
    }

    public URL getResource(String name)
    {
        final URL url = contextLoader.getResource(name);
        return url == null ? bundleLoader.getResource(name) : url;
    }

    public InputStream getResourceAsStream(String name)
    {
        final InputStream is = contextLoader.getResourceAsStream(name);
        return is == null ? bundleLoader.getResourceAsStream(name) : is;
    }

    public Enumeration<URL> getResources(String name) throws IOException
    {
        final Enumeration<URL> e = contextLoader.getResources(name);
        return e == null ? bundleLoader.getResources(name) : e;
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException
    {
        final Class<?> c = contextLoader.loadClass(name);
        return c == null ? bundleLoader.loadClass(name) : c;
    }

    public void setClassAssertionStatus(String className, boolean enabled)
    {
        contextLoader.setClassAssertionStatus(className, enabled);
    }

    public void setDefaultAssertionStatus(boolean enabled)
    {
        contextLoader.setDefaultAssertionStatus(enabled);
    }

    public void setPackageAssertionStatus(String packageName, boolean enabled)
    {
        contextLoader.setPackageAssertionStatus(packageName, enabled);
    }

    public String toString()
    {
        return "ClassLoaderWrapper["+contextLoader.toString()+","+bundleLoader.toString()+"]";
    }

    public boolean equals(Object obj)
    {
        return contextLoader.equals(obj);
    }

    public int hashCode()
    {
        return contextLoader.hashCode();
    }
}