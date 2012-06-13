package org.ops4j.pax.web.service.tomcat.internal;

import org.ops4j.pax.web.service.spi.Configuration;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerListener;
import org.ops4j.pax.web.service.spi.model.*;
import org.osgi.service.http.HttpContext;

import javax.servlet.Servlet;

/**
 * Created with IntelliJ IDEA.
 * User: romain.gilles
 * Date: 6/9/12
 * Time: 8:01 AM
 * To change this template use File | Settings | File Templates.
 */
interface ServerState{
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

    enum States {INSTALLED, INITIALIZED, ACTIVE}

    States getState();
}
