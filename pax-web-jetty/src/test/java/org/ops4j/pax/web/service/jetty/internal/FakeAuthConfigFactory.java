/*
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
package org.ops4j.pax.web.service.jetty.internal;

import java.util.Map;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.security.auth.message.config.RegistrationListener;

public class FakeAuthConfigFactory extends AuthConfigFactory {

    @Override
    public String[] detachListener(RegistrationListener listener, String layer, String appContext) {
        return new String[0];
    }

    @Override
    public AuthConfigProvider getConfigProvider(String layer, String appContext, RegistrationListener listener) {
        return null;
    }

    @Override
    public RegistrationContext getRegistrationContext(String registrationID) {
        return null;
    }

    @Override
    public String[] getRegistrationIDs(AuthConfigProvider provider) {
        return new String[0];
    }

    @Override
    public void refresh() {

    }

    @Override
    public String registerConfigProvider(AuthConfigProvider provider, String layer, String appContext, String description) {
        return null;
    }

    @Override
    public String registerConfigProvider(String className, Map properties, String layer, String appContext, String description) {
        return null;
    }

    @Override
    public boolean removeRegistration(String registrationID) {
        return false;
    }

}
