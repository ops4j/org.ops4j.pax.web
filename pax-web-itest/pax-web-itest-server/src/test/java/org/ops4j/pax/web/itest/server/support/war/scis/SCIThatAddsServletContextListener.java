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
package org.ops4j.pax.web.itest.server.support.war.scis;

import org.ops4j.pax.web.itest.server.support.war.StaticList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import java.util.Set;

public class SCIThatAddsServletContextListener implements ServletContainerInitializer {

    public static final Logger LOG = LoggerFactory.getLogger(SCIThatAddsServletContextListener.class);

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
        ctx.addListener(new ServletContextListener() {
            @Override
            public void contextInitialized(ServletContextEvent sce) {
                LOG.info("Listener-from-SCI initialized", new Throwable());
                StaticList.EVENTS.add("Listener-from-SCI initialized");
            }

            @Override
            public void contextDestroyed(ServletContextEvent sce) {
                LOG.info("Listener-from-SCI destroyed", new Throwable());
                StaticList.EVENTS.add("Listener-from-SCI destroyed");
            }
        });
    }

}
