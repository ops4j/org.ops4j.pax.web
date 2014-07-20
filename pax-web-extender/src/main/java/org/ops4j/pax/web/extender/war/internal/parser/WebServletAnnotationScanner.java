/**
 * 
 */
package org.ops4j.pax.web.extender.war.internal.parser;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.ops.pax.web.spi.WebAppModel;
import org.ops4j.pax.web.descriptor.gen.FullyQualifiedClassType;
import org.ops4j.pax.web.descriptor.gen.ServletMappingType;
import org.ops4j.pax.web.descriptor.gen.ServletNameType;
import org.ops4j.pax.web.descriptor.gen.ServletType;
import org.ops4j.pax.web.descriptor.gen.TrueFalseType;
import org.ops4j.pax.web.descriptor.gen.UrlPatternType;
import org.osgi.framework.Bundle;

/**
 * @author achim
 * 
 */
public class WebServletAnnotationScanner extends AnnotationScanner<WebServletAnnotationScanner> {

    public WebServletAnnotationScanner(Bundle bundle, String clazz) {
        super(bundle, clazz);
    }

    public void scan(WebAppModel webApp) {
        Class<?> clazz = loadClass();

        if (clazz == null) {
            log.warn("Class {} wasn't loaded", this.className);
            return;
        }

        if (!HttpServlet.class.isAssignableFrom(clazz)) {
            log.warn(clazz.getName() + " is not assignable from javax.servlet.http.HttpServlet");
            return;
        }

        WebServlet annotation = (WebServlet) clazz.getAnnotation(WebServlet.class);

        if (annotation.urlPatterns().length > 0 && annotation.value().length > 0) {
            log.warn(clazz.getName()
                + " defines both @WebServlet.value and @WebServlet.urlPatterns");
            return;
        }

        String[] urlPatterns = annotation.value();
        if (urlPatterns.length == 0) {
            urlPatterns = annotation.urlPatterns();
        }

        if (urlPatterns.length == 0) {
            log.warn(clazz.getName()
                + " defines neither @WebServlet.value nor @WebServlet.urlPatterns");
            return;
        }

        String servletName = (annotation.name().equals("") ? clazz.getName() : annotation.name());

        ServletType webAppServlet = webApp.findServlet(servletName);
        log.debug("Registering Servlet {} with url(s) {}", servletName, urlPatterns);

        if (webAppServlet == null) {
            // Add a new Servlet
            log.debug("Create a new Servlet");
            webAppServlet = new ServletType();
            ServletNameType snt = new ServletNameType();
            snt.setValue(servletName);
            webAppServlet.setServletName(snt);
            FullyQualifiedClassType classType = new FullyQualifiedClassType();
            classType.setValue(className);
            webAppServlet.setServletClass(classType);
            webApp.getServlets().add(webAppServlet);
            if (annotation.loadOnStartup() != -1) {
                webAppServlet.setLoadOnStartup(Integer.toString(annotation.loadOnStartup()));               
            }
            TrueFalseType asyncSupported = new TrueFalseType();
            asyncSupported.setValue(annotation.asyncSupported());
            webAppServlet.setAsyncSupported(asyncSupported);
            // TODO: what about the display Name
        }

//        WebAppInitParam[] initParams = webAppServlet.getInitParams();
//        // check if the existing servlet has each init-param from the
//        // annotation
//        // if not, add it
//        for (WebInitParam ip : annotation.initParams()) {
//            // if (holder.getInitParameter(ip.name()) == null)
//            if (!initParamsContain(initParams, ip.name())) {
//                WebAppInitParam initParam = new WebAppInitParam();
//                initParam.setParamName(ip.name());
//                initParam.setParamValue(ip.value());
//                webAppServlet.addInitParam(initParam);
//            }
//        }

        
        // ServletSpec 3.0 p81 If a servlet already has url mappings from a
        // descriptor the annotation is ignored
        if (!webApp.hasServletMapping(servletName)) {
            log.debug("alter/create mappings");
            ServletMappingType mapping = new ServletMappingType();
            ServletNameType snt = new ServletNameType();
            snt.setValue(servletName);
            mapping.setServletName(snt);
            for (String urlPattern : urlPatterns) {
                log.debug("adding mapping for URL {}", urlPattern);
                UrlPatternType upt = new UrlPatternType();
                upt.setValue(urlPattern);
                mapping.getUrlPattern().add(upt);
            }
            webApp.getServletMappings().add(mapping);
        }
//
//        MultipartConfig multiPartConfigAnnotation = (MultipartConfig) clazz
//            .getAnnotation(MultipartConfig.class);
//
//        if (null != multiPartConfigAnnotation) {
//            MultipartConfigElement multipartConfig = new MultipartConfigElement(
//                multiPartConfigAnnotation);
//            webAppServlet.setMultipartConfig(multipartConfig);
//        }

    }

}
