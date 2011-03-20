package org.apache.myfaces.test;

import java.io.IOException;

import javax.faces.webapp.FacesServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class CustomFacesServlet implements Servlet
{
    private FacesServlet _facesServlet;

    public void destroy()
    {
        _facesServlet.destroy();
    }

    public ServletConfig getServletConfig()
    {
        return _facesServlet.getServletConfig();
    }

    public String getServletInfo()
    {
        return _facesServlet.getServletInfo();
    }

    public void init(ServletConfig servletConfig) throws ServletException
    {
        _facesServlet.init(servletConfig);
    }

    public void service(ServletRequest request, ServletResponse response)
            throws IOException, ServletException
    {
        _facesServlet.service(request, response);
    }
    
}
