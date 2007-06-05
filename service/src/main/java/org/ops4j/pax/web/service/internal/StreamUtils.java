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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamUtils
{

    private StreamUtils()
    {
        // utility class
    }

    public static int copy(
        OutputStream outputStream,
        InputStream inputStream )
        throws IOException
    {
        if( !(outputStream instanceof BufferedOutputStream) )
        {
            outputStream = new BufferedOutputStream( outputStream );
        }
        if( !(inputStream instanceof BufferedInputStream) )
        {
            inputStream = new BufferedInputStream( inputStream );
        }
        int count = 0;
        try
        {
            int b = inputStream.read();
            while( b != -1 )
            {
                outputStream.write( b );
                count += b;
                b = inputStream.read();
            }
        }
        finally
        {
            outputStream.flush();
            inputStream.close();
        }
        return count;
    }

}
