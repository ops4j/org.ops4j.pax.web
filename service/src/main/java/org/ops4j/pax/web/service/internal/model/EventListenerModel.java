package org.ops4j.pax.web.service.internal.model;

import java.util.EventListener;
import org.osgi.service.http.HttpContext;

public class EventListenerModel extends Model
{

    private final EventListener m_eventListener;

    public EventListenerModel( final HttpContext httpContext,
                               final EventListener eventListener,
                               final ClassLoader classLoader )
    {
        super( httpContext, classLoader );
        m_eventListener = eventListener;
    }

    public EventListener getEventListener()
    {
        return m_eventListener;
    }

}