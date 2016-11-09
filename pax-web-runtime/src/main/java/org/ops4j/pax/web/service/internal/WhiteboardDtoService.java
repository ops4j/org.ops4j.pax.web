package org.ops4j.pax.web.service.internal;

import org.ops4j.pax.web.service.whiteboard.*;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;

import javax.servlet.ServletContext;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

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
        List<FilterDTO> filterDTOs = new ArrayList<>(); //TODO ...

        iterator.forEachRemaining(element -> {
            if (element  instanceof WhiteboardServlet) {
                servletContextDTOs.add(transformToDTO((WhiteboardServlet)element));
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


    protected void addServletContext(ServletContext servletContext) {
        servletContexts.add(servletContext);
    }

    protected void removeServletContext(ServletContext servletContext) {
        servletContexts.remove(servletContext);
    }
}
