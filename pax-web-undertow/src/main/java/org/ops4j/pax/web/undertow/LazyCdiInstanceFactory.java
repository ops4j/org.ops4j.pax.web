/*
 * Copyright 2014 Harald Wellmann.
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
