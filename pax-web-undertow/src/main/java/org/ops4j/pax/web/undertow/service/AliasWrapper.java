/*
 * Copyright 2014 Harald Wellmann.
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
package org.ops4j.pax.web.undertow.service;

import io.undertow.servlet.api.ServletInfo;

import org.osgi.service.http.HttpContext;


public class AliasWrapper {

    private String alias;
    private HttpContext httpContext;
    private ServletInfo servletInfo;
    
    
    public AliasWrapper(String alias) {
        this.alias = alias;
    }
    
    public String getAlias() {
        return alias;
    }
    
    public HttpContext getHttpContext() {
        return httpContext;
    }
    
    public void setHttpContext(HttpContext httpContext) {
        this.httpContext = httpContext;
    }

    
    public ServletInfo getServletInfo() {
        return servletInfo;
    }

    
    public void setServletInfo(ServletInfo servletInfo) {
        this.servletInfo = servletInfo;
    }
    
    
}
