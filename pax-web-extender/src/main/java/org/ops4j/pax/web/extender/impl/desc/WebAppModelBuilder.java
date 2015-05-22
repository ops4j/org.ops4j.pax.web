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
package org.ops4j.pax.web.extender.impl.desc;

import javax.xml.bind.JAXBElement;

import org.ops4j.pax.web.descriptor.gen.ErrorPageType;
import org.ops4j.pax.web.descriptor.gen.FilterMappingType;
import org.ops4j.pax.web.descriptor.gen.FilterType;
import org.ops4j.pax.web.descriptor.gen.JspConfigType;
import org.ops4j.pax.web.descriptor.gen.ListenerType;
import org.ops4j.pax.web.descriptor.gen.LoginConfigType;
import org.ops4j.pax.web.descriptor.gen.MimeMappingType;
import org.ops4j.pax.web.descriptor.gen.ParamValueType;
import org.ops4j.pax.web.descriptor.gen.SecurityConstraintType;
import org.ops4j.pax.web.descriptor.gen.SecurityRoleType;
import org.ops4j.pax.web.descriptor.gen.ServletMappingType;
import org.ops4j.pax.web.descriptor.gen.ServletType;
import org.ops4j.pax.web.descriptor.gen.SessionConfigType;
import org.ops4j.pax.web.descriptor.gen.WebAppType;
import org.ops4j.pax.web.descriptor.gen.WelcomeFileListType;
import org.ops4j.pax.web.spi.WebAppModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WebAppModelBuilder {
    
    private static Logger log = LoggerFactory.getLogger(WebAppModelBuilder.class);
    
    private WebAppModel model;

    public WebAppModelBuilder(WebAppType webAppType) {
        this.model = new WebAppModel(webAppType);
    }
    
    
    
    public WebAppModel build() {
        for (JAXBElement<?> elem : model.getWebApp().getModuleNameOrDescriptionAndDisplayName()) {
            Object value = elem.getValue();
            if (value instanceof ParamValueType) {
                ParamValueType contextParam = (ParamValueType) value;
                model.getContextParams().add(contextParam);
            }
            else if (value instanceof FilterType) {
                FilterType filterType = (FilterType) value;
                model.getFilters().add(filterType);
            }
            else if (value instanceof FilterMappingType) {
                FilterMappingType filterMapping = (FilterMappingType) value;
                model.getFilterMappings().add(filterMapping);
                model.putFilterMapping(filterMapping.getFilterName().getValue(), filterMapping);
            }
            else if (value instanceof ListenerType) {
                ListenerType listener = (ListenerType) value;
                model.getListeners().add(listener);
            }
            else if (value instanceof ServletType) {
                ServletType servlet = (ServletType) value;
                model.getServlets().add(servlet);
            }
            else if (value instanceof ServletMappingType) {
                ServletMappingType servletMapping = (ServletMappingType) value;
                model.getServletMappings().add(servletMapping);
            }
            else if (value instanceof SessionConfigType) {
                SessionConfigType sessionConfig = (SessionConfigType) value;
                if (model.getSessionConfig() == null) {
                    model.setSessionConfig(sessionConfig);
                }
                else {
                    log.error("duplicate <session-config>");
                }
            }
            else if (value instanceof MimeMappingType) {
                MimeMappingType mimeMapping = (MimeMappingType) value;
                model.getMimeMappings().add(mimeMapping);
            }
            else if (value instanceof WelcomeFileListType) {
                WelcomeFileListType welcomeFileList = (WelcomeFileListType) value;
                if (model.getWelcomeFileList() == null) {
                    model.setWelcomeFileList(welcomeFileList);
                }
                else {
                    log.error("duplicate <welcome-file-list>");
                }
            }
            else if (value instanceof ErrorPageType) {
                ErrorPageType errorPage = (ErrorPageType) value;
                model.getErrorPages().add(errorPage);
            }
            else if (value instanceof JspConfigType) {
                JspConfigType jspConfig = (JspConfigType) value;
                if (model.getJspConfig() == null) {
                    model.setJspConfig(jspConfig);
                }
                else {
                    log.error("duplicate <jsp-config>");
                }
            }
            else if (value instanceof SecurityConstraintType) {
                SecurityConstraintType securityConstraint = (SecurityConstraintType) value;
                model.getSecurityConstraints().add(securityConstraint);
            }
            else if (value instanceof LoginConfigType) {
                LoginConfigType loginConfig = (LoginConfigType) value;
                if (model.getLoginConfig() == null) {
                    model.setLoginConfig(loginConfig);
                }
                else {
                    log.error("duplicate <login-config>");
                }
            }
            else if (value instanceof SecurityRoleType) {
                SecurityRoleType securityRole = (SecurityRoleType) value;
                model.getSecurityRoles().add(securityRole);
            }
            else {
                log.warn("unhandled element [{}] of type [{}]", 
                    elem.getName(), value.getClass().getSimpleName());
            }
        }
        
        return model;
    }
    

}
