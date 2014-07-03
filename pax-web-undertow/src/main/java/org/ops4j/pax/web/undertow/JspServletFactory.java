package org.ops4j.pax.web.undertow;

import io.undertow.jsp.HackInstanceManager;
import io.undertow.jsp.JspServletBuilder;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;

import java.util.HashMap;

import org.apache.jasper.deploy.JspPropertyGroup;
import org.apache.jasper.deploy.TagLibraryInfo;
import org.ops4j.pax.web.extender.war.internal.model.WebApp;

public class JspServletFactory {

    public static void addJspServlet(DeploymentInfo deployment, WebApp webApp) {
        ServletInfo jspServlet = JspServletBuilder.createServlet("jsp", "*.jsp");
        deployment.addServlet(jspServlet);
        JspServletBuilder.setupDeployment(deployment, new HashMap<String, JspPropertyGroup>(),
            new HashMap<String, TagLibraryInfo>(), new HackInstanceManager());

    }

}
