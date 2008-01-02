package org.ops4j.pax.web.samples.helloworld.wc.internal;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HelloWorldServlet
    extends HttpServlet
{

    protected void doGet( final HttpServletRequest request, final HttpServletResponse response )
        throws ServletException, IOException
    {
        response.setContentType( "text/html" );

        final PrintWriter writer = response.getWriter();
        writer.println( "<html><body align='center'>" );
        writer.println( "<h1>Hello World</h1>" );
        writer.println( "<img src='/images/logo.png' border='0'/>" );
        writer.println( "<h1>" + getServletConfig().getInitParameter( "from" ) + "</h1>" );
        writer.println( "</body></html>" );
    }

}

