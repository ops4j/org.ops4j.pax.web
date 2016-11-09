/* Copyright 2016 Marc Schlegel
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;

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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.runtime.dto.FailedServletContextDTO;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;

@Component(service = WhiteboardDtoService.class)
public class WhiteboardDtoService {

    @Reference(bind = "addServletContext", unbind = "removeServletContext")
    private List<ServletContext> servletContexts;

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
        List<FilterDTO> filterDTOs = new ArrayList<>(); //TODO ... 
        
        iterator.forEachRemaining(element -> {
            if (element  instanceof WhiteboardServlet) {
                if (element.isValid())
                    servletContextDTOs.add(transformToDTO((WhiteboardServlet)element));
                else
                    failedServletContextDTOs.add(transformToDTOFailed((WhiteboardServlet)element));
            } else if (element instanceof WhiteboardFilter) {
                //TODO: add filter
            } else if (element instanceof WhiteboardErrorPage) {
                //TODO: add error pages
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

        return runtimeDto;
    }

    public RequestInfoDTO calculateRequestInfoDTO(Iterator<WhiteboardElement> iterator) {
        // FIXME TBD
        return null;
    }


    private ServletContextDTO transformToDTO(ServletContext servletContext) {
        ServletContextDTO dto = new ServletContextDTO();
        dto.contextPath = servletContext.getContextPath();
        dto.name = servletContext.getServletContextName();

        dto.attributes = Collections.list(servletContext.getAttributeNames())
                .stream()
                .map(name -> new SimpleEntry(name, servletContext.getAttribute(name)))
                .collect(
                        Collectors.toMap(
                                entry -> entry.getKey().toString(),
                                entry -> entry.getValue()
                        )
                );
        dto.initParams = Collections.list(servletContext.getInitParameterNames())
                .stream()
                .map(name -> new SimpleEntry( name, servletContext.getInitParameter(name) ) )
                .collect(
                        Collectors.toMap(
                                entry -> entry.getKey().toString(),
                                entry -> entry.getValue().toString()
                        )
                );

        return dto;
    }


    private ServletContextDTO transformToDTO(WhiteboardServlet whiteBoardServlet) {
        ServletContextDTO dto = new ServletContextDTO();

        ServletMapping servletMapping = whiteBoardServlet.getServletMapping();

        dto.contextPath = servletMapping.getHttpContextId();
        dto.name = servletMapping.getServletName();
        dto.initParams = servletMapping.getInitParams();

        //FIXME: not complete
        return dto;
    }
    
    private FailedServletContextDTO transformToDTOFailed(WhiteboardServlet whiteBoardServlet) {
        FailedServletContextDTO dto = new FailedServletContextDTO();
        
        dto.contextPath = whiteBoardServlet.getServletMapping().getHttpContextId();
        
        return dto;
    }


    public void addServletContext(ServletContext servletContext) {
        servletContexts.add(servletContext);
    }

    public void removeServletContext(ServletContext servletContext) {
        servletContexts.remove(servletContext);
    }
}
