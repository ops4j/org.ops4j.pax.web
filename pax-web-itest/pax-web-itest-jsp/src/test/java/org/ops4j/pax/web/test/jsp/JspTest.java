/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.test.jsp;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;
import javax.servlet.Servlet;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpServletResponse;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class JspTest {

    public static Logger log = LoggerFactory.getLogger(JspTest.class);

    @BeforeClass
    @SuppressWarnings("all")
    public static void init() throws Exception {
        Class<?> jspFactoryClass = Class.forName("javax.servlet.jsp.JspFactory");
        Method setDefaultFactory = jspFactoryClass.getDeclaredMethod("setDefaultFactory", jspFactoryClass);
        Class<?> jspFactoryImplClass = Class.forName("org.apache.jasper.runtime.JspFactoryImpl");
        setDefaultFactory.invoke(null, jspFactoryImplClass.newInstance());
    }

    @Test
    public void insstantiateServlet() throws Exception {
        Class<?> servletClass = Class.forName("org.apache.jasper.servlet.JspServlet");
        Constructor<?> c = servletClass.getConstructor();
        Servlet jspServlet = (Servlet) c.newInstance();
        assertNotNull(jspServlet);

        File scratchDir = new File("target", UUID.randomUUID().toString());
        scratchDir.mkdirs();

        /*
         * - checkInterval - If development is false and checkInterval is greater than zero, background compiles are enabled. checkInterval is the time in seconds between checks to see if a JSP page (and its dependent files) needs to be recompiled. Default 0 seconds.
         * - classdebuginfo - Should the class file be compiled with debugging information? true or false, default true.
         * - classpath - Defines the class path to be used to compile the generated servlets. This parameter only has an effect if the ServletContext attribute org.apache.jasper.Constants.SERVLET_CLASSPATH is not set. This attribute is always set when Jasper is used within Tomcat. By default the classpath is created dynamically based on the current web application.
         * - compilerClassName
         * - compilerSourceVM - What JDK version are the source files compatible with? (Default value: 1.7)
         * - compilerTargetVM - What JDK version are the generated files compatible with? (Default value: 1.7)
         * - compiler - Which compiler Ant should use to compile JSP pages. The valid values for this are the same as for the compiler attribute of Ant's javac task. If the value is not set, then the default Eclipse JDT Java compiler will be used instead of using Ant. There is no default value. If this attribute is set then setenv.[sh|bat] should be used to add ant.jar, ant-launcher.jar and tools.jar to the CLASSPATH environment variable.
         * - development - Is Jasper used in development mode? If true, the frequency at which JSPs are checked for modification may be specified via the modificationTestInterval parameter.true or false, default true.
         * - displaySourceFragment - Should a source fragment be included in exception messages? true or false, default true.
         * - dumpSmap - Should the SMAP info for JSR45 debugging be dumped to a file? true or false, default false. false if suppressSmap is true.
         * - enablePooling - Determines whether tag handler pooling is enabled. This is a compilation option. It will not alter the behaviour of JSPs that have already been compiled. true or false, default true.
         * - engineOptionsClass - Allows specifying the Options class used to configure Jasper. If not present, the default EmbeddedServletOptions will be used. This option is ignored if running under a SecurityManager.
         * - errorOnUseBeanInvalidClassAttribute - Should Jasper issue an error when the value of the class attribute in an useBean action is not a valid bean class? true or false, default true.
         * - fork - Have Ant fork JSP page compiles so they are performed in a separate JVM from Tomcat? true or false, default true.
         * - genStringAsCharArray - Should text strings be generated as char arrays, to improve performance in some cases? Default false.
         * - ieClassId - The class-id value to be sent to Internet Explorer when using <jsp:plugin> tags. Default clsid:8AD9C840-044E-11D1-B3E9-00805F499D93.
         * - javaEncoding - Java file encoding to use for generating java source files. Default UTF8.
         * - jspIdleTimeout - The amount of time in seconds a JSP can be idle before it is unloaded. A value of zero or less indicates never unload. Default -1
         * - keepgenerated - Should we keep the generated Java source code for each page instead of deleting it? true or false, default true.
         * - mappedfile - Should we generate static content with one print statement per input line, to ease debugging? true or false, default true.
         * - maxLoadedJsps - The maximum number of JSPs that will be loaded for a web application. If more than this number of JSPs are loaded, the least recently used JSPs will be unloaded so that the number of JSPs loaded at any one time does not exceed this limit. A value of zero or less indicates no limit. Default -1
         * - modificationTestInterval - Causes a JSP (and its dependent files) to not be checked for modification during the specified time interval (in seconds) from the last time the JSP was checked for modification. A value of 0 will cause the JSP to be checked on every access. Used in development mode only. Default is 4 seconds.
         * - quoteAttributeEL - When EL is used in an attribute value on a JSP page, should the rules for quoting of attributes described in JSP.1.6 be applied to the expression? true or false, default true.
         * - recompileOnFail - If a JSP compilation fails should the modificationTestInterval be ignored and the next access trigger a re-compilation attempt? Used in development mode only and is disabled by default as compilation may be expensive and could lead to excessive resource usage.
         * - scratchdir - What scratch directory should we use when compiling JSP pages? Default is the work directory for the current web application. This option is ignored if running under a SecurityManager.
         * - strictQuoteEscaping - When scriptlet expressions are used for attribute values, should the rules in JSP.1.6 for the escaping of quote characters be strictly applied? true or false, default true.
         * - suppressSmap - Should the generation of SMAP info for JSR45 debugging be suppressed? true or false, default false.
         * - trimSpaces - Should template text that consists entirely of whitespace be removed? true or false, default false.
         * - xpoweredBy - Determines whether X-Powered-By response header is added by generated servlet. true or false, default false.
         */
        MockServletContext context = new MockServletContext("jsps") {
            @Override
            public JspConfigDescriptor getJspConfigDescriptor() {
                return null;
            }
        };
        Object instanceManager = Class.forName("org.apache.tomcat.SimpleInstanceManager").newInstance();
        context.setAttribute("org.apache.tomcat.InstanceManager", instanceManager);

        MockServletConfig config = new MockServletConfig(context, "jsp");
        config.addInitParameter("compilerClassName", "org.apache.jasper.compiler.JDTCompiler"); // default
        config.addInitParameter("development", "false");
        config.addInitParameter("javaEncoding", "UTF-8");
        config.addInitParameter("keepgenerated", "true");
        config.addInitParameter("scratchdir", scratchDir.getCanonicalPath());
        config.addInitParameter("trimSpaces", "true");
        config.addInitParameter("xpoweredBy", "true");
        jspServlet.init(config);

        MockHttpServletRequest req = req(new MockHttpServletRequest(context), "/", "index.jsp");
        req.setMethod("GET");
        req.setAttribute("user", new User("Grzegorz"));
        MockHttpServletResponse res = new MockHttpServletResponse();

        jspServlet.service(req, res);

        assertThat(res.getStatus(), equalTo(HttpServletResponse.SC_OK));
        String response = res.getContentAsString();
        log.info("Response: {}", response);

        assertTrue(response.contains("<p id=\"p1\">Welcome Grzegorz"));
        assertTrue(response.contains("<p id=\"p2\">[hello Grzegorz]"));
    }

    private MockHttpServletRequest req(MockHttpServletRequest req, String s, String s1) {
        req.setServletPath(s);
        req.setPathInfo(s1);
        req.setRequestURI(s + s1);
        return req;
    }

    public static class User {
        private String name;

        public User(String name) {
            this.name = name;
        }

        public String hello(String arg) {
            return String.format("[hello %s]", arg);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}
