/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ops4j.pax.web.samples.primefaces;

// According to "5.4 Managed Bean Annotations" of JSF 2.3 specification,javax.faces.bean package
// is deprecated and the recommended annotations should be taken from CDI 1.2

@SuppressWarnings("deprecation")
@javax.faces.bean.ManagedBean(name = "helloWorld")
@javax.faces.bean.SessionScoped
public class HelloWorldController {

	private String name;

	public HelloWorldController() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Method that is backed to a submit button of a form.
	 */
	public String send() {
		return "success?faces-redirect=true";
	}

}
