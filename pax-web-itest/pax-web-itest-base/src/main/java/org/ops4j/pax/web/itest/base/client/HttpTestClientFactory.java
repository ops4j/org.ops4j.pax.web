/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.itest.base.client;

public class HttpTestClientFactory {

	/**
	 * creates a default HttpTestClient based on Apache HttpComponents with
	 * some default configuration.
	 * <ul>
	 * 	<li>Return-Code: 200 OK</li>
	 * 	<li>Keystore: src/test/resources/keystore</li>
	 * 	<li>Request-Header: Accept-Language=en</li>
	 * </ul>
	 * @return Apache HttpComponents HttpTestClient
	 */
	public static HttpTestClient createHttpComponentsTestClient(){
		return new HttpComponentsTestClient()
			.withKeystore("src/test/resources/keystore", "admin", "admin")
			.addRequestHeader("Accept-Language", "en")
			.withReturnCode(200);
	}
	
}
