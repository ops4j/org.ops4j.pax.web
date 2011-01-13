/*
 * Copyright 2009 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.war.internal;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.ops4j.pax.web.extender.war.internal.model.WebAppMimeMapping;
import org.ops4j.pax.web.extender.war.internal.util.Path;
import org.ops4j.pax.web.service.WebContainerContext;

/**
 * Extends {@link WebAppHttpContext} by implementing {@link WebContainerContext}.
 *
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @since 0.5.1, March 30, 2009
 */
class WebAppWebContainerContext
    extends WebAppHttpContext
    implements WebContainerContext
{

    /**
     * Constructor matching super.
     * {@inheritDoc}
     */
    WebAppWebContainerContext( final HttpContext httpContext,
    						   final String rootPath,
                               final Bundle bundle,
                               final WebAppMimeMapping[] mimeMappings )
    {
        super( httpContext, rootPath, bundle, mimeMappings );
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getResourcePaths( final String name )
    {
        final String rawNormalizedName = Path.normalizeResourcePath( name );
        final String normalizedName = m_rootPath + (rawNormalizedName.startsWith("/") ? "" : "/") + rawNormalizedName;
        
        LOG.debug(
            "Searching bundle [" + m_bundle
            + "] for resource paths of [" + name + "], normalized to [" + normalizedName + "]"
        );
        final Enumeration entryPaths = m_bundle.getEntryPaths( name );
        if( entryPaths == null || !entryPaths.hasMoreElements() )
        {
            LOG.debug( "No resource paths found" );
            return null;
        }
        Set<String> foundPaths = new HashSet<String>();
        while( entryPaths.hasMoreElements() )
        {
            foundPaths.add( (String) entryPaths.nextElement() );
        }
        LOG.debug( "Resource paths found: " + foundPaths );
        return foundPaths;
    }
    
}
