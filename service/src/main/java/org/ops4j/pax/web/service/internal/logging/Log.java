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

public interface Log
{
    boolean isDebugEnabled();
    boolean isErrorEnabled();
    boolean isFatalEnabled();
    boolean isInfoEnabled();
    boolean isTraceEnabled();
    boolean isWarnEnabled();
    void trace( Object message );
    void trace( Object message, Throwable t );
    void debug( Object message );
    void debug( Object message, Throwable t );
    void info( Object message );
    void info( Object message, Throwable t );
    void warn( Object message );
    void warn( Object message, Throwable t );
    void error( Object message );
    void error( Object message, Throwable t );
    void fatal( Object message );
    void fatal( Object message, Throwable t );
    int getLogLevel();
}
