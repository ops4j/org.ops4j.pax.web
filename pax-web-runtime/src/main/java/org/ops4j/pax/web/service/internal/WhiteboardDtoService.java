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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;

import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.whiteboard.ServletMapping;
import org.ops4j.pax.web.service.whiteboard.WhiteboardElement;
import org.ops4j.pax.web.service.whiteboard.WhiteboardErrorPage;
import org.ops4j.pax.web.service.whiteboard.WhiteboardFilter;
import org.ops4j.pax.web.service.whiteboard.WhiteboardJspMapping;
import org.ops4j.pax.web.service.whiteboard.WhiteboardListener;
import org.ops4j.pax.web.service.whiteboard.WhiteboardResource;
import org.ops4j.pax.web.service.whiteboard.WhiteboardServlet;
import org.ops4j.pax.web.service.whiteboard.WhiteboardWelcomeFile;
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
import org.osgi.service.http.runtime.dto.FailedServletContextDTO;
import org.osgi.service.http.runtime.dto.FailedServletDTO;
import org.osgi.service.http.runtime.dto.FilterDTO;
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

    public RuntimeDTO createWhiteboardRuntimeDTO(Iterator<WhiteboardElement> iterator) {
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
        //FIXME: more lists ... 

        runtimeDto.servletContextDTOs = this.servletContexts.entrySet().stream()
                .map(this::transformToDTO)
                .toArray(ServletContextDTO[]::new);


        iterator.forEachRemaining(element -> {
            if (element  instanceof WhiteboardServlet) {
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
                //TODO: add jsp mappings
            } else if (element instanceof WhiteboardListener) {
                //TODO: add Listeners
            } else if (element instanceof WhiteboardResource) {
                //TODO: add resources
            } else if (element instanceof WhiteboardWelcomeFile) {
                //TODO: add welcomefiles
            }
        });

        Arrays.stream(runtimeDto.servletContextDTOs).forEach(servletContextDTO -> {
            // map lists to correct context
            servletContextDTO.servletDTOs = servletDTOs.stream()
                    .filter(servletDTO -> servletDTO.servletContextId == servletContextDTO.serviceId)
                    .toArray(ServletDTO[]::new);
        });
        
        Arrays.stream(runtimeDto.servletContextDTOs).forEach(servletContextDTO -> {
            servletContextDTO.filterDTOs = filterDTOs.stream()
                    .filter(filterDTO -> filterDTO.servletContextId == servletContextDTO.serviceId)
                    .toArray(FilterDTO[]::new);
        });
        
        Arrays.stream(runtimeDto.servletContextDTOs).forEach(servletContextDTO -> {
            servletContextDTO.errorPageDTOs = errorPageDTOs.stream()
                    .filter(errorPageDTO -> errorPageDTO.servletContextId == servletContextDTO.serviceId)
                    .toArray(ErrorPageDTO[]::new);
        });

        runtimeDto.failedServletContextDTOs = failedServletContextDTOs.stream().toArray(FailedServletContextDTO[]::new);
        runtimeDto.failedServletDTOs = failedServletDTOs.stream().toArray(FailedServletDTO[]::new);
        runtimeDto.failedFilterDTOs = failedFilterDTOs.stream().toArray(FailedFilterDTO[]::new);
        runtimeDto.failedErrorPageDTOs = failedErrorPageDTOs.stream().toArray(FailedErrorPageDTO[]::new);

        return runtimeDto;
    }

    public RequestInfoDTO calculateRequestInfoDTO(Iterator<WhiteboardElement> iterator) {
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
                .map(name -> new SimpleEntry(name, servletContext.getAttribute(name)))
                .collect(Collectors.toMap(entry -> entry.getKey().toString(), entry -> entry.getValue()));
        dto.initParams = Collections.list(servletContext.getInitParameterNames()).stream()
                .map(name -> new SimpleEntry(name, servletContext.getInitParameter(name)))
                .collect(Collectors.toMap(entry -> entry.getKey().toString(), entry -> entry.getValue().toString()));

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
