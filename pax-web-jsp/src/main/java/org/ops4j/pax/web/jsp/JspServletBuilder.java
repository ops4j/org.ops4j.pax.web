package org.ops4j.pax.web.jsp;

import io.undertow.servlet.api.ServletInfo;

import org.apache.jasper.servlet.JspServlet;



public class JspServletBuilder {

    public static ServletInfo createServlet(final String name, final String path) {
        ServletInfo servlet = new ServletInfo(name, JspServlet.class);
        servlet.addMapping(path);
        //if the JSP servlet is mapped to a path that ends in /*
        //we want to perform welcome file matches if the directory is requested
        servlet.setRequireWelcomeFileMapping(true);
        return servlet;
    }
}
