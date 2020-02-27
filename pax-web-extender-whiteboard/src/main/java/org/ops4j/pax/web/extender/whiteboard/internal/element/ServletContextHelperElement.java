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
import org.ops4j.pax.web.extender.whiteboard.internal.util.ServicePropertiesUtils;
import org.ops4j.pax.web.service.whiteboard.HttpContextMapping;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardServletContextHelper;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServletContextHelperElement<T extends ServletContextHelper> extends HttpContextElement implements WhiteboardServletContextHelper {


    private static final Logger LOG = LoggerFactory.getLogger(ServletContextHelperElement.class);

    private final T contextHelper;

    public ServletContextHelperElement(final ServiceReference<T> ref,
                                       final HttpContextMapping contextMapping,
                                       final T contextHelper) {
        super(ref, contextMapping);
        NullArgumentException.validateNotNull(contextHelper, "ServletContextHelper");
        this.contextHelper = contextHelper;

        // validate
        String servletCtxtName = ServicePropertiesUtils.getStringProperty(serviceReference,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME);

        if (servletCtxtName == null || servletCtxtName.trim().length() == 0) {
            LOG.warn("Registered ServletContextHelper [" + getPusblishedPID() + "] did not contain a valid name");
            valid = false;
        }

        String ctxtPath = ServicePropertiesUtils.getStringProperty(serviceReference,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH);
        if (ctxtPath == null || ctxtPath.trim().length() == 0) {
            LOG.warn("Registered ServletContextHelper [" + getPusblishedPID() + "] did not contain a valid path");
            valid = false;
        }
    }


    @Override
    public ServletContextHelper getServletContextHelper() {
        return contextHelper;
    }
}
