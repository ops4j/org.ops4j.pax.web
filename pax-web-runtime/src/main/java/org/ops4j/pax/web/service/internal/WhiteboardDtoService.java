/*
 * Copyright 2016 Marc Schlegel, Achim Nierbeck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
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
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.whiteboard.ErrorPageMapping;
import org.ops4j.pax.web.service.whiteboard.ServletMapping;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardElement;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardErrorPage;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardFilter;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardHttpContext;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardJspMapping;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardListener;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardResource;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardServlet;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWelcomeFile;
import org.osgi.dto.DTO;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.runtime.dto.ErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedFilterDTO;
import org.osgi.service.http.runtime.dto.FailedListenerDTO;
import org.osgi.service.http.runtime.dto.FailedResourceDTO;
import org.osgi.service.http.runtime.dto.FailedServletContextDTO;
import org.osgi.service.http.runtime.dto.FailedServletDTO;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.ListenerDTO;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.runtime.dto.ResourceDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;

//@Component(immediate = true, service = WhiteboardDtoService.class)
public class WhiteboardDtoService {

    private volatile Map<ServiceReference<ServletContext>, ServletContext> servletContexts = new ConcurrentHashMap<>(5);

    @Activate
    protected void activate(BundleContext bundleContext) {
    }


    private boolean compareContextroot(ServletContext servletContext, String httpServicePath) {
        String servletContextPath = servletContext.getContextPath();

        if (servletContextPath == null || servletContextPath.trim().length() == 0) {
            servletContextPath = "/";
        }

        String comparePath;
        if (httpServicePath == null || httpServicePath.trim().length() == 0) {
            comparePath = "/";
        } else if (!httpServicePath.startsWith("/")) {
            comparePath = "/" + httpServicePath;
        } else {
            comparePath = httpServicePath;
        }

        return servletContextPath.equals(comparePath);
    }

    RuntimeDTO createWhiteboardRuntimeDTO(Iterator<WhiteboardElement> iterator, ServerModel serverModel) {
        // TODO not complete

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
        List<ResourceDTO> resourceDTOs = new ArrayList<>();
        List<FailedResourceDTO> failedResourceDTOs = new ArrayList<>();

        //TODO: more lists ...


        iterator.forEachRemaining(element -> {
            if (element instanceof WhiteboardHttpContext) { // also matches WhiteboardServletContextHelper
                WhiteboardHttpContext whiteboardHttpContext = (WhiteboardHttpContext) element;
                Optional<Map.Entry<ServiceReference<ServletContext>, ServletContext>> matchingServletContextEntry =
                        servletContexts.entrySet().stream()
                                .filter(entry -> compareContextroot(entry.getValue(), whiteboardHttpContext.getHttpContextMapping().getContextPath()))
                                .findFirst();
                // TODO name missing
                if (!whiteboardHttpContext.isValid()) {
                    FailedServletContextDTO dto = new FailedServletContextDTO();
                    dto.failureReason = DTOConstants.FAILURE_REASON_VALIDATION_FAILED;
                    dto.serviceId = whiteboardHttpContext.getServiceID();
                    dto.contextPath = whiteboardHttpContext.getHttpContextMapping().getContextPath();
                    failedServletContextDTOs.add(dto);
                } else if (!matchingServletContextEntry.isPresent()) {
                    // element is valid, but no actual ServletContext exist
                    FailedServletContextDTO dto = new FailedServletContextDTO();
                    dto.failureReason = DTOConstants.FAILURE_REASON_EXCEPTION_ON_INIT;
                    dto.serviceId = whiteboardHttpContext.getServiceID();
                    dto.contextPath = whiteboardHttpContext.getHttpContextMapping().getContextPath();
                    failedServletContextDTOs.add(dto);
                } else {
                    // all fine
                    servletContextDTOs.add(this.mapServletContext(matchingServletContextEntry.get()));
                }
            } else if (element  instanceof WhiteboardServlet && ( ((WhiteboardServlet) element).getErrorPageMappings() == null || ((WhiteboardServlet) element).getErrorPageMappings().isEmpty()) ) {
                mapServlet((WhiteboardServlet)element, servletDTOs, failedServletDTOs);
            }  else if (element  instanceof WhiteboardServlet && ((WhiteboardServlet) element).getErrorPageMappings() != null && !((WhiteboardServlet) element).getErrorPageMappings().isEmpty()) {
                mapErrorPage((WhiteboardServlet)element, errorPageDTOs, failedErrorPageDTOs);
            } else if (element instanceof WhiteboardFilter) {
                mapFilter((WhiteboardFilter)element, filterDTOs, failedFilterDTOs);
            } else if (element instanceof WhiteboardListener) {
                mapListener((WhiteboardListener) element, listenerDTOs, failedListenerDTOs);
            } else if (element instanceof WhiteboardResource) {
                mapResource((WhiteboardResource) element, resourceDTOs, failedResourceDTOs);
            } else if (element instanceof WhiteboardWelcomeFile) {
                //TODO: welcome-files currently not working in whiteboard-ds example
            } else if (element instanceof WhiteboardErrorPage) {
                // ErrorPageMappings only tracks ErrorServlets as specified, all other ErrorPage mechnisms are ignored. Just fullfills R6
                if (element.isValid()) {
                    errorPageDTOs.add(mapServlet((WhiteboardErrorPage)element));
                } else {
                    failedErrorPageDTOs.add(transformToDTOFailed((WhiteboardErrorPage)element));
                }
            } else if (element instanceof WhiteboardJspMapping) {
                if (element.isValid()) {
                    //TODO: map jsps to servlets
                } else {
                    failedServletDTOs.add(transformToDTOFailed((WhiteboardJspMapping)element));
                }
            }
        });

        // check for root-context: if not available due to some whiteboard-service, the default/shared-context might be available
        if (!servletContextDTOs.stream().anyMatch(servletContextDTO -> "/".equals(servletContextDTO.contextPath))) {
            servletContextDTOs.addAll(servletContexts.entrySet().stream().filter(entry ->
                    WebContainerContext.DefaultContextIds.DEFAULT.getValue().equals(entry.getValue().getServletContextName())
                            || WebContainerContext.DefaultContextIds.SHARED.getValue().equals(entry.getValue().getServletContextName()))
                    .map(this::mapServletContext)
                    .collect(Collectors.toList()));

        }
        runtimeDto.servletContextDTOs = servletContextDTOs.toArray(new ServletContextDTO[servletContextDTOs.size()]);
        runtimeDto.failedServletContextDTOs = failedServletContextDTOs.toArray(new FailedServletContextDTO[servletContextDTOs.size()]);

        // Attach valid DTOs to matching ServletContextDTO
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
            servletContextDTO.resourceDTOs = resourceDTOs.stream()
                    .filter(resourceDTO -> resourceDTO.servletContextId == servletContextDTO.serviceId)
                    .toArray(ResourceDTO[]::new);
        });
        
        // Attach failed DTOs
        runtimeDto.failedServletContextDTOs = failedServletContextDTOs.stream().toArray(FailedServletContextDTO[]::new);
        runtimeDto.failedServletDTOs = failedServletDTOs.stream().toArray(FailedServletDTO[]::new);
        runtimeDto.failedFilterDTOs = failedFilterDTOs.stream().toArray(FailedFilterDTO[]::new);
        runtimeDto.failedErrorPageDTOs = failedErrorPageDTOs.stream().toArray(FailedErrorPageDTO[]::new);
        runtimeDto.failedListenerDTOs = failedListenerDTOs.stream().toArray(FailedListenerDTO[]::new);
        runtimeDto.failedResourceDTOs = failedResourceDTOs.stream().toArray(FailedResourceDTO[]::new);

        return runtimeDto;
    }


    RequestInfoDTO calculateRequestInfoDTO(String path, Iterator<WhiteboardElement> iterator, ServerModel serverModel) {
        RequestInfoDTO dto = new RequestInfoDTO();
        dto.path = path;
        OsgiContextModel contextModel = serverModel.matchPathToContext(path);
        if (contextModel != null) {

            Optional<Map.Entry<ServiceReference<ServletContext>, ServletContext>> matchingServletContextEntry =
                    findMatchingServletContext(contextModel.getHttpContext().getContextId());

            if (matchingServletContextEntry.isPresent()) {
                dto.servletContextId = (long) matchingServletContextEntry.get().getKey().getProperty(Constants.SERVICE_ID);
            }

            List<ServletDTO> servletDTOs = new ArrayList<>();
            List<FilterDTO> filterDTOs = new ArrayList<>();
            List<ResourceDTO> resourceDTOs = new ArrayList<>();

            iterator.forEachRemaining(element -> {
                if (element  instanceof WhiteboardServlet && ( ((WhiteboardServlet) element).getErrorPageMappings() == null || ((WhiteboardServlet) element).getErrorPageMappings().isEmpty())) {
                    if (isContextModelMatchedByContextId(contextModel, ((WhiteboardServlet) element).getServletMapping().getContextId())) {
                        mapServlet((WhiteboardServlet) element, servletDTOs, new ArrayList<>()); // dont care about failureDTOs, just reuse this
                    }
                }  else if (element instanceof WhiteboardFilter) {
                    if (isContextModelMatchedByContextId(contextModel, ((WhiteboardFilter) element).getFilterMapping().getHttpContextId())) {
                        mapFilter((WhiteboardFilter) element, filterDTOs, new ArrayList<>()); // dont care about failureDTOs, just reuse this
                    }
                } else if (element instanceof WhiteboardResource ) {
                    if (isContextModelMatchedByContextId(contextModel, ((WhiteboardResource) element).getResourceMapping().getHttpContextId())) {
                        mapResource((WhiteboardResource) element, resourceDTOs, new ArrayList<>()); // dont care about failureDTOs, just reuse this
                    }
                }
            });

            if (!servletDTOs.isEmpty()) {
                dto.servletDTO = servletDTOs.get(0);
            }
            if (!resourceDTOs.isEmpty()) {
                dto.resourceDTO = resourceDTOs.get(0);
            }
            dto.filterDTOs = filterDTOs.stream().toArray(FilterDTO[]::new);
        }
        return dto;
    }


    private boolean isContextModelMatchedByContextId(OsgiContextModel model, String contextId) {
        String modelId = model.getHttpContext().getContextId();
        // FIXME is check for DEFAULT sufficeient or should SHARED be included as well
        return Objects.equals(contextId, modelId)
                || contextId == null && Objects.equals(modelId, WebContainerContext.DefaultContextIds.DEFAULT.getValue());
    }


    /*
     * Maps a default context (whithout whiteboard-service) to a ServletContextDTO
     */
    private ServletContextDTO mapServletContext(Map.Entry<ServiceReference<ServletContext>, ServletContext> mapEntry) {
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


    private void mapServlet(WhiteboardServlet whiteBoardServlet, List<ServletDTO> servletDTOs, List<FailedServletDTO> failedServletDTOs) {
        ServletDTO dto = new ServletDTO();
        ServletMapping servletMapping = whiteBoardServlet.getServletMapping();
        dto.serviceId = whiteBoardServlet.getServiceID();
        dto.name = servletMapping.getServletName();
        dto.initParams = servletMapping.getInitParams();
        dto.patterns = servletMapping.getUrlPatterns();
        dto.servletInfo = servletMapping.getServlet().getServletInfo();
        dto.asyncSupported = servletMapping.getAsyncSupported() != null ? servletMapping.getAsyncSupported() : false;

        Optional<Map.Entry<ServiceReference<ServletContext>, ServletContext>> matchingServletContextEntry = findMatchingServletContext(
                servletMapping.getContextId());

        if (matchingServletContextEntry.isPresent()) {
            dto.servletContextId = (long) matchingServletContextEntry.get().getKey().getProperty(Constants.SERVICE_ID);
        }

        if (!whiteBoardServlet.isValid()) {
            failedServletDTOs.add(
                    transformToFailedDTO(
                            dto,
                            FailedServletDTO.class,
                            DTOConstants.FAILURE_REASON_VALIDATION_FAILED));
        } else if (!matchingServletContextEntry.isPresent()) {
            failedServletDTOs.add(
                    transformToFailedDTO(
                            dto,
                            FailedServletDTO.class,
                            DTOConstants.FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING));
        } else {
            servletDTOs.add(dto);
        }
    }


    private void mapErrorPage(WhiteboardServlet whiteboardErrorPage, List<ErrorPageDTO> errorPageDTOs, List<FailedErrorPageDTO> failedErrorPageDTOs) {
        ErrorPageDTO dto = new ErrorPageDTO();

        ServletMapping servletMapping = whiteboardErrorPage.getServletMapping();

        Optional<Map.Entry<ServiceReference<ServletContext>, ServletContext>> matchingServletContextEntry = findMatchingServletContext(
                servletMapping.getContextId());

        if (matchingServletContextEntry.isPresent()) {
            dto.servletContextId = (long) matchingServletContextEntry.get().getKey().getProperty(Constants.SERVICE_ID);
        }
        dto.serviceId = whiteboardErrorPage.getServiceID();
        dto.name = servletMapping.getServletName();
        dto.initParams = servletMapping.getInitParams();
        dto.exceptions = whiteboardErrorPage.getErrorPageMappings().stream()
                .map(ErrorPageMapping::getError)
                .toArray(String[]::new);
        // FIXME where to get the errorCodes-mapping from
//        dto.errorCodes = ???

        // FIXME: not complete

        if (matchingServletContextEntry.isPresent()) {
            dto.servletContextId = (long) matchingServletContextEntry.get().getKey().getProperty(Constants.SERVICE_ID);
        }

        if (!whiteboardErrorPage.isValid()) {
            failedErrorPageDTOs.add(
                    transformToFailedDTO(
                            dto,
                            FailedErrorPageDTO.class,
                            DTOConstants.FAILURE_REASON_VALIDATION_FAILED));
        } else if (!matchingServletContextEntry.isPresent()) {
            failedErrorPageDTOs.add(
                    transformToFailedDTO(
                            dto,
                            FailedErrorPageDTO.class,
                            DTOConstants.FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING));
        } else {
            errorPageDTOs.add(dto);
        }
    }


    private void mapFilter(WhiteboardFilter whiteboardFilter, List<FilterDTO> filterDTOs, List<FailedFilterDTO> failedFilterDTOs) {
        FilterDTO dto = new FilterDTO();
        dto.name = whiteboardFilter.getFilterMapping().getName();
        dto.asyncSupported = whiteboardFilter.getFilterMapping().getAsyncSupported(); 
        dto.initParams = whiteboardFilter.getFilterMapping().getInitParams();
        dto.patterns = whiteboardFilter.getFilterMapping().getUrlPatterns();
        dto.servletNames = whiteboardFilter.getFilterMapping().getServletNames();
        dto.serviceId = whiteboardFilter.getServiceID();
        dto.dispatcher = Arrays.stream(whiteboardFilter.getFilterMapping().getDispatcherType())
                .map(Enum::toString)
                .toArray(String[]::new);
        
        Optional<Map.Entry<ServiceReference<ServletContext>, ServletContext>> matchingServletContextEntry = findMatchingServletContext(
                whiteboardFilter.getFilterMapping().getHttpContextId());

        if (matchingServletContextEntry.isPresent()) {
            dto.servletContextId = (long) matchingServletContextEntry.get().getKey().getProperty(Constants.SERVICE_ID);
        }

        if (!whiteboardFilter.isValid()) {
            failedFilterDTOs.add(
                    transformToFailedDTO(
                            dto,
                            FailedFilterDTO.class,
                            DTOConstants.FAILURE_REASON_VALIDATION_FAILED));
        } else if (!matchingServletContextEntry.isPresent()) {
            failedFilterDTOs.add(
                    transformToFailedDTO(
                            dto,
                            FailedFilterDTO.class,
                            DTOConstants.FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING));
        } else {
            filterDTOs.add(dto);
        }
    }


    private void mapListener(WhiteboardListener whiteboardLister, List<ListenerDTO> listenerDTOs, List<FailedListenerDTO> failedListenerDTOs) {
        ListenerDTO dto = new ListenerDTO();
        dto.serviceId = whiteboardLister.getServiceID();
        dto.types = Arrays.stream(whiteboardLister.getListenerMapping().getListener().getClass().getInterfaces())
                .filter(EventListener.class::isAssignableFrom)
                .map(Class::getName)
                .toArray(String[]::new);

        Optional<Map.Entry<ServiceReference<ServletContext>, ServletContext>> matchingServletContextEntry = findMatchingServletContext(
                whiteboardLister.getListenerMapping().getHttpContextId());

        if (matchingServletContextEntry.isPresent()) {
            dto.servletContextId = (long) matchingServletContextEntry.get().getKey().getProperty(Constants.SERVICE_ID);
        }

        if (!whiteboardLister.isValid()) {
            failedListenerDTOs.add(
                    transformToFailedDTO(
                            dto,
                            FailedListenerDTO.class,
                            DTOConstants.FAILURE_REASON_VALIDATION_FAILED));
        } else if (!matchingServletContextEntry.isPresent()) {
            failedListenerDTOs.add(
                    transformToFailedDTO(
                            dto,
                            FailedListenerDTO.class,
                            DTOConstants.FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING));
        } else {
            listenerDTOs.add(dto);
        }
    }


    private void mapResource(WhiteboardResource whiteboardResource, List<ResourceDTO> resourceDTOs, List<FailedResourceDTO> failedResourceDTOs) {
        ResourceDTO dto = new ResourceDTO();
        dto.serviceId = whiteboardResource.getServiceID();
        dto.prefix = whiteboardResource.getResourceMapping().getPath();
        dto.patterns = new String [] {whiteboardResource.getResourceMapping().getAlias()};

        Optional<Map.Entry<ServiceReference<ServletContext>, ServletContext>> matchingServletContextEntry = findMatchingServletContext(
                whiteboardResource.getResourceMapping().getHttpContextId());

        if (matchingServletContextEntry.isPresent()) {
            dto.servletContextId = (long) matchingServletContextEntry.get().getKey().getProperty(Constants.SERVICE_ID);
        }

        if (!whiteboardResource.isValid()) {
            failedResourceDTOs.add(
                    transformToFailedDTO(
                            dto,
                            FailedResourceDTO.class,
                            DTOConstants.FAILURE_REASON_VALIDATION_FAILED));
        } else if (!matchingServletContextEntry.isPresent()) {
            failedResourceDTOs.add(
                    transformToFailedDTO(
                            dto,
                            FailedResourceDTO.class,
                            DTOConstants.FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING));
        } else {
            resourceDTOs.add(dto);
        }
    }
    
    private ErrorPageDTO mapServlet(WhiteboardErrorPage whiteboardErrorPage) {
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
            /*
             FIXME currently no way to find Default/SharedContext because they are not available in the ServiceModel
             (could also be the shared context)
             */
            name = WebContainerContext.DefaultContextIds.DEFAULT.getValue();
        } else {
            name = httpContextId;
        }
        return this.servletContexts.entrySet().stream()
                .filter(entry -> Objects.equals(entry.getValue().getServletContextName(), name)).findFirst();
    }


    /**
     * A Failure-DTO is the same as the standard-DTO, just with a failure-reason. This method just maps
     * to a failure-DTO.
     * @param dto the DTO to convert
     * @param type the Type of the failure-DTO (necessary for correct Generic)
     * @param failureReason a constant from {@link DTOConstants}
     * @param <T> the type of the given DTO
     * @param <R> the return-type of this method, corresponds to type-param
     *
     * @return converted failure-DTO
     */
    private <T extends DTO, R extends DTO> R transformToFailedDTO(T dto, Class<R> type, int failureReason) {
        R result;
        if (dto instanceof ServletContextDTO) {
            ServletContextDTO servletContextDTO = ((ServletContextDTO) dto);
            FailedServletContextDTO failedServletContextDTO = new FailedServletContextDTO();
            failedServletContextDTO.failureReason = failureReason;
            failedServletContextDTO.serviceId = servletContextDTO.serviceId;
            failedServletContextDTO.name = servletContextDTO.name;
            failedServletContextDTO.initParams = servletContextDTO.initParams;
            failedServletContextDTO.contextPath = servletContextDTO.contextPath;
            failedServletContextDTO.attributes = servletContextDTO.attributes;
            failedServletContextDTO.errorPageDTOs = servletContextDTO.errorPageDTOs;
            failedServletContextDTO.servletDTOs = servletContextDTO.servletDTOs;
            failedServletContextDTO.listenerDTOs = servletContextDTO.listenerDTOs;
            failedServletContextDTO.filterDTOs = servletContextDTO.filterDTOs;
            failedServletContextDTO.resourceDTOs = servletContextDTO.resourceDTOs;
            result = type.cast(failedServletContextDTO);
        } else if (dto instanceof ServletDTO) {
            ServletDTO servletDTO = ((ServletDTO) dto);
            FailedServletDTO failedServletDTO = new FailedServletDTO();
            failedServletDTO.failureReason = failureReason;
            failedServletDTO.serviceId = servletDTO.serviceId;
            failedServletDTO.servletContextId = servletDTO.servletContextId;
            failedServletDTO.asyncSupported = servletDTO.asyncSupported;
            failedServletDTO.patterns = servletDTO.patterns;
            failedServletDTO.name = servletDTO.name;
            failedServletDTO.initParams = servletDTO.initParams;
            failedServletDTO.servletInfo = servletDTO.servletInfo;
            result = type.cast(failedServletDTO);
        } else if (dto instanceof ErrorPageDTO) {
            ErrorPageDTO errorPageDTO = ((ErrorPageDTO) dto);
            FailedErrorPageDTO failedErrorPageDTO = new FailedErrorPageDTO();
            failedErrorPageDTO.failureReason = failureReason;
            failedErrorPageDTO.serviceId = errorPageDTO.serviceId;
            failedErrorPageDTO.servletContextId = errorPageDTO.servletContextId;
            failedErrorPageDTO.errorCodes = errorPageDTO.errorCodes;
            failedErrorPageDTO.exceptions = errorPageDTO.exceptions;
            failedErrorPageDTO.asyncSupported = errorPageDTO.asyncSupported;
            failedErrorPageDTO.name = errorPageDTO.name;
            failedErrorPageDTO.initParams = errorPageDTO.initParams;
            failedErrorPageDTO.servletInfo = errorPageDTO.servletInfo;
            result = type.cast(failedErrorPageDTO);
        } else if (dto instanceof ListenerDTO) {
            ListenerDTO listenerDTO = ((ListenerDTO) dto);
            FailedListenerDTO failedListenerDTO = new FailedListenerDTO();
            failedListenerDTO.failureReason = failureReason;
            failedListenerDTO.serviceId = listenerDTO.serviceId;
            failedListenerDTO.servletContextId = listenerDTO.servletContextId;
            failedListenerDTO.types = listenerDTO.types;
            result = type.cast(failedListenerDTO);
        } else if (dto instanceof FilterDTO) {
            FilterDTO filterDTO = ((FilterDTO) dto);
            FailedFilterDTO failedFilterDTO = new FailedFilterDTO();
            failedFilterDTO.failureReason = failureReason;
            failedFilterDTO.serviceId = filterDTO.serviceId;
            failedFilterDTO.servletContextId = filterDTO.servletContextId;
            failedFilterDTO.asyncSupported = filterDTO.asyncSupported;
            failedFilterDTO.patterns = filterDTO.patterns;
            failedFilterDTO.name = filterDTO.name;
            failedFilterDTO.dispatcher = filterDTO.dispatcher;
            failedFilterDTO.initParams = filterDTO.initParams;
            failedFilterDTO.servletNames = filterDTO.servletNames;
            failedFilterDTO.regexs = filterDTO.regexs;
            result = type.cast(failedFilterDTO);
        } else if (dto instanceof ResourceDTO) {
            ResourceDTO resourceDTO = ((ResourceDTO) dto);
            FailedResourceDTO failedResourceDTO = new FailedResourceDTO();
            failedResourceDTO.failureReason = failureReason;
            failedResourceDTO.serviceId = resourceDTO.serviceId;
            failedResourceDTO.servletContextId = resourceDTO.servletContextId;
            failedResourceDTO.patterns = resourceDTO.patterns;
            failedResourceDTO.prefix = resourceDTO.prefix;
            result = type.cast(failedResourceDTO);
        } else {
            throw new IllegalArgumentException(dto.getClass().getName() + " not handled!");
        }
        return result;
    }


    @Reference(unbind = "removeServletContext", service = ServletContext.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addServletContext(ServiceReference<ServletContext> ref, ServletContext servletContext) {
        servletContexts.put(ref, servletContext);
    }

    protected void removeServletContext(ServiceReference<ServletContext> ref, ServletContext servletContext) {
        servletContexts.remove(ref);
    }
}
