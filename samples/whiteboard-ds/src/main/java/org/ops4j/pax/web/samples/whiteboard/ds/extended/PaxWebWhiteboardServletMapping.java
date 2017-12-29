/*
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
package org.ops4j.pax.web.samples.whiteboard.ds.extended;

import java.io.IOException;
import java.util.Map;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ops4j.pax.web.service.whiteboard.ServletMapping;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.http.HttpContext;

@Component
public class PaxWebWhiteboardServletMapping implements ServletMapping {

    private static Servlet servlet = new HttpServlet() {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.getWriter().println("Hello from " + PaxWebWhiteboardServletMapping.class.getName());
        }
    };

    @Reference( target = "(httpContext.id=CustomHttpContext)", 
                bind = "bindHttpContext", 
                unbind = "unbindHttpContext", cardinality = ReferenceCardinality.MANDATORY)
    private HttpContext context;
    
    @Override
    public String getHttpContextId() {
        return PaxWebWhiteboardHttpContext.CONTEXT_ID;
    }

    @Override
    public Servlet getServlet() {
        return servlet;
    }

    @Override
    public String getServletName() {
        return PaxWebWhiteboardServletMapping.class.getName();
    }

    @Override
    public String getAlias() {
        return "/servlet-mapping";
    }

    @Override
    public String[] getUrlPatterns() {
        return new String[0];
    }

    @Override
    public Map<String, String> getInitParams() {
        return null;
    }

    @Override
    public Integer getLoadOnStartup() {
        return 1;
    }

    @Override
    public Boolean getAsyncSupported() {
        return false;
    }

    @Override
    public MultipartConfigElement getMultipartConfig() {
        return null;
    }

    public void bindHttpContext(HttpContext context) {
        this.context = context;
    }
    
    public void unbindHttpContext(HttpContext context) {
        this.context = null;
    }

}
