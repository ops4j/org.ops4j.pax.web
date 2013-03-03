/* Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.samples.helloworld.wc.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Hello World Servlet.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 02, 2008
 */
public class HelloWorldServlet
      extends HttpServlet
{

   /**
    * 
    */
    private static final long serialVersionUID = 1633766459629276016L;

    protected void doGet( final HttpServletRequest request, final HttpServletResponse response )
         throws ServletException, IOException
   {
      /*
       * The container DOES NOT create the session if the servlet does not use it at least 
       * once, so, we do need the line below in order to create the session properly.
       */
      final HttpSession session = request.getSession();

      String userName = (String) session.getAttribute( "userName" );
      if( userName == null )
      {
         userName = "user_" + HelloWorldSessionListener.getCounter();
         session.setAttribute( "userName", userName );
      }

      final PrintWriter writer = response.getWriter();
      writer.println( "<body align='center'>" );
      writer.println( "<h1>Hello World</h1>" );
      writer.println( "<img src='"+request.getContextPath()+"/images/logo.png' border='0'/>" );
      writer.println( "<h1>" + getServletConfig().getInitParameter( "from" ) + "</h1>" );
      writer.print( getServletContext().getAttribute( "requestCounter" ).toString() + " requests" );
      writer.println( "<h1>Current User Name: " + userName + "</h1>" );
      writer.println( "<h1>HttpSessionListener:</h1>" );
      writer.println( getSessionListenerData() );
      writer.println( "</body>" );
   }

   private static String getSessionListenerData()
   {
      final StringBuffer s = new StringBuffer();

      final List< Object > userNames = HelloWorldSessionListener.getAttributes( "userName" );
      s.append( userNames.size() ).append( "<br>" );

      for( final Object userName: userNames )
      {
         s.append( userName ).append( "<br>" );
      }

      return s.toString();
   }
}