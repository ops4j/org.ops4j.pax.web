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

import java.util.EventListener;

import org.ops4j.pax.web.extender.whiteboard.ListenerMapping;

/**
 * Default implementation of {@link ListenerMapping}.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class DefaultListenerMapping
    implements ListenerMapping
{

    /**
     * Http Context id.
     */
    private String m_httpContextId;
    /**
     * Listener.
     */
    private EventListener m_listener;

    /**
     * @see ListenerMapping#getHttpContextId()
     */
    public String getHttpContextId()
    {
        return m_httpContextId;
    }

    /**
     * @see ListenerMapping#getListener() ()
     */
    public EventListener getListener()
    {
        return m_listener;
    }

    /**
     * Setter.
     *
     * @param httpContextId id of the http context this servlet belongs to
     */
    public void setHttpContextId( final String httpContextId )
    {
        m_httpContextId = httpContextId;
    }

    /**
     * Setter.
     *
     * @param listener mapped listener
     */
    public void setListener( final EventListener listener )
    {
        m_listener = listener;
    }

    @Override
    public String toString()
    {
        return new StringBuffer()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "httpContextId=" ).append( m_httpContextId )
            .append( ",listener=" ).append( m_listener )
            .append( "}" )
            .toString();
    }

}