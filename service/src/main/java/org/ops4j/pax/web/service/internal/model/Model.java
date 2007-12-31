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
package org.ops4j.pax.web.service.internal.model;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.ops4j.lang.NullArgumentException;

public class Model
{

    private final String m_id;
    private final ContextModel m_contextModel;

    private static final Lock lock = new ReentrantLock();
    private static Integer m_next = 0;

    Model( final ContextModel contextModel )
    {
        NullArgumentException.validateNotNull( contextModel, "Context model" );
        m_contextModel = contextModel;
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

    public ContextModel getContextModel()
    {
        return m_contextModel;
    }

    @Override
    public String toString()
    {
        return new StringBuilder()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "id=" ).append( m_id )
            .append( ",context=" ).append( m_contextModel )
            .append( "}" )
            .toString();
    }

}
