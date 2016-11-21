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
import java.net.URL;
import java.util.Map;

import org.ops4j.pax.web.service.whiteboard.HttpContextMapping;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.HttpContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class PaxWebWhiteboardHttpContextMapping implements HttpContextMapping {

    static final String HTTP_CONTEXT_ID = "CustomHttpContextMapping";

    @Override
    public String getHttpContextId() {
        return HTTP_CONTEXT_ID;
    }

    @Override
    public String getPath() {
        return "/custom-http-context-mapping";
    }

    @Override
    public Map<String, String> getParameters() {
        return null;
    }

    @Override
    public HttpContext getHttpContext() {
//        return new HttpContext() {
//            @Override
//            public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
//                return true;
//            }
//
//            @Override
//            public URL getResource(String name) {
//                return null;
//            }
//
//            @Override
//            public String getMimeType(String name) {
//                return null;
//            }
//        };
        return null;
    }
}
