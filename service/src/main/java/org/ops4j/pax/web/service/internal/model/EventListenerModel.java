package org.ops4j.pax.web.service.internal.model;

import java.util.EventListener;

public class EventListenerModel extends BasicModel
{

    private final EventListener m_eventListener;

    public EventListenerModel( final EventListener eventListener,
                               final ContextModel contextModel )
    {
        super( contextModel );

        m_eventListener = eventListener;
    }

    public EventListener getEventListener()
    {
        return m_eventListener;
    }

}