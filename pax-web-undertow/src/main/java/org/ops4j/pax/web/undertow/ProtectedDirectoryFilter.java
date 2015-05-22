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
package org.ops4j.pax.web.undertow;

import io.undertow.servlet.spec.HttpServletRequestImpl;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class ProtectedDirectoryFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // not used
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequestImpl httpRequest = (HttpServletRequestImpl) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getExchange().getRelativePath();
        if (path != null) {
            if (path.equals("OSGI-INF") || path.startsWith("/OSGI-INF/") || 
                path.equals("OSGI-OPT") || path.startsWith("/OSGI-OPT/")) {
                httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }
        chain.doFilter(httpRequest, httpResponse);
    }

    @Override
    public void destroy() {
        // not used
    }
}
