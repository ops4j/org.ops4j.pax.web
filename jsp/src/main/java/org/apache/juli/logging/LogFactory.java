/* Copyright 2008 Alin Dreghiciu.
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
package org.apache.juli.logging;

import org.apache.commons.logging.LogConfigurationException;

/**
 * Apache Tomcat juli -> jcl adapter.
 * The reason this classes are here is to redirect juli logging to apache commons logging.
 *
 * !!! This class is not a workable log factory. It just defines what jasper needs.
 *
 * @see org.apache.commons.logging.LogFactory
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 07, 2008
 */
public abstract class LogFactory
    extends org.apache.commons.logging.LogFactory
{

    public static Log getLog( final Class clazz )
        throws org.apache.commons.logging.LogConfigurationException
    {
        return new JclLogAdapter( org.apache.commons.logging.LogFactory.getLog( clazz ) );
    }

}
