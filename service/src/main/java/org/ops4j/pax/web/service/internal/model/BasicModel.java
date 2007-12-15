package org.ops4j.pax.web.service.internal.model;

public class BasicModel extends Identity
{

    private final ContextModel m_contextModel;

    public BasicModel( ContextModel contextModel )
    {
        m_contextModel = contextModel;
    }

    public ContextModel getContextModel()
    {
        return m_contextModel;
    }
}
