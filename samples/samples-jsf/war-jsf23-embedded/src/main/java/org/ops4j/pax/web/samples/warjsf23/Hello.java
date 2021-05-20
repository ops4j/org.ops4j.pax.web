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
package org.ops4j.pax.web.samples.warjsf23;

import java.io.IOException;
import java.util.Properties;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

// According to "5.4 Managed Bean Annotations" of JSF 2.3 specification,javax.faces.bean package
// is deprecated and the recommended annotations should be taken from CDI 1.2

@ManagedBean
@RequestScoped
public class Hello {

	private String what;
	private String result;
	private String test = "hello from working JSF 2.2 example";

	public Hello() throws IOException {
		Properties props = new Properties();
		props.load(getClass().getResourceAsStream("/version.properties"));
		test = String.format("Hello from JSF 2.3 example running on Pax Web %s",
				props.getProperty("version.pax-web"));
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
		return test;
	}

	public void say() {
		result = String.format("Hello %s!", what);
	}

}
