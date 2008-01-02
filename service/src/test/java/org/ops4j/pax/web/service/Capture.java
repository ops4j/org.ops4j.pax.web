/*
 * Copyright 2007 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service;

import static org.easymock.EasyMock.*;
import org.easymock.IArgumentMatcher;

class Capture<T> implements IArgumentMatcher
{

    private T _captured;

    public void appendTo( StringBuffer buffer )
    {
        buffer.append( "capture()" );
    }

    @SuppressWarnings( "unchecked" )
    public boolean matches( Object parameter )
    {
        _captured = (T) parameter;
        return true;
    }

    public T getCaptured()
    {
        return _captured;
    }

    public static <C> C capture( Capture<C> capture )
    {
        reportMatcher( capture );
        return null;
    }

}