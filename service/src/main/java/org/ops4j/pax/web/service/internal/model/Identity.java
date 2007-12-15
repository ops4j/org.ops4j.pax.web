package org.ops4j.pax.web.service.internal.model;

public class Identity
{

    private static Integer m_next = 0;
    protected final String m_id;

    public Identity()
    {
        synchronized( m_next )
        {
            m_next++;
            m_id = this.getClass().getName() + "-" + m_next;
        }
    }

    public String getId()
    {
        return m_id;
    }
}
