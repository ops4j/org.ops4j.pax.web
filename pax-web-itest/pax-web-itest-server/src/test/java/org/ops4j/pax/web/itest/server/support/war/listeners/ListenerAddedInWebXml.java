/*
 * Copyright 2021 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.itest.server.support.war.listeners;

import org.ops4j.pax.web.itest.server.support.war.StaticList;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ListenerAddedInWebXml implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            // It should be possible to add a listener OTHER than javax.servlet.ServletContextListener
            // from a listener added in web.xml/web-fragment.xml/@WebListener. But only Jetty allow adding new
            // ServletContextListeners IF ContextHandler.Context._extendedListenerTypes == true
            sce.getServletContext().addListener(new ServletContextListener() {
            });
        } catch (UnsupportedOperationException ignored) {
            // it should not be possible to add a listener from a listener
            StaticList.EVENTS.add("Listener-from-web.xml initialized");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        StaticList.EVENTS.add("Listener-from-web.xml destroyed");
    }

}
