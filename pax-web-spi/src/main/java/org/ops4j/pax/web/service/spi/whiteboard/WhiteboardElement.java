/* Copyright 2012 Harald Wellmann
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
package org.ops4j.pax.web.service.spi.whiteboard;

/**
 * WhiteboardElements and its subtypes are used to establish a link between http-whiteboard-services generated
 * by the whiteboard-extender and the runtime (webcontainer)
 */
public interface WhiteboardElement {

    /**
     * Only valid elements will be added to the webcontainer. Use this method to validate http-whiteboard-services
     * @return true, if registered service is valid with regards to the implementing WhiteboardElement.
     */
    boolean isValid();

    /**
     * Service-Id is necessary for WhiteboardDTO
     * @return Service-ID assigned by runtime
     * @see org.osgi.framework.ServiceReference#getProperty(String)
     * @see org.osgi.framework.Constants#SERVICE_ID
     */
    long getServiceID();
}
