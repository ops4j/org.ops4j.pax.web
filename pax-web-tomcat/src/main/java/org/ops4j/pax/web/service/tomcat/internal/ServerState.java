package org.ops4j.pax.web.service.tomcat.internal;

import javax.servlet.Servlet;

import org.ops4j.pax.web.service.spi.Configuration;
import org.ops4j.pax.web.service.spi.LifeCycle;
import org.ops4j.pax.web.service.spi.model.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.FilterModel;
import org.ops4j.pax.web.service.spi.model.SecurityConstraintMappingModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.osgi.service.http.HttpContext;

/**
 * @author Romaim Gilles
 */
interface ServerState
{
    ServerState start();

    ServerState stop();

    boolean isStarted();

    boolean isConfigured();

    ServerState configure(Configuration configuration);

    Configuration getConfiguration();

    void removeContext(HttpContext httpContext);

    void addServlet(ServletModel model);

    void removeServlet(ServletModel model);

    void addEventListener(EventListenerModel eventListenerModel);

    void removeEventListener(EventListenerModel eventListenerModel);

    void addFilter(FilterModel filterModel);

    void removeFilter(FilterModel filterModel);

    void addErrorPage(ErrorPageModel model);

    void removeErrorPage(ErrorPageModel model);

    Integer getHttpPort();

    Integer getHttpSecurePort();

    Servlet createResourceServlet(ContextModel contextModel, String alias, String name);

    void addSecurityConstraintMapping(SecurityConstraintMappingModel secMapModel);

    void addContainerInitializerModel(ContainerInitializerModel model);

    LifeCycle getContext(ContextModel model);

    enum States
    {
        INSTALLED, INITIALIZED, ACTIVE
    }

    States getState();
}
