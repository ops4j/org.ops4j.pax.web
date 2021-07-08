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

import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.osgi.service.cdi.annotations.Bean;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.url.URLStreamHandlerService;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;

@Bean
@Named("hello")
@RequestScoped
public class Hello implements Serializable {

	private String what;
	private String result;
	private final String test = "hello from working JSF 2.3/CDI 2.0 example";

	@Inject
	@Reference(target = "(!(url.handler.protocol=mvn))")
	private URLStreamHandlerService handler;

	@Inject
	@Reference
	private ServerControllerFactory serverControllerFactory;

	public Hello() {
		System.out.println("<ctor>");
	}

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
		return String.format("%s, %s, %s", test,
				handler.getClass().getName(), serverControllerFactory.getClass().getName());
	}

	public void say() {
		result = String.format("Hello %s! (mvn handler: %s, current web runtime: %s)", what,
				handler.getClass().getName(), serverControllerFactory.getClass().getName());
	}

}
