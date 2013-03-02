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
package org.ops4j.pax.web.extender.whiteboard.runtime;

import org.ops4j.pax.web.extender.whiteboard.ResourceMapping;

/**
 * Default implementation of {@link ResourceMapping}.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class DefaultResourceMapping
    implements ResourceMapping
{

    /**
     * Http Context id.
     */
    private String m_httpContextId;
    /**
     * Alias.
     */
    private String m_alias;
    /**
     * Url patterns.
     */
    private String m_path;

    /**
     * @see ResourceMapping#getHttpContextId()
     */
    public String getHttpContextId()
    {
        return m_httpContextId;
    }

    /**
     * @see ResourceMapping#getAlias()
     */
    public String getAlias()
    {
        return m_alias;
    }

    /**
     * @see ResourceMapping#getPath()
     */
    public String getPath()
    {
        return m_path;
    }

    /**
     * Setter.
     *
     * @param httpContextId id of the http context this resource belongs to
     */
    public void setHttpContextId( final String httpContextId )
    {
        m_httpContextId = httpContextId;
    }

    /**
     * Setter.
     *
     * @param alias alias this resource maps to
     */
    public void setAlias( final String alias )
    {
        m_alias = alias;
    }

    /**
     * Setter.
     *
     * @param path local path in the bundle
     */
    public void setPath( final String path )
    {
        m_path = path;
    }

    @Override
    public String toString()
    {
        return new StringBuffer()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "httpContextId=" ).append( m_httpContextId )
            .append( ",alias=" ).append( m_alias )
            .append( ",path=" ).append( m_path )
            .append( "}" )
            .toString();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_alias == null) ? 0 : m_alias.hashCode());
        result = prime * result + ((m_httpContextId == null) ? 0 : m_httpContextId.hashCode());
        result = prime * result + ((m_path == null) ? 0 : m_path.hashCode());
        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        if( this == obj )
            return true;
        if( obj == null )
            return false;
        if( getClass() != obj.getClass() )
            return false;
        final DefaultResourceMapping other = (DefaultResourceMapping) obj;
        if( m_alias == null )
        {
            if( other.m_alias != null )
                return false;
        }
        else if( !m_alias.equals( other.m_alias ) )
            return false;
        if( m_httpContextId == null )
        {
            if( other.m_httpContextId != null )
                return false;
        }
        else if( !m_httpContextId.equals( other.m_httpContextId ) )
            return false;
        if( m_path == null )
        {
            if( other.m_path != null )
                return false;
        }
        else if( !m_path.equals( other.m_path ) )
            return false;
        return true;
    }

}