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
 * <p>This package provides various implementations of {@link org.osgi.service.http.HttpContext} and
 * {@link org.ops4j.pax.web.service.WebContainerContext} to be used in:<ul>
 *     <li>Http Service implementation in pax-web-runtime bundle</li>
 *     <li>Whiteboard Service implementation in pax-web-extender-whiteboard bundle</li>
 * </ul></p>
 *
 * <p><em>Unique</em> implementations are wrappers for existing instances of
 * {@link org.ops4j.pax.web.service.WebContainerContext} and
 * {@link org.ops4j.pax.web.service.MultiBundleWebContainerContext} because Http Service specification says
 * that {@link org.osgi.service.http.HttpService#createDefaultHttpContext()} returns new instance on each call,
 * however we don't want these new instances to refer to different internal representations of the <em>contexts</em>.
 * </p>
 */
package org.ops4j.pax.web.service.spi.context;
