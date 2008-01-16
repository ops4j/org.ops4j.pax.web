/* Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service;

/**
 * Web Container related constants.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, December 27, 2007
 */
public class WebContainerConstants
{

    /**
     * Init param name for specifying a context name.
     */
    public static final String CONTEXT_NAME = "webapp.context";

    /**
     * Servlet init param name for specifying a servlet name.
     */
    public static final String SERVLET_NAME = "servlet-name";

    /**
     * Filter init param name for specifying a filter name.
     */
    public static final String FILTER_NAME = "filter-name";

}
