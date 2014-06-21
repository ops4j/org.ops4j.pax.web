package org.ops4j.pax.web.undertow;

import io.undertow.servlet.api.ClassIntrospecter;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.InstanceHandle;

import java.util.Map;


public class LazyInstanceFactory<T> implements InstanceFactory<T> {
    
    
    private DeploymentInfo deploymentInfo;
    private Class<T> klass;

    public LazyInstanceFactory(DeploymentInfo deploymentInfo, Class<T> klass) {
        this.deploymentInfo = deploymentInfo;
        this.klass = klass;
    }

    @Override
    public InstanceHandle<T> createInstance() throws InstantiationException {
        Map<String, Object> attributes = (Map<String, Object>) deploymentInfo.getServletContextAttributes().get("org.ops4j.pax.web.attributes");
        ClassIntrospecter introspector = (ClassIntrospecter) attributes.get("org.ops4j.pax.cdi.ClassIntrospecter");
        try {
            InstanceFactory<T> instanceFactory = introspector.createInstanceFactory(klass);
            return instanceFactory.createInstance();
        }
        catch (NoSuchMethodException exc) {
            throw new InstantiationException(exc.getMessage());
        }
    }

}
