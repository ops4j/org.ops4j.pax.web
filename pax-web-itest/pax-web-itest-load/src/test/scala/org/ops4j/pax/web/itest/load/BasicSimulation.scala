/*
 * Copyright 2013 Achim Nierbeck
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *   
 *    http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.ops4j.pax.web.itest.load

class BasicSimulation extends Simulation {

	val httpConf = http
		.baseURL("http://localhost:8181")
		.acceptCharsetHeader("ISO-8859-1,utf-8;q=0.7,*;q=0.7")
		.acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
		.acceptEncodingHeader("gzip, deflate")
		.acceptLanguageHeader("fr,fr-fr;q=0.8,en-us;q=0.5,en;q=0.3")
		.disableFollowRedirect

	val headers_1 = Map(
		"Keep-Alive" -> "115")

	val headers_3 = Map(
		"Keep-Alive" -> "115",
		"Content-Type" -> "application/x-www-form-urlencoded")

	val headers_6 = Map(
		"Accept" -> "application/json, text/javascript, */*; q=0.01",
		"Keep-Alive" -> "115",
		"X-Requested-With" -> "XMLHttpRequest")

	val scn = scenario("TestScenario 1")
		.exec(
			http("request_1")
				.get("/")
				.headers(headers_1)
				.check(status.is(200)))
		.pause(0 milliseconds, 100 milliseconds)
		.exec(
			http("request_2")
				.get("/helloworld/hs/")
				.headers(headers_1))
		.pause(12, 13)

	setUp(scn.inject(rampUsers(10) over (30 seconds)).protocols(httpConf))
}