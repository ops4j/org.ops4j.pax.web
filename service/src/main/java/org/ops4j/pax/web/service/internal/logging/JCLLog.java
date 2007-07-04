/* Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.internal.logging;

import org.apache.commons.logging.Log;

public class JCLLog implements org.ops4j.pax.web.service.internal.logging.Log
{

    private Log m_jclLog;

    public JCLLog( final Log jclLog )
    {
        m_jclLog = jclLog;
    }

    public boolean isDebugEnabled()
    {
        return m_jclLog.isDebugEnabled();
    }

    public boolean isErrorEnabled()
    {
        return m_jclLog.isErrorEnabled();
    }

    public boolean isFatalEnabled()
    {
        return m_jclLog.isFatalEnabled();
    }

    public boolean isInfoEnabled()
    {
        return m_jclLog.isInfoEnabled();
    }

    public boolean isTraceEnabled()
    {
        return m_jclLog.isTraceEnabled();
    }

    public boolean isWarnEnabled()
    {
        return m_jclLog.isWarnEnabled();
    }

    public void trace( Object message )
    {
        m_jclLog.trace( message );
    }

    public void trace( Object message, Throwable t )
    {
        m_jclLog.trace( message, t );
    }

    public void debug( Object message )
    {
        m_jclLog.debug( message );
    }

    public void debug( Object message, Throwable t )
    {
        m_jclLog.debug( message, t );
    }

    public void info( Object message )
    {
        m_jclLog.info( message );
    }

    public void info( Object message, Throwable t )
    {
        m_jclLog.info( message, t );
    }

    public void warn( Object message )
    {
        m_jclLog.warn( message );
    }

    public void warn( Object message, Throwable t )
    {
        m_jclLog.warn( message, t );
    }

    public void error( Object message )
    {
        m_jclLog.error( message );
    }

    public void error( Object message, Throwable t )
    {
        m_jclLog.error( message, t);
    }

    public void fatal( Object message )
    {
        m_jclLog.fatal( message );
    }

    public void fatal( Object message, Throwable t )
    {
        m_jclLog.fatal( message, t );
    }

    public int getLogLevel()
    {
        return m_jclLog.getLogLevel();
    }
    
}
