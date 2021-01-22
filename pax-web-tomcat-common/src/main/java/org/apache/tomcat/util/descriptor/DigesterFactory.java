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
package org.apache.tomcat.util.descriptor;

// CHECKSTYLE:OFF
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.RuleSet;
import org.apache.tomcat.util.res.StringManager;
import org.ops4j.pax.web.service.WebContainer;
import org.xml.sax.ext.EntityResolver2;

/**
 * Wrapper class around the Digester that hide Digester's initialization
 * details.
 */
public class DigesterFactory {

    private static final StringManager sm =
            StringManager.getManager(Constants.PACKAGE_NAME);


    /**
     * Mapping of well-known public IDs used by the Servlet API to the matching
     * local resource.
     */
    public static final Map<String,String> SERVLET_API_PUBLIC_IDS;

    /**
     * Mapping of well-known system IDs used by the Servlet API to the matching
     * local resource.
     */
    public static final Map<String,String> SERVLET_API_SYSTEM_IDS;

    static {
        Map<String, String> publicIds = new HashMap<>();
        Map<String, String> systemIds = new HashMap<>();

        // W3C
        add(publicIds, XmlIdentifiers.XSD_10_PUBLIC, locationFor("XMLSchema.dtd"));
        add(publicIds, XmlIdentifiers.DATATYPES_PUBLIC, locationFor("datatypes.dtd"));
        add(systemIds, XmlIdentifiers.XML_2001_XSD, locationFor("xml.xsd"));

        // from J2EE 1.2
        add(publicIds, XmlIdentifiers.WEB_22_PUBLIC, locationFor("web-app_2_2.dtd"));
        add(publicIds, XmlIdentifiers.TLD_11_PUBLIC, locationFor("web-jsptaglibrary_1_1.dtd"));

        // from J2EE 1.3
        add(publicIds, XmlIdentifiers.WEB_23_PUBLIC, locationFor("web-app_2_3.dtd"));
        add(publicIds, XmlIdentifiers.TLD_12_PUBLIC, locationFor("web-jsptaglibrary_1_2.dtd"));

        // from J2EE 1.4
        add(systemIds, "http://www.ibm.com/webservices/xsd/j2ee_web_services_1_1.xsd",
                locationFor("j2ee_web_services_1_1.xsd"));
        add(systemIds, "http://www.ibm.com/webservices/xsd/j2ee_web_services_client_1_1.xsd",
                locationFor("j2ee_web_services_client_1_1.xsd"));
        add(systemIds, XmlIdentifiers.WEB_24_XSD, locationFor("web-app_2_4.xsd"));
        add(systemIds, XmlIdentifiers.TLD_20_XSD, locationFor("web-jsptaglibrary_2_0.xsd"));
        addSelf(systemIds, "j2ee_1_4.xsd");
        addSelf(systemIds, "jsp_2_0.xsd");

        // from JavaEE 5
        add(systemIds, XmlIdentifiers.WEB_25_XSD, locationFor("web-app_2_5.xsd"));
        add(systemIds, XmlIdentifiers.TLD_21_XSD, locationFor("web-jsptaglibrary_2_1.xsd"));
        addSelf(systemIds, "javaee_5.xsd");
        addSelf(systemIds, "jsp_2_1.xsd");
        addSelf(systemIds, "javaee_web_services_1_2.xsd");
        addSelf(systemIds, "javaee_web_services_client_1_2.xsd");

        // from JavaEE 6
        add(systemIds, XmlIdentifiers.WEB_30_XSD, locationFor("web-app_3_0.xsd"));
        add(systemIds, XmlIdentifiers.WEB_FRAGMENT_30_XSD, locationFor("web-fragment_3_0.xsd"));
        addSelf(systemIds, "web-common_3_0.xsd");
        addSelf(systemIds, "javaee_6.xsd");
        addSelf(systemIds, "jsp_2_2.xsd");
        addSelf(systemIds, "javaee_web_services_1_3.xsd");
        addSelf(systemIds, "javaee_web_services_client_1_3.xsd");

        // from JavaEE 7
        add(systemIds, XmlIdentifiers.WEB_31_XSD, locationFor("web-app_3_1.xsd"));
        add(systemIds, XmlIdentifiers.WEB_FRAGMENT_31_XSD, locationFor("web-fragment_3_1.xsd"));
        addSelf(systemIds, "web-common_3_1.xsd");
        addSelf(systemIds, "javaee_7.xsd");
        addSelf(systemIds, "jsp_2_3.xsd");
        addSelf(systemIds, "javaee_web_services_1_4.xsd");
        addSelf(systemIds, "javaee_web_services_client_1_4.xsd");

        // from JavaEE 8
        add(systemIds, XmlIdentifiers.WEB_40_XSD, locationFor("web-app_4_0.xsd"));
        add(systemIds, XmlIdentifiers.WEB_FRAGMENT_40_XSD, locationFor("web-fragment_4_0.xsd"));
        addSelf(systemIds, "web-common_4_0.xsd");
        addSelf(systemIds, "javaee_8.xsd");

        SERVLET_API_PUBLIC_IDS = Collections.unmodifiableMap(publicIds);
        SERVLET_API_SYSTEM_IDS = Collections.unmodifiableMap(systemIds);
    }

    private static void addSelf(Map<String, String> ids, String id) {
        String location = locationFor(id);
        if (location != null) {
            ids.put(id, location);
            ids.put(location, location);
        }
    }

    private static void add(Map<String,String> ids, String id, String location) {
        if (location != null) {
            ids.put(id, location);
            // BZ 63311
            // Support http and https locations as the move away from http and
            // towards https continues.
            if (id.startsWith("http://")) {
                String httpsId = "https://" + id.substring(7);
                ids.put(httpsId, location);
            }
        }
    }

    private static String locationFor(String name) {
        // Resources are now provided in pax-web-api, as they may be needed from other Pax Web bundles
        URL location = WebContainer.class.getResource("/META-INF/resources/" + name);
        if (location == null) {
            Log log = LogFactory.getLog(DigesterFactory.class);
            log.warn(sm.getString("digesterFactory.missingSchema", name));
            return null;
        }
        return location.toExternalForm();
    }


    /**
     * Create a <code>Digester</code> parser.
     * @param xmlValidation turn on/off xml validation
     * @param xmlNamespaceAware turn on/off namespace validation
     * @param rule an instance of <code>RuleSet</code> used for parsing the xml.
     * @param blockExternal turn on/off the blocking of external resources
     * @return a new digester
     */
    public static Digester newDigester(boolean xmlValidation,
                                       boolean xmlNamespaceAware,
                                       RuleSet rule,
                                       boolean blockExternal) {
        Digester digester = new Digester();
        digester.setNamespaceAware(xmlNamespaceAware);
        digester.setValidating(xmlValidation);
        digester.setUseContextClassLoader(true);
        EntityResolver2 resolver = new LocalResolver(SERVLET_API_PUBLIC_IDS,
                SERVLET_API_SYSTEM_IDS, blockExternal);
        digester.setEntityResolver(resolver);
        if (rule != null) {
            digester.addRuleSet(rule);
        }

        return digester;
    }
}
// CHECKSTYLE:ON
