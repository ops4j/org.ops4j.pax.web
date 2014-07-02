package org.ops4j.pax.web.extender.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBeanAttributes;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ServletScanExtension implements Extension {
    
    private static Logger log = LoggerFactory.getLogger(ServletScanExtension.class);

    <T> void processAnnotatedType(
        @WithAnnotations({ WebServlet.class, WebFilter.class,WebListener.class }) 
        @Observes ProcessAnnotatedType<T>  event) {
        
        AnnotatedType<T> annotatedType = event.getAnnotatedType();
        if (annotatedType.getAnnotation(WebServlet.class) != null) {
            log.info("@WebServlet on {}", annotatedType.getJavaClass().getName());
        }
        if (annotatedType.getAnnotation(WebFilter.class) != null) {
            log.info("@WebFilter on {}", annotatedType.getJavaClass().getName());
        }
        if (annotatedType.getAnnotation(WebListener.class) != null) {
            log.info("@WebListener on {}", annotatedType.getJavaClass().getName());
        }
    }

    <T> void processBeanAttributes(@Observes ProcessBeanAttributes<T> attributes) {
        //attributes.veto();
    }

}
