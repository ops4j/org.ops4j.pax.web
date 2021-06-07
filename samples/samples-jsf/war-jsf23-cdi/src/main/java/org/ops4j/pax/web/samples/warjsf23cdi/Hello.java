/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ops4j.pax.web.samples.warjsf23cdi;

import javax.inject.Inject;
import javax.inject.Named;

import org.ops4j.pax.cdi.api.Component;
import org.ops4j.pax.cdi.api.Dynamic;
import org.ops4j.pax.cdi.api.Filter;
import org.ops4j.pax.cdi.api.Immediate;
import org.ops4j.pax.cdi.api.Service;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.osgi.service.url.URLStreamHandlerService;

@Named("hello")
@Immediate
@Component
public class Hello {

	private String what;
	private String result;
	private final String test = "hello from working JSF 2.2/CDI 1.2 example";

	@Inject
	@Dynamic
	@Service
	@Filter("(!(url.handler.protocol=mvn))")
	private URLStreamHandlerService handler;

	@Inject
	@Dynamic
	@Service
	private ServerControllerFactory serverControllerFactory;

	public void setWhat(String what) {
		this.what = what;
	}

	public String getWhat() {
		return what;
	}

	public String getResult() {
		return result;
	}

	public String getTest() {
		return String.format("test, %s, %s", handler, serverControllerFactory);
	}

	public void say() {
		result = String.format("Hello %s! (mvn handler: %s, current web runtime: %s)", what, handler, serverControllerFactory);
	}

}
