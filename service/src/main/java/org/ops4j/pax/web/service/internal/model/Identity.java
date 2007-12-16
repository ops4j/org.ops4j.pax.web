package org.ops4j.pax.web.service.internal.model;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Identity
{

    private static final Lock lock = new ReentrantLock();
    private static Integer m_next = 0;
    protected final String m_id;

    public Identity()
    {
        lock.lock();
        try
        {
            m_next++;
            m_id = this.getClass().getName() + "-" + m_next;
        }
        finally
        {
            lock.unlock();
        }
    }

    public String getId()
    {
        return m_id;
    }
}
