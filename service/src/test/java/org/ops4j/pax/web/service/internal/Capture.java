/*  Copyright 2007 Alin Dreghiciu.
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

import org.easymock.IArgumentMatcher;
import org.easymock.internal.matchers.Any;
import static org.easymock.EasyMock.reportMatcher;

public class Capture<T> implements IArgumentMatcher
{
    private T _captured;
    private IArgumentMatcher m_matcher;

    public Capture()
    {
        this( Any.ANY );
    }

    public Capture(IArgumentMatcher matcher)
    {
        m_matcher = matcher;
    }

    public void appendTo(StringBuffer buffer)
    {
        m_matcher.appendTo( buffer );
    }

    @SuppressWarnings("unchecked")
    public boolean matches(Object parameter)
    {
        _captured = (T) parameter;
        return m_matcher.matches( parameter );
    }

    public T getCaptured()
    {
        return _captured;
    }

    public static <T> T capture(Capture<T> capture)
    {
        reportMatcher(capture);
        return null;
    }

}
