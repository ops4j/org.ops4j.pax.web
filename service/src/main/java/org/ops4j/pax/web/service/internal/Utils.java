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
package org.ops4j.pax.web.service.internal;

/**
 * Divers utilities.
 *
 * @author Alin Dreghiciu
 * @since 0.2.1
 */
public class Utils
{

    /**
     * Utility class. Ment to be used via static methods.
     */
    private Utils()
    {
        // utility class. Ment to be used via static methods.
    }

    /**
     * Replaces multiple subsequent slashes with one slash.
     * E.g. ////a//path// will becaome /a/path/
     *
     * @param target target sring to be replaced
     *
     * @return a string where the subsequent slashes are replaced with one slash
     */
    static String replaceSlashes( String target )
    {
        String replaced = target;
        if( replaced != null )
        {
            replaced = replaced.replaceAll( "/+", "/" );
        }
        return replaced;
//        char[] seq = target.toCharArray();
//        if( seq[ seq.length - 1 ] == c )
//        {
//            for( int cursor = seq.length - 1; cursor >= 0; cursor-- )
//            {
//                if( seq[ cursor ] != c )
//                {
//                    // plus 2 is allowed because we know there is one more
//                    // (first condition in method)
//                    // and we are just at the first non slash
//                    // and it is necessary to ensure consistence with expected
//                    // match flow
//                    // hint: we just trim trailing slashes, so /foo// becomes
//                    // /foo/
//                    return target.substring( 0, cursor + 2 );
//                }
//                else if( cursor == 0 )
//                {
//                    // all slashes (like "//////") if c = '/' (for example)
//                    return "" + c;
//                }
//            }
//        }
//        return target;
    }

}
