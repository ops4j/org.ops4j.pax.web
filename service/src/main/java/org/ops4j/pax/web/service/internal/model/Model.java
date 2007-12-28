package org.ops4j.pax.web.service.internal.model;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.osgi.service.http.HttpContext;
import org.ops4j.pax.web.service.internal.util.Assert;

public class Model
{

    private final String m_id;
    private final HttpContext m_httpContext;
    private final ClassLoader m_classLoader;

    private static final Lock lock = new ReentrantLock();
    private static Integer m_next = 0;

    Model( final HttpContext httpContext, final ClassLoader classLoader )
    {
        Assert.notNull( "Http Context cannot be null", httpContext );
        Assert.notNull( "Class loader cannot be null", classLoader );
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
        m_classLoader = classLoader;
        m_httpContext = httpContext;
    }

    public String getId()
    {
        return m_id;
    }

    public HttpContext getHttpContext()
    {
        return m_httpContext;
    }

    public ClassLoader getClassLoader()
    {
        return m_classLoader;
    }

    @Override
    public String toString()
    {
        return new StringBuilder()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "id=" ).append( m_id )
            .append( ",httpContext=" ).append( m_httpContext )
            .append( "}" )
            .toString();
    }

}
