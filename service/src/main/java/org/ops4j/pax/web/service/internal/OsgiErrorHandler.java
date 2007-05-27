/*  Copyright 2007 Niclas Hedhman.
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
package org.ops4j.pax.web.service.internal;

import org.mortbay.jetty.handler.ErrorHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OsgiErrorHandler extends ErrorHandler
{
    private static Log m_logger = LogFactory.getLog( OsgiErrorHandler.class );
    private String m_name;

    public OsgiErrorHandler( String name )
    {
        m_name = name;
    }

    protected synchronized void doStart()
        throws Exception
    {
        m_logger.info( m_name + " -> doStart()" );
        super.doStart();
    }

    protected synchronized void doStop()
        throws Exception
    {
        m_logger.info( m_name + " -> doStop()" );
        super.doStop();
    }

}
