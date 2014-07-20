/*
 * Copyright 2014 Harald Wellmann.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.undertow;

import io.undertow.jsp.HackInstanceManager;
import io.undertow.jsp.JspServletBuilder;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;

import java.util.HashMap;

import org.apache.jasper.deploy.JspPropertyGroup;
import org.apache.jasper.deploy.TagLibraryInfo;
import org.ops.pax.web.spi.WabModel;

public class JspServletFactory {

    public static void addJspServlet(DeploymentInfo deployment, WabModel webApp) {
        ServletInfo jspServlet = JspServletBuilder.createServlet("jsp", "*.jsp");
        deployment.addServlet(jspServlet);
        JspServletBuilder.setupDeployment(deployment, new HashMap<String, JspPropertyGroup>(),
            new HashMap<String, TagLibraryInfo>(), new HackInstanceManager());
    }
}
