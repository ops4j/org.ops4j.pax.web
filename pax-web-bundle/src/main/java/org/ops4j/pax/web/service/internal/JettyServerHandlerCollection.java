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
package org.ops4j.pax.web.service.internal;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.jetty.EofException;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.servlet.Context;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;

/**
 * Jety Handler collection that calls only the handler (=context) that matches the request path after performing the
 * substring based matching of requestes path to registered aaliases.
 *
 * @author Alin Dreghiciu
 * @since 0.2.3, December 22, 2007
 */
class JettyServerHandlerCollection
    extends HandlerCollection
{

    private final ServerModel m_serverModel;

    JettyServerHandlerCollection( final ServerModel serverModel )
    {
        NullArgumentException.validateNotNull( serverModel, "Service model" );
        m_serverModel = serverModel;
    }

    @Override
    public void handle(
        final String target,
        final HttpServletRequest request,
        final HttpServletResponse response,
        final int dispatch )
        throws IOException, ServletException
    {
        final ContextModel matched = m_serverModel.matchPathToContext( target );
        if( matched != null )
        {
            final Context context =
                ( (JettyServerWrapper) getServer() ).getContext( matched.getHttpContext() );
            try
            {
                context.handle( target, request, response, dispatch );
            }
            catch( EofException e )
            {
                throw e;
            }
            catch( RuntimeException e )
            {
                throw e;
            }
            catch( Exception e )
            {
                throw new ServletException( e );
            }

            // TODO verify that this is still necessary
            if( !response.isCommitted() )
            {
                // force commit
                response.flushBuffer();
            }
        }
    }

}
