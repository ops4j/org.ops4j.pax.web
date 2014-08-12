package org.ops.pax.web.spi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;


public class WabModel {

    private WebAppModel webAppModel;

    private Bundle bundle;
    private ClassLoader classLoader;

    private boolean beanBundle;
    private String contextPath;
    private ServiceRegistration<ServletContext> servletContextRegistration;
    private List<String> virtualHosts = new ArrayList<>();
    private List<ServletContainerInitializerModel> servletContainerInitializers = new ArrayList<>();
    private File explodedDir;

    /**
     * @return the webAppModel
     */
    public WebAppModel getWebAppModel() {
        return webAppModel;
    }

    /**
     * @param webAppModel the webAppModel to set
     */
    public void setWebAppModel(WebAppModel webAppModel) {
        this.webAppModel = webAppModel;
    }

    /**
     * @return the bundle
     */
    public Bundle getBundle() {
        return bundle;
    }

    /**
     * @param bundle the bundle to set
     */
    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    /**
     * @return the classLoader
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * @param classLoader the classLoader to set
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * @return the beanBundle
     */
    public boolean isBeanBundle() {
        return beanBundle;
    }

    /**
     * @param beanBundle the beanBundle to set
     */
    public void setBeanBundle(boolean beanBundle) {
        this.beanBundle = beanBundle;
    }

    /**
     * @return the contextPath
     */
    public String getContextPath() {
        return contextPath;
    }

    /**
     * @param contextPath the contextPath to set
     */
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    /**
     * @return the servletContextRegistration
     */
    public ServiceRegistration<ServletContext> getServletContextRegistration() {
        return servletContextRegistration;
    }

    /**
     * @param servletContextRegistration the servletContextRegistration to set
     */
    public void setServletContextRegistration(
        ServiceRegistration<ServletContext> servletContextRegistration) {
        this.servletContextRegistration = servletContextRegistration;
    }

    /**
     * @return the virtualHosts
     */
    public List<String> getVirtualHosts() {
        return virtualHosts;
    }

    /**
     * @param virtualHosts the virtualHosts to set
     */
    public void setVirtualHosts(List<String> virtualHosts) {
        this.virtualHosts = virtualHosts;
    }


    /**
     * @return the servletContainerInitializers
     */
    public List<ServletContainerInitializerModel> getServletContainerInitializers() {
        return servletContainerInitializers;
    }


    /**
     * @param servletContainerInitializers the servletContainerInitializers to set
     */
    public void setServletContainerInitializers(
        List<ServletContainerInitializerModel> servletContainerInitializers) {
        this.servletContainerInitializers = servletContainerInitializers;
    }


    /**
     * @return the explodedDir
     */
    public File getExplodedDir() {
        return explodedDir;
    }


    /**
     * @param explodedDir the explodedDir to set
     */
    public void setExplodedDir(File explodedDir) {
        this.explodedDir = explodedDir;
    }


}
