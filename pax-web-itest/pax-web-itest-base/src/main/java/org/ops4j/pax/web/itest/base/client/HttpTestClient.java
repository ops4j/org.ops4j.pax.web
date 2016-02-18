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

import java.util.function.Predicate;

public interface HttpTestClient {
	
	/**
	 * Configures a keystore used for SSL
	 * @param keystoreLocation path to keystorefile
	 * @param user keystore username
	 * @param password keystore password
	 * @return the HttpTestClient-instance
	 */
	public HttpTestClient withKeystore(String keystoreLocation, String user, String password);
	
	/**
	 * Configures the pending execution with additional request-headers which must be set
	 * opon execution
	 * @param header header-name
	 * @param value header-value
	 * @return the HttpTestClient-instance
	 */
	public HttpTestClient addRequestHeader(String header, String value);
	
	/**
	 * Sets the expected return-code that is expected after execution
	 * @param returnCode the expected HTTP return-code
	 * @return the HttpTestClient-instance
	 */
	public HttpTestClient withReturnCode(int returnCode);
	
	/**
	 * The given predicate is applied against the returned response-body (String) when {@link #executeTest(String)}
	 * is called. Multiple assertions can be prepared and are combined in one assertion.
	 * @param message Assertion message in case the predicate fails
	 * @param assertion the assertion-predicate gets the response-body as parameter
	 * @return the HttpTestClient-instance
	 */
	public HttpTestClient prepareResponseAssertion(final String message, final Predicate<String> assertion);
	
	/**
	 * Executes the this test-client-configuration against the given url.
	 * Note: this method is a terminal-call and ends the fluent-method-chaining.
	 * @param url Destination-url that should be tested
	 * @throws Exception exception propagated from underlying client
	 */
	public void executeTest (String url) throws Exception;
	
}
