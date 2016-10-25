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
package org.ops4j.pax.web.extender.whiteboard.internal;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import org.ops4j.pax.web.extender.whiteboard.internal.element.WebElement;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.whiteboard.WhiteboardElement;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;


public class ExtendedHttpServiceRuntime implements HttpServiceRuntime {

    private final BundleContext bundleContext;
    /**
     * ConcurrentLinkedQueue is used because {@link WebApplication#registerWebElement(WebElement)} and
     * {@link WebApplication#unregisterWebElement(WebElement)} only uses a read-lock
     */
    private Queue<WhiteboardElement> whiteboardElements = new ConcurrentLinkedQueue<>();


//    /**
//     * Dynamic Reference to the Pax-Web Webcontainer.
//     * Volatile marks this service as dynamic for DS.
//     */
//    private volatile WebContainer webContainer;


    ExtendedHttpServiceRuntime(BundleContext bundleContext){
        this.bundleContext = bundleContext;
    }

    /*
    Using DS would be easier here, but then the whole module should be designed around DS
    Until then, the cheapest solution is to get the service if needed.
     */
    private <T> T executeWithWebContainer(Function<WebContainer, T> function){
        T result = null;
        ServiceReference<WebContainer> serviceReference = bundleContext.getServiceReference(WebContainer.class);
        if(serviceReference != null){
            WebContainer webContainer = bundleContext.getService(serviceReference);
            if(webContainer != null) {
                result = function.apply(webContainer);
                bundleContext.ungetService(serviceReference);
            }
        }
        return result;
    }


    @Override
    public RuntimeDTO getRuntimeDTO() {
        return executeWithWebContainer(webContainer ->
                webContainer.createWhiteboardRuntimeDTO(whiteboardElements.iterator()));
    }

    @Override
    public RequestInfoDTO calculateRequestInfoDTO(String path) {
        return executeWithWebContainer(webContainer ->
                webContainer.calculateRequestInfoDTO(whiteboardElements.iterator()));
    }

    /**
     * In order to keep track of the elements which are registered using the whiteboard-pattern
     * each registered element must be added.
     * @param element the whiteboard-element to add.
     */
    public void addWhiteboardElement(WhiteboardElement element){
        whiteboardElements.add(element);
    }

    /**
     * In order to keep track of the elements which are registered using the whiteboard-pattern
     * each unregistered element must be removed.
     * @param element the whiteboard-element to remove.
     */
    public void removeWhiteboardElement(WhiteboardElement element){
        whiteboardElements.remove(element);
    }
}
