/* Copyright 2016 Marc Schlegel, Achim Nierbeck
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
package org.ops4j.pax.web.service.internal;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;

import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.ServiceModel;
import org.ops4j.pax.web.service.whiteboard.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.runtime.dto.ErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedFilterDTO;
import org.osgi.service.http.runtime.dto.FailedListenerDTO;
import org.osgi.service.http.runtime.dto.FailedServletContextDTO;
import org.osgi.service.http.runtime.dto.FailedServletDTO;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.ListenerDTO;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;

@Component(immediate = true, service = WhiteboardDtoService.class)
public class WhiteboardDtoService {

    private volatile Map<ServiceReference<ServletContext>, ServletContext> servletContexts = new ConcurrentHashMap<>(5);

    private BundleContext bundleContext;

    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }


    private boolean compareContextroot(ServletContext servletContext, String httpServicePath){
        String servletContextPath = servletContext.getContextPath();

        if (servletContextPath == null || servletContextPath.trim().length() == 0) {
            servletContextPath = "/";
        }

        if(httpServicePath == null || httpServicePath.trim().length() == 0){
            httpServicePath = "/";
        } else if (!httpServicePath.startsWith("/")) {
            httpServicePath = "/" + httpServicePath;
        }

        return servletContextPath.equals(httpServicePath);
    }

    public RuntimeDTO createWhiteboardRuntimeDTO(Iterator<WhiteboardElement> iterator, ServerModel serverModel, ServiceModel serviceModel) {
        // FIXME not complete

        RuntimeDTO runtimeDto = new RuntimeDTO();
        List<ServletContextDTO> servletContextDTOs = new ArrayList<>();
        List<FailedServletContextDTO> failedServletContextDTOs = new ArrayList<>();
        List<ServletDTO> servletDTOs = new ArrayList<>();
        List<FailedServletDTO> failedServletDTOs = new ArrayList<>();
        List<FilterDTO> filterDTOs = new ArrayList<>(); 
        List<FailedFilterDTO> failedFilterDTOs = new ArrayList<>();
        List<ErrorPageDTO> errorPageDTOs = new ArrayList<>();
        List<FailedErrorPageDTO> failedErrorPageDTOs = new ArrayList<>();
        List<ListenerDTO> listenerDTOs = new ArrayList<>();
        List<FailedListenerDTO> failedListenerDTOs = new ArrayList<>();

        //FIXME: more lists ... 


        iterator.forEachRemaining(element -> {
            if (element instanceof WhiteboardHttpContext) { // also matches WhiteboardServletContextHelper
                WhiteboardHttpContext whiteboardHttpContext = (WhiteboardHttpContext) element;
                Optional<Map.Entry<ServiceReference<ServletContext>, ServletContext>> matchingServletContextEntry =
                        servletContexts.entrySet().stream()
                                .filter(entry -> compareContextroot(entry.getValue(), whiteboardHttpContext.getHttpContextMapping().getPath()))
                                .findFirst();
                if(!whiteboardHttpContext.isValid()){
                    FailedServletContextDTO dto = new FailedServletContextDTO();
                    dto.failureReason = DTOConstants.FAILURE_REASON_VALIDATION_FAILED;
                    dto.serviceId = whiteboardHttpContext.getServiceID();
                    dto.contextPath = whiteboardHttpContext.getHttpContextMapping().getPath();
                    failedServletContextDTOs.add(dto);
                } else if (!matchingServletContextEntry.isPresent()) {
                    // element is valid, but no actual ServletContext exist
                    FailedServletContextDTO dto = new FailedServletContextDTO();
                    dto.failureReason = DTOConstants.FAILURE_REASON_EXCEPTION_ON_INIT;
                    dto.serviceId = whiteboardHttpContext.getServiceID();
                    dto.contextPath = whiteboardHttpContext.getHttpContextMapping().getPath();
                    failedServletContextDTOs.add(dto);
                } else {
                    // all fine
                    servletContextDTOs.add(this.transformToDTO(matchingServletContextEntry.get()));
                }
            } else if (element  instanceof WhiteboardServlet) {
                if (element.isValid()) {
                    servletDTOs.add(transformToDTO((WhiteboardServlet)element));
                } else {
                    failedServletDTOs.add(transformToDTOFailed((WhiteboardServlet)element));
                }
            } else if (element instanceof WhiteboardFilter) {
                if (element.isValid()) {
                    filterDTOs.add(transformToDTO((WhiteboardFilter)element));
                } else {
                    failedFilterDTOs.add(transformToDTOFailed((WhiteboardFilter)element));
                }
            } else if (element instanceof WhiteboardErrorPage) {
                if (element.isValid()) {
                    errorPageDTOs.add(transformToDTO((WhiteboardErrorPage)element));
                } else {
                    failedErrorPageDTOs.add(transformToDTOFailed((WhiteboardErrorPage)element));
                }
            } else if (element instanceof WhiteboardJspMapping) {
                if (element.isValid()) {
                    //TODO: map jsps to servlets
                } else {
                    failedServletDTOs.add(transformToDTOFailed((WhiteboardJspMapping)element));
                }
            } else if (element instanceof WhiteboardListener) {
                if (element.isValid()) {
                    listenerDTOs.add(tranformToDTO((WhiteboardListener) element));
                } else {
                    failedListenerDTOs.add(transformToDTOFailed((WhiteboardListener)element));
                }
            } else if (element instanceof WhiteboardResource) {
                //TODO: add resources
            } else if (element instanceof WhiteboardWelcomeFile) {
                //TODO: add welcomefiles
            }
        });

        // check for root-context: if not available due to some whiteboard-service, the default/shared-context might be available
        if (!servletContextDTOs.stream().anyMatch(servletContextDTO -> "/".equals(servletContextDTO.contextPath))) {
            servletContextDTOs.addAll(servletContexts.entrySet().stream().filter(entry ->
                    WebContainerContext.DefaultContextIds.DEFAULT.getValue().equals(entry.getValue().getServletContextName())
                            || WebContainerContext.DefaultContextIds.SHARED.getValue().equals(entry.getValue().getServletContextName()))
                    .map(this::transformToDTO)
                    .collect(Collectors.toList()));

        }
        runtimeDto.servletContextDTOs = servletContextDTOs.toArray(new ServletContextDTO[servletContextDTOs.size()]);
        runtimeDto.failedServletContextDTOs = failedServletContextDTOs.toArray(new FailedServletContextDTO[servletContextDTOs.size()]);


        Arrays.stream(runtimeDto.servletContextDTOs).forEach(servletContextDTO -> {
            // map lists to correct context
            servletContextDTO.servletDTOs = servletDTOs.stream()
                    .filter(servletDTO -> servletDTO.servletContextId == servletContextDTO.serviceId)
                    .toArray(ServletDTO[]::new);
            servletContextDTO.filterDTOs = filterDTOs.stream()
                    .filter(filterDTO -> filterDTO.servletContextId == servletContextDTO.serviceId)
                    .toArray(FilterDTO[]::new);
            servletContextDTO.errorPageDTOs = errorPageDTOs.stream()
                    .filter(errorPageDTO -> errorPageDTO.servletContextId == servletContextDTO.serviceId)
                    .toArray(ErrorPageDTO[]::new);
            servletContextDTO.listenerDTOs = listenerDTOs.stream()
                    .filter(listenerDTO -> listenerDTO.servletContextId == servletContextDTO.serviceId)
                    .toArray(ListenerDTO[]::new);
        });
        
        

        runtimeDto.failedServletContextDTOs = failedServletContextDTOs.stream().toArray(FailedServletContextDTO[]::new);
        runtimeDto.failedServletDTOs = failedServletDTOs.stream().toArray(FailedServletDTO[]::new);
        runtimeDto.failedFilterDTOs = failedFilterDTOs.stream().toArray(FailedFilterDTO[]::new);
        runtimeDto.failedErrorPageDTOs = failedErrorPageDTOs.stream().toArray(FailedErrorPageDTO[]::new);
        runtimeDto.failedListenerDTOs = failedListenerDTOs.stream().toArray(FailedListenerDTO[]::new);

        return runtimeDto;
    }

    public RequestInfoDTO calculateRequestInfoDTO(String path, Iterator<WhiteboardElement> iterator, ServerModel serverModel, ServiceModel serviceModel) {
        // FIXME TBD
        return null;
    }
    
    private ServletContextDTO transformToDTO(Map.Entry<ServiceReference<ServletContext>, ServletContext> mapEntry) {
        final ServiceReference<ServletContext> ref = mapEntry.getKey();
        final ServletContext servletContext = mapEntry.getValue();

        ServletContextDTO dto = new ServletContextDTO();
        dto.serviceId = (long) ref.getProperty(Constants.SERVICE_ID);
        // the actual ServletContext might use "" instead of "/" (depends on the
        // container). DTO must use "/" for root
        dto.contextPath = servletContext.getContextPath().trim().length() == 0 ? "/" : servletContext.getContextPath();
        dto.name = servletContext.getServletContextName();

        dto.attributes = Collections.list(servletContext.getAttributeNames()).stream()
                .map(name -> new SimpleEntry<>(name, servletContext.getAttribute(name)))
                .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
        dto.initParams = Collections.list(servletContext.getInitParameterNames()).stream()
                .map(name -> new SimpleEntry<>(name, servletContext.getInitParameter(name)))
                .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

        return dto;
    }


    private ServletDTO transformToDTO(WhiteboardServlet whiteBoardServlet) {
        ServletDTO dto = new ServletDTO();

        ServletMapping servletMapping = whiteBoardServlet.getServletMapping();

        Optional<Map.Entry<ServiceReference<ServletContext>, ServletContext>> matchingServletContextEntry = findMatchingServletContext(
                servletMapping.getHttpContextId());

        if (matchingServletContextEntry.isPresent()) {
            dto.servletContextId = (long) matchingServletContextEntry.get().getKey().getProperty(Constants.SERVICE_ID);
        } else {
            // FIXME something wrong...what to do
        }
        dto.serviceId = whiteBoardServlet.getServiceID();
        dto.name = servletMapping.getServletName();
        dto.initParams = servletMapping.getInitParams();

        // FIXME: not complete
        return dto;
    }

    private FilterDTO transformToDTO(WhiteboardFilter whiteboardFilter) {
        FilterDTO dto = new FilterDTO();
        dto.asyncSupported = whiteboardFilter.getFilterMapping().getAsyncSupported(); 
        dto.initParams = whiteboardFilter.getFilterMapping().getInitParams();
        dto.patterns = whiteboardFilter.getFilterMapping().getUrlPatterns();
        dto.servletNames = whiteboardFilter.getFilterMapping().getServletNames();
        dto.serviceId = whiteboardFilter.getServiceID();
        
        Optional<Map.Entry<ServiceReference<ServletContext>, ServletContext>> matchingServletContextEntry = findMatchingServletContext(
                whiteboardFilter.getFilterMapping().getHttpContextId());

        if (matchingServletContextEntry.isPresent()) {
            dto.servletContextId = (long) matchingServletContextEntry.get().getKey().getProperty(Constants.SERVICE_ID);
        } else {
            // FIXME something wrong...what to do
        }
        
        return dto;
    }
    
    private ErrorPageDTO transformToDTO(WhiteboardErrorPage whiteboardErrorPage) {
        ErrorPageDTO dto = new ErrorPageDTO();
        
        try {
            long code = Long.parseLong(whiteboardErrorPage.getErrorPageMapping().getError());
            dto.errorCodes = new long[] {code};
        } catch (NumberFormatException nfe) {
            // OK, not a number must be a class then
            dto.exceptions = new String[] {whiteboardErrorPage.getErrorPageMapping().getError()};
        }
        
        dto.serviceId = whiteboardErrorPage.getServiceID();

//        whiteboardErrorPage.getErrorPageMapping().getLocation();
        //FIXME: the errorpage location is never used by the errorpage dto ... 
        // what really bothers me, the errorpagedto is based on top of a servlet dto, 
        // but error pages aren't servlets !!!
        
        Optional<Map.Entry<ServiceReference<ServletContext>, ServletContext>> matchingServletContextEntry = findMatchingServletContext(
                whiteboardErrorPage.getErrorPageMapping().getHttpContextId());
        
        if (matchingServletContextEntry.isPresent()) {
            dto.servletContextId = (long) matchingServletContextEntry.get().getKey().getProperty(Constants.SERVICE_ID);
        } else {
            // FIXME something wrong...what to do
        }
        
        return dto;
    }
    
    private ListenerDTO tranformToDTO(WhiteboardListener whiteboardListener) {
        ListenerDTO dto = new ListenerDTO();
        
        EventListener listener = whiteboardListener.getListenerMapping().getListener();
        dto.types = new String[] { listener.getClass().getName() };
        
        Optional<Map.Entry<ServiceReference<ServletContext>, ServletContext>> matchingServletContextEntry = findMatchingServletContext(
                whiteboardListener.getListenerMapping().getHttpContextId());
        
        if (matchingServletContextEntry.isPresent()) {
            dto.servletContextId = (long) matchingServletContextEntry.get().getKey().getProperty(Constants.SERVICE_ID);
        } else {
            // FIXME something wrong...what to do
        }
        
        return dto;
    }

    private FailedServletDTO transformToDTOFailed(WhiteboardServlet whiteboardServlet) {
        FailedServletDTO dto = new FailedServletDTO();

        dto.serviceId = whiteboardServlet.getServiceID();
        dto.failureReason = DTOConstants.FAILURE_REASON_VALIDATION_FAILED; 
        // FIXME: not the only possible reason
        
        // FIXME: not complete
        return dto;
    }
    
    private FailedFilterDTO transformToDTOFailed(WhiteboardFilter whiteboardFilter) {
        FailedFilterDTO dto = new FailedFilterDTO();
        
        dto.serviceId = whiteboardFilter.getServiceID();
        dto.failureReason = DTOConstants.FAILURE_REASON_VALIDATION_FAILED;
        //FIXME: ... could be some arbitrary reasons
        
        return dto;
    }
    
    private FailedErrorPageDTO transformToDTOFailed(WhiteboardErrorPage whiteboardErrorPage) {
        FailedErrorPageDTO dto = new FailedErrorPageDTO();
        
        dto.serviceId = whiteboardErrorPage.getServiceID();
        dto.failureReason = DTOConstants.FAILURE_REASON_VALIDATION_FAILED;
        
        return dto;
    }
    
    private FailedListenerDTO transformToDTOFailed(WhiteboardListener whiteboardListener) {
        FailedListenerDTO dto = new FailedListenerDTO();
        
        dto.serviceId = whiteboardListener.getServiceID();
        dto.failureReason = DTOConstants.FAILURE_REASON_VALIDATION_FAILED;
        
        return dto;
    }
    
    private FailedServletDTO transformToDTOFailed(WhiteboardJspMapping whiteboardJspMapping) {
        FailedServletDTO dto = new FailedServletDTO();
        
        dto.serviceId = whiteboardJspMapping.getServiceID();
        dto.failureReason = DTOConstants.FAILURE_REASON_VALIDATION_FAILED;
        
        return dto;
    }

    private Optional<Map.Entry<ServiceReference<ServletContext>, ServletContext>> findMatchingServletContext(
            String httpContextId) {
        final String name;
        if (httpContextId == null || httpContextId.trim().length() == 0) {
            // FIXME not nice, but currently the only way to map to context
            // (could also be the shared context)
            name = WebContainerContext.DefaultContextIds.DEFAULT.getValue();
        } else {
            name = httpContextId;
        }
        return this.servletContexts.entrySet().stream()
                .filter(entry -> Objects.equals(entry.getValue().getServletContextName(), name)).findFirst();
    }

    @Reference(unbind = "removeServletContext", service = ServletContext.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addServletContext(ServiceReference<ServletContext> ref, ServletContext servletContext) {
        servletContexts.put(ref, servletContext);
    }

    protected void removeServletContext(ServiceReference<ServletContext> ref, ServletContext servletContext) {
        servletContexts.remove(ref);
    }
}
