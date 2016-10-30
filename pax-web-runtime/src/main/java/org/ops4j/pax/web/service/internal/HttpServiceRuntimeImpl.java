package org.ops4j.pax.web.service.internal;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;

import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.whiteboard.WhiteboardElement;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;

@Component
public class HttpServiceRuntimeImpl implements HttpServiceRuntime {

    @Reference(bind = "addServletContext", unbind = "removeServletContext")
    private List<ServletContext> servletContexts;
    
    @Reference(bind = "addWebContainer", unbind = "removeWebContainer")
    private WebContainer webContainer;

    private BundleContext bundleContext;
    
    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
    
    @Deactivate
    public void destroy() {
        this.bundleContext = null;
    }
    
    @Override
    public RuntimeDTO getRuntimeDTO() {
        RuntimeDTO runtimeDto = new RuntimeDTO();
        ServletContextDTO[] servletContextDTOs = null;        
        
        List<ServletContextDTO> collect = (List<ServletContextDTO>) servletContexts.stream().map(this::transformToDTO).collect(Collectors.toList());
        servletContextDTOs = collect.toArray(new ServletContextDTO[collect.size()]);        
        runtimeDto.servletContextDTOs = servletContextDTOs;

        return runtimeDto;
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

    @Override
    public RequestInfoDTO calculateRequestInfoDTO(String path) {
        Iterator<WhiteboardElement> iterator = null;
        return webContainer.calculateRequestInfoDTO(iterator);
    }
    
    public void addServletContext(ServletContext servletContext) {
        servletContexts.add(servletContext);
    }
    
    public void removeServletContext(ServletContext servletContext) {
        servletContexts.remove(servletContext);
    }
    
    public void addWebContainer(WebContainer webContainer) {
        this.webContainer = webContainer; 
    }
    
    public void removeWebContainer() {
        this.webContainer = null;
    }

}