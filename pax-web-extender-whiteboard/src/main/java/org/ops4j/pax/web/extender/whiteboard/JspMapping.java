/*
 * Copyright 2008 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.whiteboard;

import java.util.Map;

/**
 * Jsp mapping.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, March 15, 2008
 */
public interface JspMapping
{

    /**
     * Getter.
     *
     * @return id of the http context this jsp belongs to
     */
    String getHttpContextId();

    /**
     * Getter.
     *
     * @return an array of url patterns this jsp support maps to. If null, a default "*.jsp" will be used.
     */
    String[] getUrlPatterns();
    
    /**
     * Getter.
     *
     * @return map of initialization parameters.
     */
    Map<String, String> getInitParams();

}