package org.apache.myfaces.test;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

public class ContextListener implements ServletRequestListener
{
    public void requestInitialized(ServletRequestEvent sre)
    {
        // TODO Auto-generated method stub
        ClassLoaderWrapper wrapper = new ClassLoaderWrapper(
                Thread.currentThread().getContextClassLoader(), 
                this.getClass().getClassLoader());
        Thread.currentThread().setContextClassLoader(wrapper);
    }

    public void requestDestroyed(ServletRequestEvent sre)
    {
        ClassLoaderWrapper wrapper = (ClassLoaderWrapper) 
            Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(wrapper.getContextLoader());
    }
}
