/*
 * Copyright 2007 Damian Golda.
 * Copyright 2007 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.whiteboard.internal.element;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.whiteboard.HttpContextMapping;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardHttpContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpContextElement implements WhiteboardHttpContext {

    private static final Logger LOG = LoggerFactory.getLogger(HttpContextElement.class);

    protected final ServiceReference serviceReference;
    protected boolean valid = true;
    private final HttpContextMapping contextMapping;

    public HttpContextElement(final ServiceReference ref, final HttpContextMapping contextMapping) {
        NullArgumentException.validateNotNull(ref, "service-reference");
        NullArgumentException.validateNotNull(contextMapping, "Context mapping");
        this.serviceReference = ref;
        this.contextMapping = contextMapping;

        // validate
        String httpContextId = contextMapping.getContextId();
        if ( httpContextId == null || httpContextId.trim().length() == 0) {
            LOG.warn("Registered http context [" + getPusblishedPID() + "] did not contain a valid http context id");
            valid = false;
        }
    }

    @Override
    public HttpContextMapping getHttpContextMapping() {
        return contextMapping;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public long getServiceID() {
        return (Long)serviceReference.getProperty(Constants.SERVICE_ID);
    }


    /**
     * Return the PID of the registered whiteboard-service. Used mainly for logging.
     * @return PID from registered whiteboard-service
     */
    protected String getPusblishedPID() {
        return (String)serviceReference.getProperty(Constants.SERVICE_PID);
    }

}
