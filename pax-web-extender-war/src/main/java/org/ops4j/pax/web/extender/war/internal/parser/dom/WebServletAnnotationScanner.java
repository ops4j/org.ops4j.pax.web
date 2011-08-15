/**
 * 
 */
package org.ops4j.pax.web.extender.war.internal.parser.dom;

import java.util.List;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.model.WebAppInitParam;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServlet;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServletMapping;

/**
 * @author achim
 *
 */
public class WebServletAnnotationScanner extends AnnotationScanner<WebServletAnnotationScanner>{

	public WebServletAnnotationScanner(String clazz) {
		super(clazz);
	}
	
	public void scan(WebApp webApp) {
		Class clazz = loadClass();
		
		if (clazz == null) {
			LOG.warn("Class %s wasn't loaded", this.className);
			return;
		}
		
		if (!HttpServlet.class.isAssignableFrom(clazz)) {
			LOG.warn(clazz.getName()+" is not assignable from javax.servlet.http.HttpServlet");
            return;
		}
		
		WebServlet annotation = (WebServlet)clazz.getAnnotation(WebServlet.class);
		
		if (annotation.urlPatterns().length > 0 && annotation.value().length > 0)
        {
            LOG.warn(clazz.getName()+ " defines both @WebServlet.value and @WebServlet.urlPatterns");
            return;
        }
        
        String[] urlPatterns = annotation.value();
        if (urlPatterns.length == 0)
            urlPatterns = annotation.urlPatterns();
        
        if (urlPatterns.length == 0)
        {
            LOG.warn(clazz.getName()+ " defines neither @WebServlet.value nor @WebServlet.urlPatterns");
            return;
        }
        
        String servletName = (annotation.name().equals("")?clazz.getName():annotation.name());
        
        WebAppServlet webAppServlet = webApp.findServlet(servletName);
        
        		
        if (webAppServlet == null) {
        	// Add a new Servlet
        	final WebAppServlet servlet = new WebAppServlet();
			servlet.setServletName(servletName);
			servlet.setServletClass(className);
			webApp.addServlet(servlet);
			servlet.setLoadOnStartup(annotation.loadOnStartup());
			servlet.setAsyncSupported(annotation.asyncSupported());
			//TODO: what about the display Name
        } else {
        	//alter existing Servlet
        	WebAppInitParam[] initParams = webAppServlet.getInitParams();
        	//check if the existing servlet has each init-param from the annotation
            //if not, add it
            for (WebInitParam ip:annotation.initParams())
            {
              //if (holder.getInitParameter(ip.name()) == null)
                if (!initParamsContain(initParams, ip.name()))
                {
                    WebAppInitParam initParam = new WebAppInitParam();
                    initParam.setParamName(ip.name());
                    initParam.setParamValue(ip.value());
                    webAppServlet.addInitParam(initParam);
                }  
            }
            
            //check the url-patterns, if there annotation has a new one, add it
            List<WebAppServletMapping> mappings = webApp.getServletMappings(servletName);

            //ServletSpec 3.0 p81 If a servlet already has url mappings from a 
            //descriptor the annotation is ignored
            if (mappings == null && mappings.isEmpty())
            {
                for (String urlPattern : urlPatterns) {
                	WebAppServletMapping mapping = new WebAppServletMapping();
                	mapping.setServletName(servletName);
                	mapping.setUrlPattern(urlPattern);
                	webApp.addServletMapping(mapping);
				}
            }
        }
		
	}

}
