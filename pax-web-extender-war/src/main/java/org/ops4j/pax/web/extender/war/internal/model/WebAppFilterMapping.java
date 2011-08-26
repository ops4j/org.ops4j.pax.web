/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.war.internal.model;

import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.ops4j.lang.NullArgumentException;

/**
 * Models filter mapping element in web.xml.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, December 27, 2007
 */
public class WebAppFilterMapping
{

    /**
     * Filter name.
     */
    private String m_filterName;
    /**
     * Mapped url pattern.
     */
    private String m_urlPattern;
    /**
     * Mapped servlet name.
     */
    private String m_servletName;
    
    
	private EnumSet<DispatcherType> dispatcherSet;

    /**
     * Getter.
     *
     * @return filter name
     */
    public String getFilterName()
    {
        return m_filterName;
    }

    /**
     * Setter.
     *
     * @param filterName value to set. Cannot be null.
     *
     * @throws NullArgumentException if filter name is null
     */
    public void setFilterName( final String filterName )
    {
        NullArgumentException.validateNotNull( filterName, "Filter name" );
        m_filterName = filterName;
    }

    /**
     * Getter.
     *
     * @return url pattern
     */
    public String getUrlPattern()
    {
        return m_urlPattern;
    }

    /**
     * Setter.
     *
     * @param urlPattern value to set
     */
    public void setUrlPattern( final String urlPattern )
    {
        m_urlPattern = urlPattern;
    }

    /**
     * Getter.
     *
     * @return servlet name
     */
    public String getServletName()
    {
        return m_servletName;
    }

    /**
     * Setter.
     *
     * @param servletName value to set
     */
    public void setServletName( final String servletName )
    {
        m_servletName = servletName;
    }

    public void setDispatcherTypes(EnumSet<DispatcherType> dispatcherSet) {
		this.dispatcherSet = dispatcherSet;
	}
    
    public EnumSet<DispatcherType> getDispatcherTypes() {
    	return dispatcherSet;
    }

    @Override
    public String toString()
    {
        return new StringBuffer()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "filterName=" ).append( m_filterName )
            .append( ",urlPattern=" ).append( m_urlPattern )
            .append( ",servletName=" ).append( m_servletName )
            .append( "}" )
            .toString();
    }

}