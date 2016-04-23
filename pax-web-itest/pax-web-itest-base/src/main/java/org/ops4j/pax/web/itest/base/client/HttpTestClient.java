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

import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface HttpTestClient {

	/**
	 * Configures a keystore for SSL located somewhere on the filesystem. The path-parameter can be
	 * prefix by a system-variable ${var}/path
	 *
	 * @param keystoreLocation path to keystore-file
	 * @return the HttpTestClient-instance
     */
	HttpTestClient withExternalKeystore(String keystoreLocation);

	/**
	 * Configures a keystore for SSL which is located within a bundle.
	 *
	 * @param bundleSymbolicName The bundle containing the keystore-file
	 * @param keystoreLocation path to keystore-file within bundle
	 * @return the HttpTestClient-instance
	 */
	HttpTestClient withBundleKeystore(String bundleSymbolicName, String keystoreLocation);

	/**
	 * Configures the seconds after which a request will get a TimeoutException
	 * @param seconds the time after which the requests times out
	 * @return the HttpTestClient-instance
     */
	HttpTestClient timeoutInSeconds(int seconds);

	/**
	 * Configures the pending execution with additional request-headers which must be set
	 * opon execution
	 * @param header header-name
	 * @param value header-value
	 * @return the HttpTestClient-instance
	 */
	HttpTestClient addRequestHeader(String header, String value);

	/**
	 * <p>
	 * 	Each execution of the HttpTestClient creates a new Session. Sometimes it is necessary to
	 * 	transfer a previous session to a new (for example POST-) request.
	 * </p>
	 * <p>
	 *	For the first request, create a new instance of {@link CookieState} which is then passed
	 *	to all following test-executions.
	 * </p>
	 * @param cookieState mutable state
	 * @return the HttpTestClient-instance
     */
	HttpTestClient useCookieState(CookieState cookieState);
	
	/**
	 * Sets the expected return-code that is expected after execution.
	 * Multiple values can be set, but successive calls will overwrite previous values
	 * @param returnCode the expected HTTP return-code
	 * @return the HttpTestClient-instance
	 */
	HttpTestClient withReturnCode(int... returnCode);

	/**
	 * Sets the test to run the http-request asynchronous
	 * @return the HttpTestClient-instance
     */
	HttpTestClient async();

	/**
	 * Enables BaseAuth authentication for this test
	 * @param user the user which is used in BaseAuth
	 * @param password the password which is used in BaseAuth
	 * @param realm the realm which is used in BaseAuth
     * @return the HttpTestClient-instance
     */
	HttpTestClient authenticate(String user, String password, String realm);

	/**
	 * The given predicate is applied against the returned response-body (String) when
	 * {@link HttpTestClient#executeTest()} is called.
	 * Multiple assertions can be prepared and are combined in one assertion.
	 * @param message Assertion message in case the predicate fails
	 * @param assertion the assertion-predicate gets the response-body as parameter, which is never null
	 * @return the HttpTestClient-instance
	 */
	HttpTestClient withResponseAssertion(final String message, final Predicate<String> assertion);

	/**
	 * The given predicate is applied against the returned headers (stream of map-entries) when
	 * {@link HttpTestClient#executeTest()} is called.
	 * Multiple assertions can be prepared and are combined in one assertion.
	 * @param message Assertion message in case the predicate fails
	 * @param assertion the assertion-predicate gets the response-headers as parameter
	 * @return the HttpTestClient-instance
	 */
	HttpTestClient withResponseHeaderAssertion(final String message, final Predicate<Stream<Map.Entry<String, String>>> assertion);

	/**
	 * Prepares the client to execute a GET-request against the given URL.
	 * @param url Destination-url that should be tested
	 * @return the HttpGetConfiguration-instance
     */
	HttpTestClient doGET(String url);

	/**
	 * Conveniece-method for executing the test without further configuration
	 * Note: this method is a terminal-call and ends the fluent-method-chaining.
	 * @param url Destination-url that should be tested
	 * @return the response-body for further processing, in case all assertions passed successfully
	 * @throws AssertionError a single AssertionError for all prepared assertions
	 * @throws Exception exceptions propagated from underlying client
	 * @see #doGET(String)
	 */
	String doGETandExecuteTest(String url) throws Exception;

	/**
	 * Prepares the client to execute a POST-request against the given URL.
	 * @param url Destination-url that should be tested
	 * @return the HttpPostConfiguration-instance
	 */
	HttpTestClient doPOST(String url);


	/**
	 * Depending on {@link #doGET(String)} or {@link #doPOST(String)}
	 * the parameters are later added to the GET-URL or the POST-content.
	 * @param name parameter-name
	 * @param value parameter-value
	 * @return the HttpPostConfiguration-instance
	 */
	HttpTestClient addParameter(String name, String value);

	/**
	 * Executes the this test-client-configuration.
	 * Note: this method is a terminal-call and ends the fluent-method-chaining.
	 * @return the response-body for further processing, in case all assertions passed successfully
	 * @throws AssertionError a single AssertionError for all prepared assertions
	 * @throws Exception exceptions propagated from underlying client
	 */
	String executeTest() throws Exception;
}
