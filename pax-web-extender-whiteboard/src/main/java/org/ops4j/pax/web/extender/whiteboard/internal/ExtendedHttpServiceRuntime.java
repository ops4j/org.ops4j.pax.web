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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.ops4j.pax.web.extender.whiteboard.internal.element.WebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.util.WebContainerUtils;
import org.ops4j.pax.web.extender.whiteboard.internal.util.tracker.ReplaceableService;
import org.ops4j.pax.web.extender.whiteboard.internal.util.tracker.ReplaceableServiceListener;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardElement;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;


public class ExtendedHttpServiceRuntime implements HttpServiceRuntime, ReplaceableServiceListener<HttpService> {

    private final BundleContext bundleContext;
    /**
     * ConcurrentHashMap.KeySet is used because {@link WebApplication#registerWebElement(WebElement)} and
     * {@link WebApplication#unregisterWebElement(WebElement)} only uses a read-lock
     */
    private Set<WhiteboardElement> whiteboardElements = ConcurrentHashMap.newKeySet();
    
    /**
     * Http service tracker
     */
    private ReplaceableService<HttpService> httpServiceTracker;

    /**
     * Http service lock.
     */
    private final ReadWriteLock httpServiceLock;
    
    /**
     * Active http service;
     */
    private volatile WebContainer webContainer;
    
    /**
     * RuntimeService as OSGi service
     */
    private ServiceRegistration<HttpServiceRuntime> serviceRuntimeService;

    ExtendedHttpServiceRuntime(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.httpServiceLock = new ReentrantReadWriteLock();
        this.httpServiceTracker = new ReplaceableService<>(bundleContext, HttpService.class, this, true);
    }

    @Override
    public RuntimeDTO getRuntimeDTO() {
//        return webContainer.createWhiteboardRuntimeDTO(whiteboardElements.iterator());
		return null;
    }

    @Override
    public RequestInfoDTO calculateRequestInfoDTO(String path) {
//        return webContainer.calculateRequestInfoDTO(path, whiteboardElements.iterator());
		return null;
    }

    /**
     * In order to keep track of the elements which are registered using the whiteboard-pattern
     * each registered element must be added.
     * @param element the whiteboard-element to add.
     */
    public void addWhiteboardElement(WhiteboardElement element) {
        whiteboardElements.add(element);
    }

    /**
     * In order to keep track of the elements which are registered using the whiteboard-pattern
     * each unregistered element must be removed.
     * @param element the whiteboard-element to remove.
     */
    public void removeWhiteboardElement(WhiteboardElement element) {
        whiteboardElements.remove(element);
    }

    @Override
    public void serviceChanged(HttpService oldService, HttpService newService, Map<String, Object> serviceProperties) {
        if (newService != null && !WebContainerUtils.isWebContainer(newService)) {
            throw new IllegalStateException("HttpService must be implementing Pax-Web WebContainer!");
        }
        httpServiceLock.writeLock().lock();
        try {
            unregisterService();
            webContainer = (WebContainer)newService;
            registerService((WebContainer)newService, serviceProperties);
        } finally {
            httpServiceLock.writeLock().unlock();
        }
    }
    
    private void registerService(WebContainer newService, Map<String, Object> serviceProperties) {
        if (newService == null) {
            return;
        }
        
        Dictionary<String, Object> props = new Hashtable<>();
        
        Long id = (Long) serviceProperties.get("service.id");
        String endpointString = "";
        
//        if (newService.getWebcontainerDTO().listeningAddresses != null && newService.getWebcontainerDTO().listeningAddresses.length > 0) {
//            endpointString = newService.getWebcontainerDTO().listeningAddresses[0] + ":";
//        }
//
//        endpointString += newService.getWebcontainerDTO().port;
        
        List<Long> idList = new ArrayList<>();
        idList.add(id);
        
        props.put(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT, endpointString);
        props.put(HttpServiceRuntimeConstants.HTTP_SERVICE_ID, idList);
        
        serviceRuntimeService = bundleContext.registerService(HttpServiceRuntime.class, this, props);
    }

    private void unregisterService() {
        if (serviceRuntimeService != null) {
            serviceRuntimeService.unregister();
            serviceRuntimeService = null;
        }
    }
    
    public void start() {
        httpServiceTracker.start();
    }
    
    public void stop() {
        unregisterService();
        httpServiceTracker.stop();
    }

    
}
