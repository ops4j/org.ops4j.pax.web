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

public class NullLog implements Log
{
    public boolean isDebugEnabled()
    {
        return false;
    }

    public boolean isErrorEnabled()
    {
        return false;
    }

    public boolean isFatalEnabled()
    {
        return false;
    }

    public boolean isInfoEnabled()
    {
        return false;
    }

    public boolean isTraceEnabled()
    {
        return false;
    }

    public boolean isWarnEnabled()
    {
        return false;
    }

    public void trace( Object message )
    {
        // do nothing
    }

    public void trace( Object message, Throwable t )
    {
        // do nothing
    }

    public void debug( Object message )
    {
        // do nothing
    }

    public void debug( Object message, Throwable t )
    {
        // do nothing
    }

    public void info( Object message )
    {
        // do nothing
    }

    public void info( Object message, Throwable t )
    {
        // do nothing
    }

    public void warn( Object message )
    {
        // do nothing
    }

    public void warn( Object message, Throwable t )
    {
        // do nothing
    }

    public void error( Object message )
    {
        // do nothing
    }

    public void error( Object message, Throwable t )
    {
        // do nothing
    }

    public void fatal( Object message )
    {
        // do nothing
    }

    public void fatal( Object message, Throwable t )
    {
        // do nothing
    }

    public int getLogLevel()
    {
        return Integer.MAX_VALUE;
    }
}
