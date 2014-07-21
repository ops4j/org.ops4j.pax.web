/**
 * 
 */
package org.ops4j.pax.web.extender.war.internal.parser;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.annotation.WebFilter;

import org.ops.pax.web.spi.WebAppModel;
import org.ops4j.pax.web.descriptor.gen.FilterMappingType;
import org.ops4j.pax.web.descriptor.gen.FilterNameType;
import org.ops4j.pax.web.descriptor.gen.FilterType;
import org.ops4j.pax.web.descriptor.gen.FullyQualifiedClassType;
import org.ops4j.pax.web.descriptor.gen.ServletNameType;
import org.ops4j.pax.web.descriptor.gen.UrlPatternType;
import org.osgi.framework.Bundle;

/**
 * @author achim
 * 
 */
public class WebFilterAnnotationScanner extends AnnotationScanner<WebFilterAnnotationScanner> {

    public WebFilterAnnotationScanner(Bundle bundle, String className) {
        super(bundle, className);
    }

    public void scan(WebAppModel webApp) {
        Class<?> clazz = loadClass();

        if (clazz == null) {
            log.warn("Class %s wasn't loaded", this.className);
            return;
        }

        if (!Filter.class.isAssignableFrom(clazz)) {
            log.warn(clazz.getName() + " is not assignable from javax.servlet.Filter");
            return;
        }

        WebFilter filterAnnotation = (WebFilter) clazz.getAnnotation(WebFilter.class);

        if (filterAnnotation.value().length > 0 && filterAnnotation.urlPatterns().length > 0) {
            log.warn(clazz.getName() + " defines both @WebFilter.value and @WebFilter.urlPatterns");
            return;
        }

        String name = (filterAnnotation.filterName().equals("") ? clazz.getName()
            : filterAnnotation.filterName());
        String[] urlPatterns = filterAnnotation.value();

        FilterType filter = webApp.findFilter(name);

        FilterNameType fnt = new FilterNameType();
        fnt.setValue(name);
        if (filter == null) {
            filter = new FilterType();
            filter.setFilterName(fnt);
            FullyQualifiedClassType klass = new FullyQualifiedClassType();
            klass.setValue(className);
            filter.setFilterClass(klass);
            webApp.getFilters().add(filter);

            // TODO: what about the DisplayName?

            // holder.setDisplayName(filterAnnotation.displayName());
            // metaData.setOrigin(name+".filter.display-name");

//            for (WebInitParam ip : filterAnnotation.initParams()) {
//                WebAppInitParam initParam = new WebAppInitParam();
//                initParam.setParamName(ip.name());
//                initParam.setParamValue(ip.value());
//                filter.addInitParam(initParam);
//            }
            
            
            FilterMappingType filterMapping = new FilterMappingType();
            filterMapping.setFilterName(fnt);
            webApp.getFilterMappings().add(filterMapping);

            if (urlPatterns == null || urlPatterns.length == 0) {
                urlPatterns = filterAnnotation.urlPatterns();
            }

            
            for (String urlPattern : urlPatterns) {
                UrlPatternType upt = new UrlPatternType();
                upt.setValue(urlPattern);
                filterMapping.getUrlPatternOrServletName().add(upt);
            }

            for (String servletName : filterAnnotation.servletNames()) {
                ServletNameType snt = new ServletNameType();
                snt.setValue(servletName);
                filterMapping.getUrlPatternOrServletName().add(snt);
            }

            for (DispatcherType d : filterAnnotation.dispatcherTypes()) {
                org.ops4j.pax.web.descriptor.gen.DispatcherType dt = new org.ops4j.pax.web.descriptor.gen.DispatcherType();
                dt.setValue(d.toString());
                filterMapping.getDispatcher().add(dt);
            }
        }
        else {
//            WebAppInitParam[] initParams = filter.getInitParams();
            // A Filter definition for the same name already exists from web.xml
            // ServletSpec 3.0 p81 if the Filter is already defined and has
            // mappings,
            // they override the annotation. If it already has DispatcherType
            // set, that
            // also overrides the annotation. Init-params are additive, but
            // web.xml overrides
            // init-params of the same name.
//            for (WebInitParam ip : filterAnnotation.initParams()) {
//                // if (holder.getInitParameter(ip.name()) == null)
//                if (!initParamsContain(initParams, name)) {
//                    WebAppInitParam initParam = new WebAppInitParam();
//                    initParam.setParamName(ip.name());
//                    initParam.setParamValue(ip.value());
//                    filter.addInitParam(initParam);
//                }
//            }

            // if a descriptor didn't specify at least one mapping, use the
            // mappings from the annotation and the DispatcherTypes
            // from the annotation
            if (!webApp.hasFilterMapping(name)) {
                FilterMappingType filterMapping = new FilterMappingType();
                filterMapping.setFilterName(fnt);
                webApp.getFilterMappings().add(filterMapping);
                for (String urlPattern : urlPatterns) {
                    UrlPatternType upt = new UrlPatternType();
                    upt.setValue(urlPattern);
                    filterMapping.getUrlPatternOrServletName().add(upt);
                }

                for (String servletName : filterAnnotation.servletNames()) {
                    ServletNameType snt = new ServletNameType();
                    snt.setValue(servletName);
                    filterMapping.getUrlPatternOrServletName().add(snt);
                }

                for (DispatcherType d : filterAnnotation.dispatcherTypes()) {
                    org.ops4j.pax.web.descriptor.gen.DispatcherType dt = new org.ops4j.pax.web.descriptor.gen.DispatcherType();
                    dt.setValue(d.toString());
                    filterMapping.getDispatcher().add(dt);
                }
            }
        }
    }
}
