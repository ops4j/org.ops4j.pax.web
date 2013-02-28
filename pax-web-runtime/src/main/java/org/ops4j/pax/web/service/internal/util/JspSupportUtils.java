/*
 * Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.internal.util;

/**
 * Utilities related to Jsp support.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 07, 2007
 */
public class JspSupportUtils
{

    /**
     * Utility class. Should be used only via static methods.
     */
    private JspSupportUtils()
    {
        // utility class
    }

    /**
     * Verify if jsp support is available.
     *
     * @return true if WebContainer is available
     * @deprecated use {@link SupportUtils#isJSPAvailable()}
     */
    @Deprecated
    public static boolean jspSupportAvailable()
    {
       return SupportUtils.isJSPAvailable();

    }

}
