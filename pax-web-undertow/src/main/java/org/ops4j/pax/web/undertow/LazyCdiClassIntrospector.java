package org.ops4j.pax.web.undertow;

import io.undertow.servlet.api.ClassIntrospecter;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.InstanceFactory;

public class LazyCdiClassIntrospector implements ClassIntrospecter {

    private DeploymentInfo deploymentInfo;

    public LazyCdiClassIntrospector(DeploymentInfo deploymentInfo) {
        this.deploymentInfo = deploymentInfo;
    }

    @Override
    public <T> InstanceFactory<T> createInstanceFactory(Class<T> klass)
        throws NoSuchMethodException {
        return new LazyCdiInstanceFactory<T>(deploymentInfo, klass);
    }
}
