/* Copyright 2008 Alin Dreghiciu.
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
package org.apache.juli.logging;

/**
 * Apache Tomcat juli -> jcl adapter.
 * The reason this classes are here is to redirect juli logging to apache commons logging.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 07, 2008
 */
public class JclLogAdapter
    implements Log
{

    private final org.apache.commons.logging.Log m_delegate;

    JclLogAdapter( org.apache.commons.logging.Log delegate)
    {

        m_delegate = delegate;
    }

    public void debug( Object o )
    {
        m_delegate.debug( o );
    }

    public void debug( Object o, Throwable throwable )
    {
        m_delegate.debug( o, throwable );
    }

    public void error( Object o )
    {
        m_delegate.error( o );
    }

    public void error( Object o, Throwable throwable )
    {
        m_delegate.error( o, throwable );
    }

    public void fatal( Object o )
    {
        m_delegate.fatal( o );
    }

    public void fatal( Object o, Throwable throwable )
    {
        m_delegate.fatal( o, throwable );
    }

    public void info( Object o )
    {
        m_delegate.info( o );
    }

    public void info( Object o, Throwable throwable )
    {
        m_delegate.info( o, throwable );
    }

    public boolean isDebugEnabled()
    {
        return m_delegate.isDebugEnabled();
    }

    public boolean isErrorEnabled()
    {
        return m_delegate.isErrorEnabled();
    }

    public boolean isFatalEnabled()
    {
        return m_delegate.isFatalEnabled();
    }

    public boolean isInfoEnabled()
    {
        return m_delegate.isInfoEnabled();
    }

    public boolean isTraceEnabled()
    {
        return m_delegate.isTraceEnabled();
    }

    public boolean isWarnEnabled()
    {
        return m_delegate.isWarnEnabled();
    }

    public void trace( Object o )
    {
        m_delegate.trace( o );
    }

    public void trace( Object o, Throwable throwable )
    {
        m_delegate.trace( o, throwable );
    }

    public void warn( Object o )
    {
        m_delegate.warn( o );
    }

    public void warn( Object o, Throwable throwable )
    {
        m_delegate.warn( o, throwable );
    }
}
