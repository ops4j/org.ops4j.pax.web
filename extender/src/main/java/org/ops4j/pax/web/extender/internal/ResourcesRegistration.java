/*
 * Copyright 2007 Damian Golda.
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.internal;

import org.osgi.framework.Bundle;

/**
 * Holds alias and bundle for tracked Resources.
 *
 * @author Damian Golda
 * @author Alin Dreghiciu
 * @since August 22, 2007
 */
public class ResourcesRegistration
    extends Registration
{

    /**
     * The bundle that published resources.
     */
    private final Bundle bundle;

    /**
     * Creates a new Resources registration.
     *
     * @param alias  an alias; mandatory
     * @param bundle the bundle that published the resources; mandatory
     */
    public ResourcesRegistration( final String alias, final Bundle bundle )
    {
        super( alias );
        if ( bundle == null )
        {
            throw new IllegalArgumentException( "Bundle cannot be null" );
        }
        this.bundle = bundle;
    }

    /**
     * Returns the bundle that published the resources.
     *
     * @return a bundle
     */
    public Bundle getBundle()
    {
        return bundle;
    }

}
