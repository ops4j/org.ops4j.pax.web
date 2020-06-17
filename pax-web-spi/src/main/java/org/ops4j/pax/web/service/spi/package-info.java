/*
 * Copyright 2020 Achim Nierbeck
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
 * Main <em>SPI</em> package for Pax Web. It's not <em>pure</em> SPI defined as collection of provider interfaces.
 * It's rather set of infrastructure classes/packages/mechanisms/interfaces to be used by different server
 * environments (Jetty, Tomcat, Undertow) and specification implementations (HttpService, Whiteboard).
 */
package org.ops4j.pax.web.service.spi;
