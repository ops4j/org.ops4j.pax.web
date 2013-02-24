package org.ops4j.pax.web.itest.load

import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.jdbc.Predef._
import com.excilys.ebi.gatling.http.Headers.Names._
import akka.util.duration._
import bootstrap._

class BasicSimulation extends Simulation {

	val httpConf = httpConfig
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

	setUp(scn.users(10).ramp(10).protocolConfig(httpConf))
}