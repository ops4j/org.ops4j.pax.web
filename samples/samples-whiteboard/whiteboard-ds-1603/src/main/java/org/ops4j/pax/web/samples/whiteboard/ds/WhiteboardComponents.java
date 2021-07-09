/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.samples.whiteboard.ds;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.context.ServletContextHelper;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Optional;

public class WhiteboardComponents {

    @Component(service = { Filter.class }, scope = ServiceScope.PROTOTYPE,
            property = {
                    "osgi.http.whiteboard.filter.name=LiftFilter",
                    "osgi.http.whiteboard.filter.servlet=LiftServlet",
                    "osgi.http.whiteboard.filter.asyncSupported=true",
                    "osgi.http.whiteboard.context.select=(osgi.http.whiteboard.context.name=liftweb)"
            })
    public static class LiftFilterWrapper implements Filter {
        public void init(FilterConfig filterConfig) throws ServletException {
        }

        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            System.out.println("LiftFilter: doFilter req=" + request);
            chain.doFilter(request, response);
        }

        public void destroy() {
        }
    }

    @Component(service = { Servlet.class }, scope = ServiceScope.PROTOTYPE,
            property = {
                    "osgi.http.whiteboard.servlet.name=LiftServlet",
                    "osgi.http.whiteboard.servlet.pattern=/",
                    "osgi.http.whiteboard.servlet.asyncSupported=true",
                    "osgi.http.whiteboard.context.select=(osgi.http.whiteboard.context.name=liftweb)"
            })
    public static class LiftServletComponent extends HttpServlet {
        private static final long serialVersionUID = 5054038529008636284L;

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            // this is a request for a static resource, other request are already handled by
            // the lift filter
            System.out.println("LiftServletComponent::doGet() uri=" + request.getRequestURI());
            final String pathInfo = request.getRequestURI();
            HttpServletRequestWrapper wrapped = new HttpServletRequestWrapper(request) {
                // re-use path info since it is otherwise lost due to the forwarding
                @Override
                public String getPathInfo() {
                    return pathInfo;
                }
            };
            // forward this to the internal whiteboard resources servlet as defined by
            // LiftResources
            request.getRequestDispatcher("/lift-resources" + pathInfo).forward(wrapped, response);
        }
    }

    @Component(service = { LiftResources.class }, property = {
            "osgi.http.whiteboard.resource.pattern=/lift-resources/*",
            "osgi.http.whiteboard.resource.prefix=/",
            "osgi.http.whiteboard.context.select=(osgi.http.whiteboard.context.name=liftweb)"
    })
    public static class LiftResources {
    }

    public interface LiftService {
    }

    /**
     * Registers the Lift service that helps clients to detect if the Lift framework
     * is already running or not
     */
    @Component(reference = {
            // add reference to HttpService because it is accessed in activate()
            @Reference(name = "HttpService", service = HttpService.class),
            // the LiftService should only be available if LiftFilter is already registered
            @Reference(name = "LiftFilter", service = Filter.class, target = "(osgi.http.whiteboard.filter.name=LiftFilter)")
    })
    class LiftServiceComponent implements LiftService {
        @Activate
        public void activate(ComponentContext ctx) {
            ServiceReference<HttpService> httpServiceRef = ctx.getBundleContext()
                    .getServiceReference(HttpService.class);
            Optional<String> portKey = Arrays.asList(httpServiceRef.getPropertyKeys()).stream()
                    .filter(k -> k.endsWith("http.port")).findFirst();
            Optional<Integer> port = portKey.map(k -> Integer.valueOf(httpServiceRef.getProperty(k).toString()));
            port.ifPresent(p -> System.out.println("LiftServiceComponent::activate() port=" + p));
        }
    }

    @Component(service = { ServletContextHelper.class }, scope = ServiceScope.BUNDLE, property = {
            "osgi.http.whiteboard.context.name=liftweb",
            "osgi.http.whiteboard.context.path=/",
            "service.ranking:Integer=1"
    })
    public static class LiftHttpContext extends ServletContextHelper {
        Bundle bundle;

        @Activate
        public void activate(ComponentContext ctx) {
            bundle = ctx.getBundleContext().getBundle();
        }

        @Override
        public String getMimeType(String r) {
            // whiteboard implementation determines the mime type itself
            return null;
        }

        @Override
        public URL getResource(String r) {
            System.out.println("LiftHttpContext::getResource(" + r + ")");
            if (null != r && null != bundle) {
                return bundle.getResource(r);
            }
            return super.getResource(r);
        }
    }

}
