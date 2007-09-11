/*
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

/**
 * Holder for specific data per published holder.
 * Classes that need more data should extend this class and add specific holders.
 *
 * @author Alin Dreghiciu
 * @since August 23, 2007
 */
public class Registration
{

    /**
     * Registration alias.
     */
    private final String m_alias;

    /**
     * Creates a new registration.
     *
     * @param alias published alias; mandatory
     */
    public Registration( final String alias )
    {
        if( alias == null )
        {
            throw new IllegalArgumentException( "Alias cannot be null" );
        }
        m_alias = alias;
    }

    /**
     * Returns the alias.
     *
     * @return alias
     */
    public String getAlias()
    {
        return m_alias;
    }

}
