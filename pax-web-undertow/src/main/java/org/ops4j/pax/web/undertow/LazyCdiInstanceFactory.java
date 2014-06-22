package org.ops4j.pax.web.undertow;

import io.undertow.servlet.api.ClassIntrospecter;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.servlet.util.DefaultClassIntrospector;

import java.util.Map;

public class LazyCdiInstanceFactory<T> implements InstanceFactory<T> {

    private DeploymentInfo deploymentInfo;
    private Class<T> klass;

    public LazyCdiInstanceFactory(DeploymentInfo deploymentInfo, Class<T> klass) {
        this.deploymentInfo = deploymentInfo;
        this.klass = klass;
    }

    @Override
    public InstanceHandle<T> createInstance() throws InstantiationException {
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = (Map<String, Object>) deploymentInfo
            .getServletContextAttributes().get("org.ops4j.pax.web.attributes");
        ClassIntrospecter introspector = (ClassIntrospecter) attributes
            .get("org.ops4j.pax.cdi.ClassIntrospecter");
        try {
            if (introspector == null) {
                return DefaultClassIntrospector.INSTANCE.createInstanceFactory(klass)
                    .createInstance();
            }
            else {
                InstanceFactory<T> instanceFactory = introspector.createInstanceFactory(klass);
                return instanceFactory.createInstance();
            }
        }
        catch (NoSuchMethodException exc) {
            throw new InstantiationException(exc.getMessage());
        }
    }
}
