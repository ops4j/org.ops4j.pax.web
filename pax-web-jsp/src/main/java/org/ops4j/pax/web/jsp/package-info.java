/*
 * Copyright 2020 OPS4J.
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

/**
 * <p>Package that contains implementation of "JSP container" from the point of view of Servlet API specification.</p>
 *
 * <p>At some point during evolution of Servlet/JSP API specifications, the Servlet container was freed from the
 * responsibility of being "JSP container" and the interoperation was delegated to the mechanism of
 * {@link javax.servlet.ServletContainerInitializer}s (SCIs). This package contains one such SCI that prepares
 * the context (and in Pax Web it is specialized <em>OSGi context</em>) to allow usage of JSPs. The most
 * important thing is support for discovering TLD descriptors.</p>
 *
 * <p>In Pax Web 7 there was quite complex machinery involved and several Tomcat (Jasper) classes had to be
 * shadowed in pax-web-jsp. But in Pax Web 8 it's all much cleared.</p>
 *
 * <p>One example is {@code org.apache.tomcat.util.descriptor.DigesterFactory} that had to be changed to look
 * for JSP/Servlet API XSDs using different means. pax-web-jsp simply contains the needed resources, so original
 * {@code org.apache.tomcat.util.descriptor.DigesterFactory} works out of the box.</p>
 */
package org.ops4j.pax.web.jsp;
